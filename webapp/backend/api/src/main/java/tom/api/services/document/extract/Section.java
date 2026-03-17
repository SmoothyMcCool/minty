package tom.api.services.document.extract;

public class Section {
	public int index;
	public int level;
	public String title;
	public String content;
	public Integer parentIndex; // null if top-level
}