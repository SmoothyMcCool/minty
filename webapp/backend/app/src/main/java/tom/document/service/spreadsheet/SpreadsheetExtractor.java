package tom.document.service.spreadsheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class SpreadsheetExtractor {

	public static String extract(byte[] bytes) throws IOException {
		StringBuilder out = new StringBuilder();

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			for (Sheet sheet : workbook) {
				Iterator<Row> rows = sheet.iterator();

				while (rows.hasNext()) {
					Row row = rows.next();
					int lastColumn = row.getLastCellNum();
					out.append("\t"); // match Tika's leading tab format

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
		}

		return out.toString();
	}

	private static String sanitize(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\t", " ").replace("\r", "").replace("\n", " ").replace("\u00A0", " ").trim();
	}
}