package tom.api.model.document;

import java.util.List;

public class DocumentSearchMatch {

	private int section;
	private String sectionTitle;
	private String text;
	private List<DocumentSearchContext> context;

	public DocumentSearchMatch() {
	}

	public DocumentSearchMatch(int section, String sectionTitle, String text, List<DocumentSearchContext> context) {
		this.section = section;
		this.sectionTitle = sectionTitle;
		this.text = text;
		this.context = context;
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

	public List<DocumentSearchContext> getContext() {
		return context;
	}

	public void setContext(List<DocumentSearchContext> context) {
		this.context = context;
	}
}