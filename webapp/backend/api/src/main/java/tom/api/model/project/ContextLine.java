package tom.api.model.project;

public class ContextLine {

	private int line;
	private String text;

	public ContextLine() {
	}

	public ContextLine(int line, String text) {
		this.line = line;
		this.text = text;
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
}