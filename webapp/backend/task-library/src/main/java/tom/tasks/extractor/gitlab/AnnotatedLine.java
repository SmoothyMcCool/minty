package tom.tasks.extractor.gitlab;

public class AnnotatedLine {

	String content;
	String type; // "added", "removed", "unchanged"

	AnnotatedLine(String content, String type) {
		this.content = content;
		this.type = type;
	}

}
