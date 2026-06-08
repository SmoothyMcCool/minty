package tom.api.model.document;

import java.time.LocalDateTime;

import tom.api.DocumentId;
import tom.api.DocumentSectionId;

public record DocumentSection(DocumentSectionId id, DocumentId documentId, int sequenceOrder, Integer parentIndex,
		int level, String title, String content, LocalDateTime created) {

	/**
	 * Static method to initiate the builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().id(id).documentId(documentId).sequenceOrder(sequenceOrder).parentIndex(parentIndex)
				.level(level).title(title).content(content).created(created);
	}

	/**
	 * Manual Builder Implementation
	 */
	public static class Builder {
		private DocumentSectionId id;
		private DocumentId documentId;
		private int sequenceOrder;
		private Integer parentIndex;
		private int level;
		private String title;
		private String content;
		private LocalDateTime created;

		public Builder id(DocumentSectionId id) {
			this.id = id;
			return this;
		}

		public Builder documentId(DocumentId documentId) {
			this.documentId = documentId;
			return this;
		}

		public Builder sequenceOrder(int sequenceOrder) {
			this.sequenceOrder = sequenceOrder;
			return this;
		}

		public Builder parentIndex(Integer parentIndex) {
			this.parentIndex = parentIndex;
			return this;
		}

		public Builder level(int level) {
			this.level = level;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder created(LocalDateTime created) {
			this.created = created;
			return this;
		}

		/**
		 * Finalizes the construction by calling the record's canonical constructor
		 */
		public DocumentSection build() {
			return new DocumentSection(id, documentId, sequenceOrder, parentIndex, level, title, content, created);
		}
	}

}
