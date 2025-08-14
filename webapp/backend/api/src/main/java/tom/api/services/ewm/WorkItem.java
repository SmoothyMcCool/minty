package tom.api.services.ewm;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkItem(Integer id, String title, Instant created, Instant modified, ResourceRef state, ResourceRef type,
		ResourceRef ownedBy, ResourceRef creator, ResourceRef priority, ResourceRef severity, ResourceRef filedAgainst,
		ResourceRef plannedFor, List<String> tags, Map<String, Object> custom) {

	public WorkItem {
		// Defensive copies for mutable collections
		tags = tags != null ? List.copyOf(tags) : null;
		custom = custom != null ? Map.copyOf(custom) : null;
	}

	// Copy constructor
	public WorkItem(WorkItem other) {
		this(other.id, other.title, other.created, other.modified,
				other.state != null ? new ResourceRef(other.state) : null,
				other.type != null ? new ResourceRef(other.type) : null,
				other.ownedBy != null ? new ResourceRef(other.ownedBy) : null,
				other.creator != null ? new ResourceRef(other.creator) : null,
				other.priority != null ? new ResourceRef(other.priority) : null,
				other.severity != null ? new ResourceRef(other.severity) : null,
				other.filedAgainst != null ? new ResourceRef(other.filedAgainst) : null,
				other.plannedFor != null ? new ResourceRef(other.plannedFor) : null, other.tags, other.custom);
	}

	@Override
	public String toString() {
		return "WorkItem{" + "id=" + id + ", title='" + title + '\'' + ", state=" + state + ", type=" + type
				+ ", ownedBy=" + ownedBy + ", priority=" + priority + ", severity=" + severity + ", filedAgainst="
				+ filedAgainst + ", plannedFor=" + plannedFor + ", tags=" + tags + ", custom=" + custom + '}';
	}

}
