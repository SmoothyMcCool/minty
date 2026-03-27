package tom.document.extract.pdf;

import java.util.List;

public class PdfMarkdownRenderer {

	public static String render(List<PdfBlock> blocks) {

		StringBuilder md = new StringBuilder();

		for (PdfBlock block : blocks) {

			if (block instanceof PdfBlock.Heading h) {
				md.append("#".repeat(h.getLevel())).append(" ").append(h.getText()).append("\n\n");
			}

			else if (block instanceof PdfBlock.Paragraph p) {
				md.append(p.getText()).append("\n\n");
			}

			else if (block instanceof PdfBlock.ListBlock l) {
				for (String item : l.getItems()) {
					md.append("- ").append(item).append("\n");
				}
				md.append("\n");
			}

			else if (block instanceof PdfBlock.Table t) {
				renderTable(md, t.getRows());
			}
		}

		return md.toString();
	}

	private static void renderTable(StringBuilder md, List<List<String>> rows) {

		if (rows.isEmpty())
			return;

		int cols = rows.get(0).size();

		// Header row
		md.append("| ");
		for (String cell : rows.get(0)) {
			md.append(escape(cell)).append(" | ");
		}
		md.append("\n");

		// Separator
		md.append("|");
		for (int i = 0; i < cols; i++) {
			md.append(" --- |");
		}
		md.append("\n");

		// Data rows
		for (int i = 1; i < rows.size(); i++) {
			List<String> row = rows.get(i);
			md.append("| ");
			for (int j = 0; j < cols; j++) {
				// Pad with empty cell if this row has fewer columns than the header
				String cell = j < row.size() ? row.get(j) : "";
				md.append(escape(cell)).append(" | ");
			}
			md.append("\n");
		}

		md.append("\n");
	}

	private static String escape(String cell) {
		return cell.replace("|", "\\|");
	}
}
