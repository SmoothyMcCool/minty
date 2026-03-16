package tom.document.service.extract.pdf;

import java.util.List;

public interface PdfBlock {

	class Heading implements PdfBlock {
		private final int level;
		private final String text;

		public Heading(int level, String text) {
			this.level = level;
			this.text = text;
		}

		public int getLevel() {
			return level;
		}

		public String getText() {
			return text;
		}
	}

	class Paragraph implements PdfBlock {
		private final String text;

		public Paragraph(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	class ListBlock implements PdfBlock {
		private final List<String> items;

		public ListBlock(List<String> items) {
			this.items = items;
		}

		public List<String> getItems() {
			return items;
		}
	}

	class Table implements PdfBlock {
		private final List<List<String>> rows;

		public Table(List<List<String>> rows) {
			this.rows = rows;
		}

		public List<List<String>> getRows() {
			return rows;
		}
	}
}
