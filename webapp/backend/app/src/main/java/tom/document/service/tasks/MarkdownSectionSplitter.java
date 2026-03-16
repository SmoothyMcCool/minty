package tom.document.service.tasks;

import java.util.ArrayList;
import java.util.List;

public class MarkdownSectionSplitter {

	public static class Section {
		public int index;
		public int level;
		public String title;
		public String content;
		public Integer parentIndex; // null if top-level
	}

	public static List<Section> split(String markdown) {
		List<Section> sections = new ArrayList<>();
		String[] lines = markdown.split("\n");

		Section current = null;
		StringBuilder preamble = new StringBuilder();

		for (String line : lines) {

			if (line.matches("^#{1,3} .*")) {

				if (current != null) {
					current.content = current.content.trim();
					sections.add(current);
				}

				current = new Section();
				current.level = countLeadingHashes(line);
				current.title = line.replaceFirst("^#{1,3} ", "").trim();
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

		// Add preamble as a level-0 section if there was content before the first
		// heading
		String preambleText = preamble.toString().trim();
		if (!preambleText.isEmpty()) {
			Section pre = new Section();
			pre.level = 0;
			pre.title = "Preamble";
			pre.content = preambleText;
			sections.add(0, pre);
		}

		// Assign indices and resolve parent relationships
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).index = i + 1;
		}
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).parentIndex = findParentIndex(sections, i);
		}

		return sections;
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
		for (int j = i - 1; j >= 0; j--) {
			if (sections.get(j).level < currentLevel) {
				return sections.get(j).index;
			}
		}
		return null;
	}
}