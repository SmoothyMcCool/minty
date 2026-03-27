package tom.document.markdown;

import java.util.ArrayList;
import java.util.List;

import tom.document.Section;

public class MarkdownSectionSplitter {

	public static List<Section> split(String markdown, int maxHeadingLevel, int minimumSectionSize) {
		List<Section> sections = new ArrayList<>();

		String normalized = markdown.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n");

		String[] lines = normalized.split("\n");

		Section current = null;
		StringBuilder preamble = new StringBuilder();

		for (String line : lines) {
			String trimmedLine = line.stripTrailing();
			if (isHeading(trimmedLine, maxHeadingLevel)) {
				if (current != null) {
					current.content = current.content.trim();
					sections.add(current);
				}
				current = new Section();
				current.level = countLeadingHashes(trimmedLine);
				current.title = trimmedLine.replaceFirst("^#{1," + maxHeadingLevel + "}\\s+", "").trim();
				current.content = "";
			} else if (current != null) {
				current.content += line + "\n";
			} else {
				preamble.append(line).append("\n");
			}
		}

		if (current != null) {
			current.content = current.content.trim();
			sections.add(current);
		}

		String preambleText = preamble.toString().trim();
		if (!preambleText.isEmpty()) {
			Section pre = new Section();
			pre.level = 0;
			pre.title = "Preamble";
			pre.content = preambleText;
			sections.add(0, pre);
		}

		// Combine very small sections together.
		sections = collapse(sections, minimumSectionSize);

		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).index = i + 1;
		}
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).parentIndex = findParentIndex(sections, i);
		}

		return sections;
	}

	/**
	 * Collapses sections that are shorter than minChars into their predecessor. The
	 * title of the short section is folded in as a Markdown heading so the content
	 * remains navigable, and the predecessor's title is kept. Preamble sections
	 * (level 0) are never used as a collapse target.
	 */
	private static List<Section> collapse(List<Section> sections, int minChars) {
		if (minChars <= 0) {
			return sections;
		}

		List<Section> result = new ArrayList<>();

		for (Section section : sections) {
			boolean isTooShort = contentLength(section) < minChars;
			Section target = lastCollapsibleSection(result, section.level);

			if (isTooShort && target != null) {
				// Prepend the target's own heading the first time it absorbs a section
				if (!target.content.startsWith("#")) {
					String targetHeading = "#".repeat(target.level) + " " + target.title;
					target.content = target.content.isBlank() ? targetHeading : targetHeading + "\n\n" + target.content;
				}
				String incomingHeading = "#".repeat(section.level) + " " + section.title;
				target.content = target.content.isBlank() ? incomingHeading + "\n\n" + section.content
						: target.content + "\n\n" + incomingHeading + "\n\n" + section.content;
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
	private static Section lastCollapsibleSection(List<Section> sections, int incomingLevel) {
		for (int i = sections.size() - 1; i >= 0; i--) {
			Section candidate = sections.get(i);
			if (candidate.level > 0 && candidate.level <= incomingLevel) {
				return candidate;
			}
		}
		return null;
	}

	private static int contentLength(Section section) {
		return section.content == null ? 0 : section.content.length();
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

	private static Integer findParentIndex(List<Section> sections, int i) {
		int currentLevel = sections.get(i).level;
		if (currentLevel == 0) {
			return null;
		}
		for (int j = i - 1; j >= 0; j--) {
			if (sections.get(j).level < currentLevel) {
				return sections.get(j).index;
			}
		}
		return null;
	}
}