package tom.tasks.transform.confluence.model;

public class SearchResult {

	private String id;
	private String title;
	private String space;
	private String excerpt;

	// No-arg constructor
	public SearchResult() {
	}

	// All-args constructor
	public SearchResult(String id, String title, String space, String excerpt) {
		this.id = id;
		this.title = title;
		this.space = space;
		this.excerpt = excerpt;
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

	public String getExcerpt() {
		return excerpt;
	}

	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}
}
