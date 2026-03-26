package tom.document.service.extract.spreadsheet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import tom.document.SpreadsheetFormat;

public class SpreadsheetExtractor {

	public static String extract(File file, SpreadsheetFormat format) throws IOException {
		String name = file.getName().toLowerCase();
		char delimiter = name.endsWith(".tsv") || name.endsWith(".tab") ? '\t' : ',';

		if (name.endsWith(".csv") || name.endsWith(".tsv") || name.endsWith(".tab")) {
			return extractDelimited(file, delimiter, format);
		}

		StringBuilder out = new StringBuilder();
		try (Workbook workbook = WorkbookFactory.create(file)) {
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			for (Sheet sheet : workbook) {
				if (format == SpreadsheetFormat.MARKDOWN) {
					extractMarkdown(sheet, formatter, evaluator, out);
				} else {
					extractTsv(sheet, formatter, evaluator, out);
				}
			}
		}
		return out.toString();
	}

	private static void extractMarkdown(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator,
			StringBuilder out) {

		boolean headerWritten = false;

		for (Row row : sheet) {
			int lastColumn = row.getLastCellNum();
			List<String> cells = new ArrayList<>();

			for (int col = 0; col < lastColumn; col++) {
				Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				String value = "";

				if (cell != null) {
					value = formatter.formatCellValue(cell, evaluator);
				}

				cells.add(sanitize(value));
			}

			out.append("| ").append(String.join(" | ", cells)).append(" |\n");

			if (!headerWritten) {
				out.append("|");
				for (int i = 0; i < cells.size(); i++) {
					out.append(" --- |");
				}
				out.append("\n");

				headerWritten = true;
			}
		}

		out.append("\n");
	}

	private static void extractTsv(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator,
			StringBuilder out) {

		for (Row row : sheet) {
			int lastColumn = row.getLastCellNum();

			for (int col = 0; col < lastColumn; col++) {
				Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				String value = "";

				if (cell != null) {
					value = formatter.formatCellValue(cell, evaluator);
				}

				out.append(sanitize(value));

				if (col < lastColumn - 1) {
					out.append("\t");
				}
			}

			out.append("\n");
		}
	}

	private static String extractDelimited(File file, char delimiter, SpreadsheetFormat format) throws IOException {
		List<List<String>> rows = new ArrayList<>();
		int maxCols = 0;

		try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank())
					continue;
				List<String> cells = splitDelimited(line, delimiter);
				maxCols = Math.max(maxCols, cells.size());
				rows.add(cells);
			}
		}

		if (rows.isEmpty())
			return "";

		StringBuilder out = new StringBuilder();
		if (format == SpreadsheetFormat.MARKDOWN) {
			// Header row
			List<String> header = rows.get(0);
			out.append("| ").append(String.join(" | ", header)).append(" |\n");
			out.append("|");
			for (int i = 0; i < header.size(); i++)
				out.append(" --- |");
			out.append("\n");
			// Data rows
			for (int i = 1; i < rows.size(); i++) {
				List<String> row = rows.get(i);
				out.append("| ");
				for (int col = 0; col < header.size(); col++) {
					out.append(col < row.size() ? sanitize(row.get(col)) : "");
					out.append(" | ");
				}
				out.append("\n");
			}
		} else {
			for (List<String> row : rows) {
				out.append(String.join("\t", row)).append("\n");
			}
		}

		out.append("\n");
		return out.toString();
	}

	/**
	 * Splits a delimited line respecting quoted fields. e.g. a,"b,c",d -> [a, b,c,
	 * d]
	 */
	private static List<String> splitDelimited(String line, char delimiter) {
		List<String> cells = new ArrayList<>();
		StringBuilder cell = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				// Handle escaped quote ""
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					cell.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == delimiter && !inQuotes) {
				cells.add(sanitize(cell.toString()));
				cell.setLength(0);
			} else {
				cell.append(c);
			}
		}
		cells.add(sanitize(cell.toString()));
		return cells;
	}

	private static String sanitize(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\t", " ").replace("\r", "").replace("\n", " ").replace("\u00A0", " ").trim();
	}
}