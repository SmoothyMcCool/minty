package tom.tasks.transform.confluence.model;

import java.util.List;

public class PageResponse {

	private String id;
	private String title;
	private String space;
	private List<String> labels;
	private String bodyText;
	private String url;

	// No-arg constructor
	public PageResponse() {
	}

	// All-args constructor
	public PageResponse(String id, String title, String space, List<String> labels, String bodyText, String url) {
		this.id = id;
		this.title = title;
		this.space = space;
		this.labels = labels;
		this.bodyText = bodyText;
		this.url = url;
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public String getBodyText() {
		return bodyText;
	}

	public void setBodyText(String bodyText) {
		this.bodyText = bodyText;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
