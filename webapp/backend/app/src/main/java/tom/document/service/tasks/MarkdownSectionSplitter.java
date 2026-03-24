package tom.document.service.tasks;

import java.util.ArrayList;
import java.util.List;

import tom.api.services.document.extract.Section;

public class MarkdownSectionSplitter {

	public static List<Section> split(String markdown, int maxHeadingLevel) {
		List<Section> sections = new ArrayList<>();

		// Normalize: handle \r\n, \r, and strip any BOM
		String normalized = markdown.replace("\uFEFF", "") // strip BOM
				.replace("\r\n", "\n") // Windows line endings
				.replace("\r", "\n"); // old Mac line endings

		String[] lines = normalized.split("\n");
		Section current = null;
		StringBuilder preamble = new StringBuilder();

		for (String line : lines) {
			// Strip trailing whitespace including \r, NBSP, etc.
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

		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).index = i + 1;
		}
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).parentIndex = findParentIndex(sections, i);
		}

		return sections;
	}

	private static boolean isHeading(String line, int maxHeadingLevel) {
		if (!line.startsWith("#")) {
			return false;
		}
		// Count hashes
		int i = 0;
		while (i < line.length() && line.charAt(i) == '#') {
			i++;
		}
		// Must be 1-6 hashes followed by whitespace
		return i <= maxHeadingLevel && i < line.length() && Character.isWhitespace(line.charAt(i));
	}

	private static int countLeadingHashes(String line) {
		int count = 0;
		while (count < line.length() && line.charAt(count) == '#') {
			count++;
		}
		return count;
	}

	/**
	 * Walks backwards from the current section to find the nearest section with a
	 * lower heading level — that's the parent.
	 */
	private static Integer findParentIndex(List<Section> sections, int i) {
		int currentLevel = sections.get(i).level;
		if (currentLevel == 0) {
			return null; // preamble has no parent
		}
		for (int j = i - 1; j >= 0; j--) {
			if (sections.get(j).level < currentLevel) {
				return sections.get(j).index;
			}
		}
		return null;
	}
}