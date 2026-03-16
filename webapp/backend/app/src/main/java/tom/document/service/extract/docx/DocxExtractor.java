package tom.document.service.extract.docx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.FldChar;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;

import jakarta.xml.bind.JAXBElement;

public class DocxExtractor {

	public static String extract(File file) throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.load(file);
		List<Object> content = pkg.getMainDocumentPart().getContent();
		StringBuilder md = new StringBuilder();

		for (Object obj : content) {
			Object unwrapped = unwrap(obj); // <-- unwrap first

			if (unwrapped instanceof P) {
				P paragraph = (P) unwrapped;
				String text = getText(paragraph);

				if (text.isBlank()) {
					continue;
				}

				String style = getStyle(paragraph);
				md.append(applyStyle(style, text)).append("\n\n");
			}

			if (unwrapped instanceof Tbl) {
				md.append(tableToMarkdown((Tbl) unwrapped));
			}
		}

		return md.toString();
	}

	private static Object unwrap(Object obj) {
		if (obj instanceof JAXBElement<?>) {
			return ((JAXBElement<?>) obj).getValue();
		}
		return obj;
	}

	private static String applyStyle(String style, String text) {
		if (style == null) {
			return text;
		}

		switch (style) {

		case "Heading1":
			return "# " + text;

		case "Heading2":
			return "## " + text;

		case "Heading3":
			return "### " + text;

		case "Heading4":
			return "#### " + text;

		default:
			return text;
		}
	}

	private static String getStyle(P paragraph) {
		if (paragraph.getPPr() == null) {
			return null;
		}
		if (paragraph.getPPr().getPStyle() == null) {
			return null;
		}
		return paragraph.getPPr().getPStyle().getVal();
	}

	private static String getText(P paragraph) {
		StringBuilder sb = new StringBuilder();
		boolean inFieldInstruction = false;

		for (Object o : paragraph.getContent()) {
			Object unwrapped = unwrap(o);

			if (unwrapped instanceof R) {
				inFieldInstruction = appendRunText(sb, (R) unwrapped, inFieldInstruction);
			}

			if (unwrapped instanceof P.Hyperlink) {
				P.Hyperlink hyperlink = (P.Hyperlink) unwrapped;
				for (Object hObj : hyperlink.getContent()) {
					Object unwrappedH = unwrap(hObj);
					if (unwrappedH instanceof R) {
						inFieldInstruction = appendRunText(sb, (R) unwrappedH, inFieldInstruction);
					}
				}
			}
		}

		return sb.toString().trim();
	}

	private static boolean appendRunText(StringBuilder sb, R run, boolean inFieldInstruction) {
		for (Object r : run.getContent()) {
			Object unwrappedR = unwrap(r);

			if (unwrappedR instanceof FldChar) {
				String type = ((FldChar) unwrappedR).getFldCharType().value();
				if ("begin".equals(type))
					return true;
				if ("separate".equals(type))
					return false;
				if ("end".equals(type))
					return false;
				continue;
			}

			if (inFieldInstruction)
				continue;

			if (unwrappedR instanceof Text) {
				sb.append(((Text) unwrappedR).getValue());
			}
		}
		return inFieldInstruction;
	}

	private static String tableToMarkdown(Tbl table) {
		StringBuilder md = new StringBuilder();
		boolean headerWritten = false;

		for (Object rowObj : table.getContent()) {
			Object unwrappedRow = unwrap(rowObj);
			if (!(unwrappedRow instanceof Tr)) {
				continue;
			}

			Tr row = (Tr) unwrappedRow;
			List<String> values = new ArrayList<>();

			for (Object cellObj : row.getContent()) {
				Object unwrappedCell = unwrap(cellObj);
				if (!(unwrappedCell instanceof Tc)) {
					continue;
				}
				values.add(extractCellText((Tc) unwrappedCell));
			}

			if (values.isEmpty())
				continue;

			md.append("| ").append(String.join(" | ", values)).append(" |\n");

			if (!headerWritten) {
				md.append("|");
				for (int i = 0; i < values.size(); i++)
					md.append(" --- |");
				md.append("\n");
				headerWritten = true;
			}
		}

		md.append("\n");
		return md.toString();
	}

	private static String extractCellText(Tc cell) {
		StringBuilder sb = new StringBuilder();

		for (Object obj : cell.getContent()) {
			Object unwrapped = unwrap(obj);
			if (unwrapped instanceof P) {
				String text = getText((P) unwrapped);
				if (!text.isBlank()) {
					sb.append(text).append(" ");
				}
			}
		}

		return sb.toString().trim().replace("|", "\\|");
	}

}
