package tom.config.model;

import java.util.List;

public record ParsingConfig(String pandocPath, List<String> pandocMimeTypes, List<String> pandocExtensions) {

}
