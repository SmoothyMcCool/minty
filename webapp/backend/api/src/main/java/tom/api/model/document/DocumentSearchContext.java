package tom.api.model.document;

public class DocumentSearchContext {

	private int section;
	private String sectionTitle;
	private String text;

	public DocumentSearchContext() {
	}

	public DocumentSearchContext(int section, String sectionTitle, String text) {
		this.section = section;
		this.sectionTitle = sectionTitle;
		this.text = text;
	}

	public int getSection() {
		return section;
	}

	public void setSection(int section) {
		this.section = section;
	}

	public String getSectionTitle() {
		return sectionTitle;
	}

	public void setSectionTitle(String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}