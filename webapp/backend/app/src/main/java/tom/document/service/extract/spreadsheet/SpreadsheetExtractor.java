package tom.document.service.extract.spreadsheet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import tom.api.services.document.SpreadsheetFormat;

public class SpreadsheetExtractor {

	public static String extract(File file, SpreadsheetFormat format) throws IOException {
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

	private static String sanitize(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\t", " ").replace("\r", "").replace("\n", " ").replace("\u00A0", " ").trim();
	}
}