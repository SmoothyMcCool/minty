package tom.document.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import tom.api.DocumentId;
import tom.api.DocumentSectionId;
import tom.api.model.document.DocumentSection;

@Entity
@Table(name = "DocumentSegment")
public class MintyDocSegment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "documentId", nullable = false)
	private MintyDoc document;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(name = "sequenceOrder", nullable = false)
	private int sequenceOrder;

	@Column(name = "parentIndex", nullable = true)
	private Integer parentIndex;

	@Column(name = "level", nullable = false)
	private int level;

	@Column(name = "title")
	private String title;

	@Column(name = "created", updatable = false)
	private LocalDateTime created;

	public MintyDocSegment() {
	}

	@PrePersist
	protected void onCreate() {
		created = LocalDateTime.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public MintyDoc getDocument() {
		return document;
	}

	public void setDocument(MintyDoc document) {
		this.document = document;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getSequenceOrder() {
		return sequenceOrder;
	}

	public void setSequenceOrder(int sequenceOrder) {
		this.sequenceOrder = sequenceOrder;
	}

	public int getParentIndex() {
		return parentIndex;
	}

	public void setParentIndex(Integer parentIndex) {
		this.parentIndex = parentIndex;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public DocumentSection fromEntityNoContent() {
		return new DocumentSection(new DocumentSectionId(id), getDocumentId(), sequenceOrder, parentIndex, level, title,
				null, created);
	}

	public DocumentSection fromEntityWithContent() {
		return new DocumentSection(new DocumentSectionId(id), getDocumentId(), sequenceOrder, parentIndex, level, title,
				content, created);
	}

	@Transient
	private DocumentId getDocumentId() {
		return document != null ? new DocumentId(document.getId()) : null;
	}
}
