package tom.api.model.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import tom.api.DocumentId;
import tom.api.ProjectId;
import tom.api.UserId;

public record Document(DocumentId id, String title, UserId ownerId, ProjectId projectId, boolean vectorized,
		Instant created, Instant updated, String summary, List<DocumentSection> sections) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().id(id).title(title).ownerId(ownerId).projectId(projectId).vectorized(vectorized)
				.created(created).updated(updated).summary(summary).sections(sections);
	}

	public static class Builder {

		// Builder state
		private DocumentId id;
		private String title;
		private UserId ownerId;
		private ProjectId projectId;
		private boolean vectorized;
		private Instant created;
		private Instant updated;
		private String summary;
		private List<DocumentSection> sections = new ArrayList<>();

		private Builder() {
		}

		public static Builder builder() {
			return new Builder();
		}

		public Builder id(DocumentId id) {
			this.id = id;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder ownerId(UserId ownerId) {
			this.ownerId = ownerId;
			return this;
		}

		public Builder projectId(ProjectId projectId) {
			this.projectId = projectId;
			return this;
		}

		public Builder vectorized(boolean vectorized) {
			this.vectorized = vectorized;
			return this;
		}

		public Builder created(Instant created) {
			this.created = created;
			return this;
		}

		public Builder updated(Instant updated) {
			this.updated = updated;
			return this;
		}

		public Builder summary(String summary) {
			this.summary = summary;
			return this;
		}

		public Builder sections(List<DocumentSection> sections) {
			this.sections = new ArrayList<>(sections);
			return this;
		}

		public Builder addSection(DocumentSection section) {
			this.sections.add(section);
			return this;
		}

		public Document build() {
			// Simple null checks – you can replace these with more elaborate validation
			if (title == null) {
				throw new IllegalStateException("title must not be null");
			}
			if (ownerId == null) {
				throw new IllegalStateException("ownerId must not be null");
			}
			if (projectId == null) {
				throw new IllegalStateException("projectId must not be null");
			}
			if (created == null) {
				throw new IllegalStateException("created must not be null");
			}
			if (updated == null) {
				throw new IllegalStateException("updated must not be null");
			}

			return new Document(id, title, ownerId, projectId, vectorized, created, updated, summary,
					List.copyOf(sections));
		}
	}

}
