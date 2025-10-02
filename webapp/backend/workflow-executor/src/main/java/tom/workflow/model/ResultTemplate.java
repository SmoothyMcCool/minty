package tom.workflow.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.UserId;

@Entity
public class ResultTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UserId ownerId;
	private String name;
	private String content;

	public ResultTemplate() {

	}

	public ResultTemplate(UUID id, UserId ownerId, String name, String content) {
		this.id = id;
		this.ownerId = ownerId;
		this.name = name;
		this.content = content;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
