package tom.skill.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import tom.api.UserId;

@Entity
@Table(name = "Skills")
public class Skill {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@Lob
	@Column(name = "file")
	private byte[] file;

	@Column(name = "ownerId", nullable = false)
	private UserId ownerId;

	// Constructors
	public Skill() {
	}

	public Skill(UUID id, String name, byte[] file, UserId ownerId) {
		this.id = id;
		this.name = name;
		this.file = file;
		this.ownerId = ownerId;
	}

	// Getters & Setters
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

}
