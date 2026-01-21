package tom.render.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class HelperFunctions {

	static ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(SerializationFeature.INDENT_OUTPUT).build();

	public String jsonToString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return "";
		}
	}

	public boolean isHtml(String str) {
		if (str == null || str.trim().isEmpty()) {
			return false;
		}
		Document doc = Jsoup.parse(str);
		// If the body has any element nodes, treat as HTML
		return !doc.body().children().isEmpty();
	}

	public String makeSafe(String html) {
		return Jsoup.clean(html, Safelist.relaxed());
	}

	public String stripMarkdown(String text) {
		return text.replaceAll("(?s)^\\s*```[a-zA-Z0-9_-]*\\s*", "").replaceAll("(?s)\\s*```\\s*$", "").trim();
	}

	public String tryMakeSafe(String text) {
		if (text == null || text.trim().isEmpty())
			return null;

		String noMd = stripMarkdown(text);
		if (isHtml(noMd)) {
			return makeSafe(noMd);
		}

		// Still nothing – return null so the caller can fall back to plain text
		return null;
	}
}
