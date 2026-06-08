package tom.document.markdown;

import java.util.ArrayList;
import java.util.List;

import tom.api.model.document.DocumentSection;

public class MarkdownSectionSplitter {

	public static List<DocumentSection> split(String markdown, int maxHeadingLevel, int minimumSectionSize) {
		List<DocumentSection> sections = new ArrayList<>();

		String normalized = markdown.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n");

		String[] lines = normalized.split("\n");

		DocumentSection.Builder current = null;
		StringBuilder curContent = new StringBuilder();
		StringBuilder preamble = new StringBuilder();

		for (String line : lines) {
			String trimmedLine = line.stripTrailing();

			if (isHeading(trimmedLine, maxHeadingLevel)) {

				if (current != null) {
					current.content(curContent.toString().trim());
					sections.add(current.build());
				}

				current = DocumentSection.builder();
				int lvl = countLeadingHashes(trimmedLine);
				String title = trimmedLine.replaceFirst("^#{1," + maxHeadingLevel + "}\\s+", "").trim();

				current.level(lvl).title(title);

				curContent.setLength(0);
			} else if (current != null) {
				curContent.append(line).append('\n');
			} else {
				preamble.append(line).append('\n');
			}
		}

		if (current != null) {
			current.content(curContent.toString().trim());
			sections.add(current.build());
		}

		String preambleText = preamble.toString().trim();
		if (!preambleText.isEmpty()) {
			DocumentSection.Builder dsb = DocumentSection.builder();
			dsb.level(0);
			dsb.title("Preamble");
			dsb.content(preambleText);
			sections.add(0, dsb.build());
		}

		// Combine very small sections together.
		sections = collapse(sections, minimumSectionSize);

		List<DocumentSection> updated = new ArrayList<>(sections.size());
		for (int i = 0; i < sections.size(); i++) {
			DocumentSection old = sections.get(i);
			int sequenceOrder = i + 1;
			Integer parentIdx = findParentIndex(sections, i);
			updated.add(old.toBuilder().sequenceOrder(sequenceOrder).title("Section " + sequenceOrder)
					.parentIndex(parentIdx).build());
		}

		return updated;
	}

	/**
	 * Collapses sections that are shorter than minChars into their predecessor. The
	 * title of the short section is folded in as a Markdown heading so the content
	 * remains navigable, and the predecessor's title is kept. Preamble sections
	 * (level 0) are never used as a collapse target.
	 */
	private static List<DocumentSection> collapse(List<DocumentSection> sections, int minChars) {
		if (minChars <= 0) {
			return sections;
		}

		List<DocumentSection> result = new ArrayList<>();

		for (DocumentSection section : sections) {
			boolean isTooShort = contentLength(section) < minChars;
			DocumentSection target = lastCollapsibleSection(result, section.level());

			if (isTooShort && target != null) {
				int targetIndex = result.indexOf(target);

				String mergedContent = target.content();

				// Prepend the target's own heading the first time it absorbs a section
				if (!mergedContent.startsWith("#")) {
					String targetHeading = "#".repeat(target.level()) + " " + target.title();
					mergedContent = mergedContent.isBlank() ? targetHeading : targetHeading + "\n\n" + mergedContent;
				}

				String incomingHeading = "#".repeat(section.level()) + " " + section.title();

				if (mergedContent.isBlank()) {
					mergedContent = incomingHeading + "\n\n" + section.content();
				} else {
					mergedContent = mergedContent + "\n\n" + incomingHeading + "\n\n" + section.content();
				}

				DocumentSection merged = target.toBuilder().content(mergedContent).build();

				result.set(targetIndex, merged);

			} else {
				result.add(section);
			}
		}

		return result;
	}

	/**
	 * Returns the last section in the list that is a valid collapse target: not a
	 * preamble, and at an equal or higher level (lower number) than the incoming
	 * section so we never fold a parent under a child.
	 */
	private static DocumentSection lastCollapsibleSection(List<DocumentSection> sections, int incomingLevel) {
		for (int i = sections.size() - 1; i >= 0; i--) {
			DocumentSection candidate = sections.get(i);
			if (candidate.level() > 0 && candidate.level() <= incomingLevel) {
				return candidate;
			}
		}
		return null;
	}

	private static int contentLength(DocumentSection section) {
		return section.content() == null ? 0 : section.content().length();
	}

	private static boolean isHeading(String line, int maxHeadingLevel) {
		if (!line.startsWith("#")) {
			return false;
		}
		int i = 0;
		while (i < line.length() && line.charAt(i) == '#') {
			i++;
		}
		return i <= maxHeadingLevel && i < line.length() && Character.isWhitespace(line.charAt(i));
	}

	private static int countLeadingHashes(String line) {
		int count = 0;
		while (count < line.length() && line.charAt(count) == '#') {
			count++;
		}
		return count;
	}

	private static Integer findParentIndex(List<DocumentSection> sections, int i) {
		int currentLevel = sections.get(i).level();
		if (currentLevel == 0) {
			return null;
		}
		for (int j = i - 1; j >= 0; j--) {
			if (sections.get(j).level() < currentLevel) {
				return sections.get(j).sequenceOrder();
			}
		}
		return null;
	}
}