package tom.api.model.project;

public class KnowledgeItemInfo {

	private KnowledgeItemType type;
	private String name;
	private String path;
	private String description;

	public KnowledgeItemInfo() {
	}

	public KnowledgeItemInfo(KnowledgeItemType type, String name, String path, String description) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.description = description;
	}

	public KnowledgeItemType getType() {
		return type;
	}

	public void setType(KnowledgeItemType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}