package tom.tag.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import tom.document.model.MintyDoc;

@Entity(name = "Tag")
public class MintyTag {

	@Id
	private UUID id;
	private String tag;

	@ManyToMany
	@JoinTable(name = "TagToDoc", joinColumns = @JoinColumn(name = "tagId"), inverseJoinColumns = @JoinColumn(name = "documentId"))
	private List<MintyDoc> associatedDocuments;

	public MintyTag() {
	}

	public MintyTag(UUID id, String tag) {
		this.id = id;
		this.tag = tag;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public List<MintyDoc> getAssociatedDocuments() {
		return associatedDocuments;
	}

	public void setAssociatedDocuments(List<MintyDoc> associatedDocuments) {
		this.associatedDocuments = associatedDocuments;
	}

}
