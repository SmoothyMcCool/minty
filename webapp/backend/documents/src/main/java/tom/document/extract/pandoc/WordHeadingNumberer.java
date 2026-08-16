package tom.document.extract.pandoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Resolves Word's multilevel heading numbers (e.g. "2.3.1") for OOXML files.
 *
 * HYBRID MODE: 1. If numbering metadata exists (Word/LibreOffice), it
 * calculates complex hierarchical numbers. 2. If no numbering metadata exists
 * (Google Docs), it performs sequential numbering (1., 2., 3...) on all
 * detected headings to ensure the Markdown is numbered.
 *
 * All diagnostic logging below is tagged "[HEADNUM]" so it can be grepped out
 * of application logs independently of everything else.
 */
public final class WordHeadingNumberer {

	private static final Logger logger = LogManager.getLogger(WordHeadingNumberer.class);
	private static final String TAG = "[HEADNUM]";

	private static final Pattern HEADING_STYLE_ID = Pattern.compile("^Heading\\s*(\\d)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ATX_HEADER = Pattern.compile("^(#{1,6})(\\s+)(.*)$");
	private static final Pattern ALREADY_NUMBERED = Pattern.compile("^\\(?\\d+([.\\-]\\d+)*[.)]?\\s");

	private WordHeadingNumberer() {
	}

	public static String injectNumbers(File docx, String markdown) {
		logger.info("{} injectNumbers() called for file={} exists={} size={} markdownLen={}", TAG, docx.getName(),
				docx.exists(), docx.exists() ? docx.length() : -1, markdown == null ? -1 : markdown.length());
		try {
			List<String> numbers = resolveHeadingNumbers(docx);
			logger.info("{} resolveHeadingNumbers() returned {} number(s): {}", TAG, numbers.size(), numbers);
			if (numbers.isEmpty()) {
				logger.warn("{} numbers list is EMPTY -- returning markdown UNCHANGED. This is why you see no "
						+ "numbers with no exception: nothing failed, there was just nothing to inject.", TAG);
				return markdown;
			}
			String result = injectIntoMarkdown(markdown, numbers);
			boolean changed = !result.equals(markdown);
			logger.info("{} injectIntoMarkdown() completed. markdown actually changed={}", TAG, changed);
			return result;
		} catch (Exception e) {
			logger.warn("{} EXCEPTION resolving/injecting heading numbers for {}: {}", TAG, docx.getName(),
					e.toString(), e);
			return markdown;
		}
	}

	// ------------------------------------------------------------------
	// docx XML -> ordered list of resolved number strings
	// ------------------------------------------------------------------

	private static List<String> resolveHeadingNumbers(File docx) throws Exception {
		Document documentXml = readZipEntryXml(docx, "word/document.xml");
		if (documentXml == null) {
			logger.warn("{} word/document.xml not found or unparsable -- not a valid docx zip?", TAG);
			return List.of();
		}

		Document numberingXml = readZipEntryXml(docx, "word/numbering.xml");
		Document stylesXml = readZipEntryXml(docx, "word/styles.xml");
		logger.info("{} zip parts found: document.xml=yes numbering.xml={} styles.xml={}", TAG,
				numberingXml != null ? "yes" : "NO", stylesXml != null ? "yes" : "NO");

		Map<String, Element> stylesById = stylesXml == null ? Map.of() : indexStylesById(stylesXml);
		logger.info("{} indexed {} style(s) from styles.xml (by styleId)", TAG, stylesById.size());

		// --- STEP 1: Check if we are in a "Rich" numbering document (Word/LibreOffice)
		// ---
		boolean hasComplexNumbering = false;
		int abstractNumCount = 0;
		if (numberingXml != null) {
			NodeList abstractNums = numberingXml.getElementsByTagNameNS("*", "abstractNum");
			abstractNumCount = abstractNums.getLength();
			if (abstractNumCount > 0) {
				hasComplexNumbering = true;
			}
		}
		logger.info("{} hasComplexNumbering={} (abstractNum count in numbering.xml={})", TAG, hasComplexNumbering,
				abstractNumCount);

		NodeList allParagraphs = documentXml.getElementsByTagNameNS("*", "p");
		logger.info("{} total <w:p> paragraphs in document.xml={}", TAG, allParagraphs.getLength());

		// --- CASE A: COMPLEX MODE (Word/LibreOffice) ---
		if (hasComplexNumbering && numberingXml != null) {
			logger.info("{} MODE = COMPLEX (numbering.xml driven)", TAG);
			return computeComplexNumbers(documentXml, numberingXml, stylesById);
		}

		// --- CASE B: SEQUENTIAL FALLBACK MODE (Google Docs / Flat XML) ---
		else {
			logger.info("{} MODE = SEQUENTIAL FALLBACK for {} (no abstractNum entries found)", TAG, docx.getName());
			List<String> sequentialNumbers = new ArrayList<>();
			NodeList paragraphs = documentXml.getElementsByTagNameNS("*", "p");

			int[] currentCounters = new int[6];

			for (int i = 0; i < paragraphs.getLength(); i++) {
				Element p = (Element) paragraphs.item(i);
				boolean isHeading = isHeadingParagraph(p, stylesById);
				if (!isHeading) {
					continue;
				}

				int currentLevel = getHeadingLevelFromElement(p, stylesById);
				String title = getFirstTextContent(p);

				if (currentLevel == 0) {
					logger.info(
							"{} [seq] paragraph {} styleId={} title=\"{}\" -> detected as heading but level=0, SKIPPING",
							TAG, i, headingStyleId(p), title);
					continue;
				}

				if (alreadyNumbered(title)) {
					logger.info(
							"{} [seq] paragraph {} styleId={} title=\"{}\" -> already starts with a number, SKIPPING",
							TAG, i, headingStyleId(p), title);
					continue;
				}

				for (int l = currentLevel; l < 6; l++)
					currentCounters[l] = 0;
				currentCounters[currentLevel - 1]++;

				StringBuilder sb = new StringBuilder();
				for (int l = 0; l <= currentLevel - 1; l++) {
					if (l > 0)
						sb.append(".");
					sb.append(currentCounters[l]);
				}
				String computed = sb.toString();
				sequentialNumbers.add(computed);
				logger.info("{} [seq] paragraph {} styleId={} title=\"{}\" level={} -> computed number={}", TAG, i,
						headingStyleId(p), title, currentLevel, computed);
			}
			logger.info("{} [seq] TOTAL numbers computed={}", TAG, sequentialNumbers.size());
			return sequentialNumbers;
		}
	}

	private static int getHeadingLevelFromElement(Element p, Map<String, Element> stylesById) {
		Element outline = firstDescendant(p, "outlineLvl");
		if (outline != null) {
			int lvl = parseIntSafe(outline.getAttribute("w:val"), 0);
			return lvl + 1;
		}

		String styleId = headingStyleId(p);
		if (styleId != null) {
			Matcher m = Pattern.compile("(?i)Heading\\s*(\\d)").matcher(styleId);
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		}

		return 1;
	}

	/**
	 * Logic for resolving hierarchical numbers (1.1.2) using numbering.xml
	 */
	private static List<String> computeComplexNumbers(Document documentXml, Document numberingXml,
			Map<String, Element> stylesById) throws Exception {
		NumberingDefs defs = parseNumberingXml(numberingXml);
		logger.info("{} [complex] parsed numbering.xml: {} abstractNum def(s), {} numId mapping(s)", TAG,
				defs.abstractLevelFormat.size(), defs.numIdToAbstractId.size());

		List<String> results = new ArrayList<>();
		Map<String, int[]> counters = new HashMap<>();

		NodeList paragraphs = documentXml.getElementsByTagNameNS("*", "p");

		int headingCount = 0;
		int noNumPrCount = 0;
		int unresolvedAbstractCount = 0;
		int badLevelCount = 0;

		for (int i = 0; i < paragraphs.getLength(); i++) {
			Element p = (Element) paragraphs.item(i);
			if (!isHeadingParagraph(p, stylesById))
				continue;

			headingCount++;
			String styleId = headingStyleId(p);
			String title = getFirstTextContent(p);

			int[] numPrArr = directNumPr(p);
			String numPrSource = "direct-on-paragraph";
			if (numPrArr == null) {
				numPrArr = resolveNumPrViaStyleChain(styleId, stylesById);
				numPrSource = "style-chain";
			}
			if (numPrArr == null) {
				noNumPrCount++;
				logger.info(
						"{} [complex] heading #{} (paragraph idx={}) styleId={} title=\"{}\" -> NO numPr found "
								+ "(checked direct paragraph AND full style basedOn chain). SKIPPING.",
						TAG, headingCount, i, styleId, title);
				continue;
			}

			String numIdStr = String.valueOf(numPrArr[0]);
			int ilvl = numPrArr[1];
			String abstractId = defs.numIdToAbstractId.get(numIdStr);
			if (abstractId == null) {
				unresolvedAbstractCount++;
				logger.info(
						"{} [complex] heading #{} (paragraph idx={}) styleId={} title=\"{}\" -> numId={} ilvl={} "
								+ "(source={}) but numId NOT FOUND in numbering.xml's <w:num> list "
								+ "(known numIds: {}). Possible w:numStyleLink/styleLink case. SKIPPING.",
						TAG, headingCount, i, styleId, title, numIdStr, ilvl, numPrSource,
						defs.numIdToAbstractId.keySet());
				continue;
			}

			int[] fmtCodes = defs.abstractLevelFormat.get(abstractId);
			String[] lvlTexts = defs.abstractLevelText.get(abstractId);
			int[] starts = defs.abstractLevelStart.get(abstractId);
			if (fmtCodes == null || ilvl >= fmtCodes.length) {
				badLevelCount++;
				logger.info("{} [complex] heading #{} (paragraph idx={}) styleId={} title=\"{}\" -> numId={} resolved "
						+ "to abstractId={} but ilvl={} is out of range or abstractId has no level data. SKIPPING.",
						TAG, headingCount, i, styleId, title, numIdStr, abstractId, ilvl);
				continue;
			}

			int[] counter = counters.computeIfAbsent(numIdStr, k -> {
				int[] c = new int[9];
				Arrays.fill(c, -1);
				return c;
			});

			counter[ilvl] = (counter[ilvl] < 0 ? starts[ilvl] : counter[ilvl] + 1);
			for (int lvl = ilvl + 1; lvl < counter.length; lvl++) {
				counter[lvl] = -1;
			}

			String computed = formatLevelText(lvlTexts[ilvl], counter, fmtCodes);
			results.add(computed);
			logger.info(
					"{} [complex] heading #{} (paragraph idx={}) styleId={} title=\"{}\" -> numId={} ilvl={} "
							+ "(source={}) abstractId={} lvlTextPattern=\"{}\" -> COMPUTED NUMBER = {}",
					TAG, headingCount, i, styleId, title, numIdStr, ilvl, numPrSource, abstractId, lvlTexts[ilvl],
					computed);
		}

		logger.info(
				"{} [complex] SUMMARY: paragraphsScanned={}, detectedAsHeading={}, noNumPr={}, "
						+ "unresolvedAbstractId={}, badLevelData={}, numbersComputed={}",
				TAG, paragraphs.getLength(), headingCount, noNumPrCount, unresolvedAbstractCount, badLevelCount,
				results.size());

		return results;
	}

	// ------------------------------------------------------------------
	// XML Parsing Helpers (Namespace Aware)
	// ------------------------------------------------------------------

	private static final class NumberingDefs {
		Map<String, String> numIdToAbstractId = new HashMap<>();
		Map<String, int[]> abstractLevelFormat = new HashMap<>();
		Map<String, String[]> abstractLevelText = new HashMap<>();
		Map<String, int[]> abstractLevelStart = new HashMap<>();
	}

	private static NumberingDefs parseNumberingXml(Document numberingXml) {
		NumberingDefs defs = new NumberingDefs();
		NodeList abstractNums = numberingXml.getElementsByTagNameNS("*", "abstractNum");

		// Pass 1: index every abstractNum's own raw level data (if any), and note
		// which ones are pure "link" definitions (w:numStyleLink pointing at a
		// w:styleLink elsewhere) rather than containing real <w:lvl> data. This
		// pattern is common in enterprise templates built around named list styles
		// (e.g. Word's "List Paragraph" gallery) rather than direct multilevel lists.
		Map<String, String> styleLinkNameToAbstractId = new HashMap<>(); // styleLink val -> abstractId that defines it
		Map<String, String> abstractIdToNumStyleLink = new HashMap<>(); // abstractId -> styleLink name it points to

		for (int i = 0; i < abstractNums.getLength(); i++) {
			Element abstractNum = (Element) abstractNums.item(i);
			String abstractId = abstractNum.getAttribute("w:abstractNumId");

			Element styleLink = firstDescendant(abstractNum, "styleLink");
			if (styleLink != null) {
				styleLinkNameToAbstractId.put(styleLink.getAttribute("w:val"), abstractId);
				logger.info("{} [numbering.xml] abstractNumId={} DEFINES list style \"{}\"", TAG, abstractId,
						styleLink.getAttribute("w:val"));
			}
			Element numStyleLink = firstDescendant(abstractNum, "numStyleLink");
			if (numStyleLink != null) {
				abstractIdToNumStyleLink.put(abstractId, numStyleLink.getAttribute("w:val"));
				logger.info("{} [numbering.xml] abstractNumId={} is a LINK to list style \"{}\" (no levels of its own)",
						TAG, abstractId, numStyleLink.getAttribute("w:val"));
			}

			int[] fmt = new int[9];
			String[] text = new String[9];
			int[] start = new int[9];
			Arrays.fill(start, 1);

			NodeList lvls = abstractNum.getElementsByTagNameNS("*", "lvl");
			for (int j = 0; j < lvls.getLength(); j++) {
				Element lvl = (Element) lvls.item(j);
				int ilvl = parseIntSafe(lvl.getAttribute("w:ilvl"), j);
				if (ilvl < 0 || ilvl >= 9)
					continue;

				Element numFmt = firstDescendant(lvl, "numFmt");
				Element lvlText = firstDescendant(lvl, "lvlText");
				Element startEl = firstDescendant(lvl, "start");

				fmt[ilvl] = numFmtCode(numFmt == null ? "decimal" : numFmt.getAttribute("w:val"));
				String lvlTextVal = lvlText == null ? null : lvlText.getAttribute("w:val");
				text[ilvl] = (lvlTextVal == null || lvlTextVal.isEmpty()) ? cumulativeLevelPattern(ilvl) : lvlTextVal;
				start[ilvl] = startEl == null ? 1 : parseIntSafe(startEl.getAttribute("w:val"), 1);
			}

			defs.abstractLevelFormat.put(abstractId, fmt);
			defs.abstractLevelText.put(abstractId, text);
			defs.abstractLevelStart.put(abstractId, start);
			logger.info("{} [numbering.xml] abstractNumId={} level0..8 lvlText={}", TAG, abstractId,
					Arrays.toString(text));
		}

		// Pass 2: redirect every "link" abstractNum to the level data of whichever
		// abstractNum actually defines the list style it points to.
		for (Map.Entry<String, String> entry : abstractIdToNumStyleLink.entrySet()) {
			String linkAbstractId = entry.getKey();
			String targetStyleName = entry.getValue();
			String targetAbstractId = styleLinkNameToAbstractId.get(targetStyleName);
			if (targetAbstractId == null) {
				logger.info(
						"{} [numbering.xml] abstractNumId={} links to list style \"{}\" but no abstractNum "
								+ "defines that style (styleLink) -- cannot resolve.",
						TAG, linkAbstractId, targetStyleName);
				continue;
			}
			defs.abstractLevelFormat.put(linkAbstractId, defs.abstractLevelFormat.get(targetAbstractId));
			defs.abstractLevelText.put(linkAbstractId, defs.abstractLevelText.get(targetAbstractId));
			defs.abstractLevelStart.put(linkAbstractId, defs.abstractLevelStart.get(targetAbstractId));
			logger.info("{} [numbering.xml] abstractNumId={} (link) RESOLVED to abstractNumId={}'s level data via "
					+ "list style \"{}\"", TAG, linkAbstractId, targetAbstractId, targetStyleName);
		}

		NodeList nums = numberingXml.getElementsByTagNameNS("*", "num");
		for (int i = 0; i < nums.getLength(); i++) {
			Element num = (Element) nums.item(i);
			String numId = num.getAttribute("w:numId");
			Element abstractNumIdElem = firstDescendant(num, "abstractNumId");
			if (abstractNumIdElem != null) {
				String abstractId = abstractNumIdElem.getAttribute("w:val");
				defs.numIdToAbstractId.put(numId, abstractId);

				// Manual "restart numbering at N" overrides for this specific list
				// instance (common after section breaks / appendices).
				NodeList overrides = num.getElementsByTagNameNS("*", "lvlOverride");
				for (int j = 0; j < overrides.getLength(); j++) {
					Element override = (Element) overrides.item(j);
					int ilvl = parseIntSafe(override.getAttribute("w:ilvl"), -1);
					Element startOverride = firstDescendant(override, "startOverride");
					if (ilvl >= 0 && startOverride != null) {
						int[] starts = defs.abstractLevelStart.get(abstractId);
						if (starts != null && ilvl < starts.length) {
							int newStart = parseIntSafe(startOverride.getAttribute("w:val"), starts[ilvl]);
							logger.info(
									"{} [numbering.xml] numId={} has startOverride at ilvl={}: start changes {} -> {}",
									TAG, numId, ilvl, starts[ilvl], newStart);
							// Per-numId, not per-abstractId -- copy the start array so we don't
							// mutate a definition shared by other numIds pointing at the same abstract.
							int[] perNumStarts = defs.abstractLevelStart.get(abstractId).clone();
							perNumStarts[ilvl] = newStart;
							defs.abstractLevelStart.put(abstractId, perNumStarts);
						}
					}
				}
			} else {
				logger.info("{} [numbering.xml] <w:num w:numId=\"{}\"> has NO <w:abstractNumId> child", TAG, numId);
			}
		}
		logger.info("{} [numbering.xml] final numId -> abstractId map: {}", TAG, defs.numIdToAbstractId);

		return defs;
	}

	private static Map<String, Element> indexStylesById(Document stylesXml) {
		Map<String, Element> map = new HashMap<>();
		NodeList styles = stylesXml.getElementsByTagNameNS("*", "style");
		for (int i = 0; i < styles.getLength(); i++) {
			Element style = (Element) styles.item(i);
			String id = style.getAttribute("w:styleId");
			if (id != null && !id.isEmpty())
				map.put(id, style);
		}
		return map;
	}

	private static boolean isHeadingParagraph(Element p, Map<String, Element> stylesById) {
		String styleId = headingStyleId(p);
		if (styleId != null && HEADING_STYLE_ID.matcher(styleId).matches())
			return true;
		if (firstDescendant(p, "outlineLvl") != null)
			return true;
		return resolveViaStyleChain(styleId, stylesById, pPr -> firstDescendant(pPr, "outlineLvl")) != null;
	}

	private static String headingStyleId(Element p) {
		Element pStyle = firstDescendant(p, "pStyle");
		return pStyle == null ? null : pStyle.getAttribute("w:val");
	}

	private static int[] directNumPr(Element p) {
		Element numPr = firstDescendant(p, "numPr");
		return numPr == null ? null : readNumPr(numPr);
	}

	private static int[] resolveNumPrViaStyleChain(String styleId, Map<String, Element> stylesById) {
		// ilvl and numId are frequently defined at DIFFERENT points in the basedOn
		// chain: e.g. Heading4 sets only <w:ilvl w:val="3"/> locally, while numId is
		// defined once higher up (often on Heading1) and inherited by every
		// descendant heading level. Resolving "the first <w:numPr> found" as a whole
		// unit (as a naive walk would) breaks this: Heading4's own numPr has ilvl
		// but no numId, so a naive lookup stops there and reports "no numPr" even
		// though a usable numId exists further up the chain.
		Integer ilvl = null;
		Integer numId = null;
		String ilvlFoundAt = null;
		String numIdFoundAt = null;

		Set<String> visited = new HashSet<>();
		String current = styleId;
		while (current != null && visited.add(current)) {
			Element style = stylesById.get(current);
			if (style == null) {
				break;
			}
			Element pPr = firstDescendant(style, "pPr");
			Element numPr = pPr == null ? null : firstDescendant(pPr, "numPr");
			if (numPr != null) {
				if (ilvl == null) {
					Element ilvlEl = firstDescendant(numPr, "ilvl");
					if (ilvlEl != null) {
						ilvl = parseIntSafe(ilvlEl.getAttribute("w:val"), 0);
						ilvlFoundAt = current;
					}
				}
				if (numId == null) {
					Element numIdEl = firstDescendant(numPr, "numId");
					if (numIdEl != null) {
						int v = parseIntSafe(numIdEl.getAttribute("w:val"), -1);
						if (v >= 0) {
							numId = v;
							numIdFoundAt = current;
						}
					}
				}
			}
			if (ilvl != null && numId != null) {
				break;
			}
			Element basedOn = firstDescendant(style, "basedOn");
			current = basedOn == null ? null : basedOn.getAttribute("w:val");
		}

		if (numId == null) {
			return null;
		}
		logger.info(
				"{} [style-chain] resolved for style={}: ilvl={} (found on style \"{}\"), numId={} (found on style \"{}\")",
				TAG, styleId, ilvl, ilvlFoundAt, numId, numIdFoundAt);
		return new int[] { numId, ilvl == null ? 0 : ilvl };
	}

	private static int[] readNumPr(Element numPr) {
		Element numId = firstDescendant(numPr, "numId");
		Element ilvl = firstDescendant(numPr, "ilvl");
		if (numId == null)
			return null;
		int numIdVal = parseIntSafe(numId.getAttribute("w:val"), -1);
		int ilvlVal = ilvl == null ? 0 : parseIntSafe(ilvl.getAttribute("w:val"), 0);
		return numIdVal < 0 ? null : new int[] { numIdVal, ilvlVal };
	}

	private static Element resolveViaStyleChain(String styleId, Map<String, Element> stylesById,
			java.util.function.Function<Element, Element> lookup) {
		Set<String> visited = new HashSet<>();
		while (styleId != null && visited.add(styleId)) {
			Element style = stylesById.get(styleId);
			if (style == null)
				return null;
			Element pPr = firstDescendant(style, "pPr");
			if (pPr != null) {
				Element found = lookup.apply(pPr);
				if (found != null)
					return found;
			}
			Element basedOn = firstDescendant(style, "basedOn");
			styleId = basedOn == null ? null : basedOn.getAttribute("w:val");
		}
		return null;
	}

	private static String cumulativeLevelPattern(int ilvl) {
		StringBuilder sb = new StringBuilder();
		for (int lvl = 1; lvl <= ilvl + 1; lvl++) {
			if (lvl > 1)
				sb.append('.');
			sb.append('%').append(lvl);
		}
		return sb.toString();
	}

	private static int numFmtCode(String val) {
		if (val == null)
			return 0;
		return switch (val) {
		case "lowerLetter" -> 1;
		case "upperLetter" -> 2;
		case "lowerRoman" -> 3;
		case "upperRoman" -> 4;
		default -> 0;
		};
	}

	private static String formatLevelText(String lvlText, int[] counter, int[] fmtCodes) {
		if (lvlText == null)
			return String.valueOf(counter[0]);
		StringBuilder out = new StringBuilder();
		Matcher m = Pattern.compile("%(\\d)").matcher(lvlText);
		int last = 0;
		while (m.find()) {
			out.append(lvlText, last, m.start());
			int level = Integer.parseInt(m.group(1)) - 1;
			int value = level >= 0 && level < counter.length && counter[level] > 0 ? counter[level] : 1;
			int fmt = level >= 0 && level < fmtCodes.length ? fmtCodes[level] : 0;
			out.append(formatNumber(value, fmt));
			last = m.end();
		}
		out.append(lvlText.substring(last));
		return out.toString();
	}

	private static String formatNumber(int value, int fmtCode) {
		return switch (fmtCode) {
		case 1 -> toLetters(value).toLowerCase(Locale.ROOT);
		case 2 -> toLetters(value).toUpperCase(Locale.ROOT);
		case 3 -> toRoman(value).toLowerCase(Locale.ROOT);
		case 4 -> toRoman(value).toUpperCase(Locale.ROOT);
		default -> String.valueOf(value);
		};
	}

	private static String toLetters(int value) {
		StringBuilder sb = new StringBuilder();
		while (value > 0) {
			int rem = (value - 1) % 26;
			sb.insert(0, (char) ('A' + rem));
			value = (value - 1) / 26;
		}
		return sb.length() == 0 ? "A" : sb.toString();
	}

	private static final int[] ROMAN_VALUES = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
	private static final String[] ROMAN_SYMS = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV",
			"I" };

	private static String toRoman(int value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ROMAN_VALUES.length && value > 0; i++) {
			while (value >= ROMAN_VALUES[i]) {
				value -= ROMAN_VALUES[i];
				sb.append(ROMAN_SYMS[i]);
			}
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// markdown injection
	// ------------------------------------------------------------------

	private static String injectIntoMarkdown(String markdown, List<String> numbers) {
		String[] lines = markdown.split("\\r?\\n", -1);
		Deque<String> queue = new ArrayDeque<>(numbers);
		StringBuilder out = new StringBuilder();
		boolean inFence = false;

		int atxLinesSeen = 0;
		int injected = 0;
		int skippedAlreadyNumbered = 0;
		int skippedQueueEmpty = 0;

		for (String line : lines) {
			if (line.startsWith("```")) {
				inFence = !inFence;
				out.append(line).append('\n');
				continue;
			}
			Matcher m = !inFence ? ATX_HEADER.matcher(line.trim()) : null;
			if (m != null && m.matches()) {
				atxLinesSeen++;
				String title = m.group(3);
				if (queue.isEmpty()) {
					skippedQueueEmpty++;
					out.append(line).append('\n');
				} else if (alreadyNumbered(title)) {
					skippedAlreadyNumbered++;
					logger.info("{} [inject] heading line \"{}\" already starts with a number-like pattern, SKIPPING "
							+ "(a number stays unused in the queue for this heading)", TAG, title);
					out.append(line).append('\n');
				} else {
					String number = queue.poll();
					injected++;
					out.append(m.group(1)).append(m.group(2)).append(number).append(" ").append(title).append('\n');
				}
			} else {
				out.append(line).append('\n');
			}
		}

		logger.info(
				"{} [inject] SUMMARY: markdown ATX heading lines seen={}, numbers injected={}, "
						+ "skipped(already numbered)={}, skipped(queue empty)={}, numbers left unused in queue={}",
				TAG, atxLinesSeen, injected, skippedAlreadyNumbered, skippedQueueEmpty, queue.size());

		if (out.length() > 0)
			out.setLength(out.length() - 1);
		return out.toString();
	}

	private static boolean alreadyNumbered(String headingText) {
		return ALREADY_NUMBERED.matcher(headingText).find();
	}

	// ------------------------------------------------------------------
	// XML/zip helpers
	// ------------------------------------------------------------------

	private static Document readZipEntryXml(File docx, String entryName) throws Exception {
		try (ZipFile zip = new ZipFile(docx)) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null)
				return null;
			try (InputStream in = zip.getInputStream(entry)) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				return factory.newDocumentBuilder().parse(in);
			}
		} catch (IOException e) {
			logger.warn("{} readZipEntryXml({}) failed: {}", TAG, entryName, e.toString());
			return null;
		}
	}

	private static Element firstDescendant(Element parent, String localName) {
		NodeList list = parent.getElementsByTagNameNS("*", getLocalName(localName));
		return list.getLength() == 0 ? null : (Element) list.item(0);
	}

	private static String getLocalName(String nameWithPrefix) {
		int index = nameWithPrefix.indexOf(':');
		return (index == -1) ? nameWithPrefix : nameWithPrefix.substring(index + 1);
	}

	private static int parseIntSafe(String s, int fallback) {
		if (s == null || s.isEmpty())
			return fallback;
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String getFirstTextContent(Element parent) {
		NodeList nl = parent.getElementsByTagNameNS("*", "t");
		if (nl.getLength() > 0)
			return nl.item(0).getTextContent().trim();
		return "";
	}
}