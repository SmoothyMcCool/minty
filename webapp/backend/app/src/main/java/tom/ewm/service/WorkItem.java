package tom.ewm.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tom.api.services.ewm.ResourceRef;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkItem {
	public int id;
	public String title;
	public Instant created;
	public Instant modified;
	public ResourceRef state;
	public ResourceRef type;
	public ResourceRef ownedBy;
	public ResourceRef creator;
	public ResourceRef priority;
	public ResourceRef severity;
	public ResourceRef filedAgainst;
	public ResourceRef plannedFor;
	public List<String> tags;
	public Map<String, Object> custom; // custom attributes captured by id

	@Override
	public String toString() {
		return "WorkItem{" + "id=" + id + ", title='" + title + '\'' + ", state=" + state + ", type=" + type
				+ ", ownedBy=" + ownedBy + ", priority=" + priority + ", severity=" + severity + ", filedAgainst="
				+ filedAgainst + ", plannedFor=" + plannedFor + ", tags=" + tags + ", custom=" + custom + '}';
	}

	public tom.api.services.ewm.WorkItem toApiWorkItem() {
		return new tom.api.services.ewm.WorkItem(id, title, created, modified, state, type, ownedBy, creator, priority,
				severity, filedAgainst, plannedFor, tags, custom);
	}
}
