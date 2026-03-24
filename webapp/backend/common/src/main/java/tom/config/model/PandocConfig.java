package tom.config.model;

import java.util.List;

public record PandocConfig(String path, String outputFormat, String luaFilter, boolean noHighlight,
		boolean stripComments, String wrap, int headingLevel, List<String> extraArgs, List<String> mimeTypes,
		List<String> extensions) {

}
