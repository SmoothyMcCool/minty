package tom.api.model.project;

import java.util.List;

import tom.api.model.document.DocumentSearchContext;

public class KnowledgeSearchMatch {

	private Integer line;
	private Integer section;
	private String title;
	private String text;

	private List<ContextLine> fileContext;
	private List<DocumentSearchContext> documentContext;

	public KnowledgeSearchMatch() {
	}

	public KnowledgeSearchMatch(Integer line, Integer section, String title, String text, List<ContextLine> fileContext,
			List<DocumentSearchContext> documentContext) {

		this.line = line;
		this.section = section;
		this.title = title;
		this.text = text;
		this.fileContext = fileContext;
		this.documentContext = documentContext;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public Integer getSection() {
		return section;
	}

	public void setSection(Integer section) {
		this.section = section;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<ContextLine> getFileContext() {
		return fileContext;
	}

	public void setFileContext(List<ContextLine> fileContext) {
		this.fileContext = fileContext;
	}

	public List<DocumentSearchContext> getDocumentContext() {
		return documentContext;
	}

	public void setDocumentContext(List<DocumentSearchContext> documentContext) {
		this.documentContext = documentContext;
	}
}