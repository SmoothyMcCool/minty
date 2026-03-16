package tom.document.service.extract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class HtmlToMarkdown {

	public static String convert(String html) {
		Document doc = Jsoup.parse(html);
		return block(doc.body()).trim() + "\n";
	}

	private static String block(Element root) {
		StringBuilder md = new StringBuilder();

		for (Node node : root.childNodes()) {

			if (!(node instanceof Element)) {
				continue;
			}

			Element el = (Element) node;

			switch (el.tagName()) {

			case "h1":
				md.append("# ").append(inline(el)).append("\n\n");
				break;
			case "h2":
				md.append("## ").append(inline(el)).append("\n\n");
				break;
			case "h3":
				md.append("### ").append(inline(el)).append("\n\n");
				break;
			case "h4":
				md.append("#### ").append(inline(el)).append("\n\n");
				break;

			case "p":
				paragraph(md, inline(el));
				break;

			case "ul":
				md.append(list(el, false, 0));
				break;

			case "ol":
				md.append(list(el, true, 0));
				break;

			case "table":
				md.append(table(el)).append("\n\n");
				break;

			case "img":
				md.append("\n\n![](").append(el.attr("src")).append(")\n\n");
				break;

			default:
				md.append(block(el));
			}
		}

		return normalize(md.toString());
	}

	private static String list(Element list, boolean ordered, int depth) {
		StringBuilder md = new StringBuilder();

		int index = 1;

		for (Element li : list.select("> li")) {
			String indent = "  ".repeat(depth);
			String prefix = ordered ? (index++) + ". " : "- ";
			String text = normalizeBulletChars(inline(li).trim());

			if (!text.isEmpty()) {
				md.append(indent).append(prefix).append(text).append("\n");
			}

			for (Element sub : li.children()) {
				if (sub.tagName().equals("ul")) {
					md.append(list(sub, false, depth + 1));
				}

				if (sub.tagName().equals("ol")) {
					md.append(list(sub, true, depth + 1));
				}
			}
		}

		md.append("\n"); // extra newline after list
		return md.toString();
	}

	private static String inline(Node node) {

		if (node instanceof TextNode) {
			return escapeMarkdownText(((TextNode) node).text());
		}

		if (!(node instanceof Element)) {
			return "";
		}

		Element el = (Element) node;

		switch (el.tagName()) {

		case "strong":
		case "b":
			return "**" + children(el).trim().replaceAll("\\s+", " ") + "**";

		case "em":
		case "i":
			return "_" + children(el).trim().replaceAll("\\s+", " ") + "_";

		case "code":
			return "`" + escapeMarkdownText(el.text()) + "`";

		case "a":

			String href = el.attr("href");
			String text = children(el);

			if (isValidLink(href, text))
				return "[" + text + "](" + href + ")";

			return text;

		default:
			return children(el);
		}
	}

	private static String children(Element el) {

		StringBuilder out = new StringBuilder();

		for (Node child : el.childNodes())
			out.append(inline(child));

		return out.toString();
	}

	private static String table(Element table) {
		StringBuilder md = new StringBuilder();
		Elements rows = table.select("tr");

		if (rows.isEmpty()) {
			return "";
		}

		Elements header = rows.get(0).select("th,td");

		for (Element h : header) {
			md.append("| ").append(escapeTableCell(inline(h))).append(" ");
		}
		md.append("|\n");

		for (int i = 0; i < header.size(); i++) {
			md.append("|---");
		}
		md.append("|\n");

		for (int r = 1; r < rows.size(); r++) {
			for (Element c : rows.get(r).select("td")) {
				md.append("| ").append(escapeTableCell(inline(c))).append(" ");
			}
			md.append("|\n");
		}

		return md.toString();
	}

	private static void paragraph(StringBuilder md, String text) {

		text = text.trim();
		if (text.isEmpty()) {
			return;
		}

		md.append(text).append("\n\n");
	}

	private static boolean isValidLink(String href, String text) {

		if (href == null) {
			return false;
		}

		href = href.toLowerCase();

		if (!(href.startsWith("http://") || href.startsWith("https://") || href.startsWith("mailto:"))) {
			return false;
		}

		return text.length() < 200;
	}

	private static String normalize(String text) {

		return text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n");
	}

	private static String escapeMarkdownText(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String escapeTableCell(String text) {
		if (text == null) {
			return "";
		}
		return escapeMarkdownText(text.trim()).replace("|", "\\|").replace("\n", " ").replace("\r", " ");
	}

	private static String normalizeBulletChars(String text) {
		if (text == null)
			return "";
		return text.replace("\u2022", "-") // • to -
				.replace("\u25E6", "-") // ◦ to -
				.replace("\u00B7", "-"); // · to -
	}
}