package tom.meta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps to the {@code request_status} lookup / enum table.
 *
 * <pre>
 * CREATE TABLE request_status (
 *   status VARCHAR(20) NOT NULL PRIMARY KEY
 * );
 * </pre>
 */
@Entity
@Table(name = "LlmRequestStatus")
public class RequestStatusEntity {

	@Id
	@Column(name = "status", nullable = false, length = 20)
	private String status;

	protected RequestStatusEntity() {
	}

	public RequestStatusEntity(String status) {
		this.status = status;
	}

	public static RequestStatusEntity of(RequestStatus status) {
		return new RequestStatusEntity(status.toDbValue());
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return status;
	}
}