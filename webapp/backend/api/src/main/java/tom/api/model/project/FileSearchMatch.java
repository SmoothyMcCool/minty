package tom.api.model.project;

import java.util.List;

public class FileSearchMatch {

	private int line;
	private String text;
	private List<ContextLine> context;

	public FileSearchMatch() {
	}

	public FileSearchMatch(int line, String text, List<ContextLine> context) {
		this.line = line;
		this.text = text;
		this.context = context;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<ContextLine> getContext() {
		return context;
	}

	public void setContext(List<ContextLine> context) {
		this.context = context;
	}
}