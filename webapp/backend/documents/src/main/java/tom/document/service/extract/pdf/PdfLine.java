package tom.document.service.extract.pdf;

public class PdfLine {

	private final String text;
	private final float fontSize;
	private final String fontName;
	private final float x;
	private final float y;

	public PdfLine(String text, float fontSize, String fontName, float x, float y) {
		this.text = text;
		this.fontSize = fontSize;
		this.fontName = fontName;
		this.x = x;
		this.y = y;
	}

	public String getText() {
		return text;
	}

	public float getFontSize() {
		return fontSize;
	}

	public String getFontName() {
		return fontName;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	@Override
	public String toString() {
		return String.format("PdfLine[y=%.1f, fontSize=%.1f, text='%s']", y, fontSize, text);
	}
}
