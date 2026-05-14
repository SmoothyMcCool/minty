package tom.meta.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.repository.converter.ConversationIdConverter;
import tom.repository.converter.UserIdConverter;

/**
 * Maps to the {@code requests} table.
 *
 * <p>
 * Timestamps use {@link Instant} (UTC) and are stored as {@code DATETIME(6)}
 * (microsecond precision) in MariaDB via the {@code columnDefinition} hints.
 *
 * <p>
 * The {@code status} column is backed by a FK to {@code request_status}, so we
 * map it as a {@link ManyToOne} to {@link RequestStatusEntity} rather than a
 * plain string - this keeps the FK constraint intact at the DB level while
 * still giving us type-safe access in code via {@link RequestStatus}.
 *
 * <pre>
 * Lifecycle order:
 *   created_at → queued_at → dequeued_at → first_token_at → completed_at
 * </pre>
 */
@Entity
@Table(name = "LlmRequests", indexes = { @Index(name = "idx_requests_userId", columnList = "userId"),
		@Index(name = "idx_requests_sessionId", columnList = "sessionId"),
		@Index(name = "idx_requests_status", columnList = "status"),
		@Index(name = "idx_requests_createdAt", columnList = "createdat") })
public class AiRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Convert(converter = UserIdConverter.class)
	private UserId userId;

	@Convert(converter = ConversationIdConverter.class)
	private ConversationId conversationId;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "status", nullable = false, foreignKey = @ForeignKey(name = "fk_requests_status"))
	private RequestStatusEntity status;

	@CreationTimestamp
	@Column(name = "createdat", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
	private Instant createdAt;

	/** Set when the request enters the processing queue. */
	@Column(name = "queuedat", columnDefinition = "DATETIME(6)")
	private Instant queuedAt;

	/** Set when a worker picks the request up and starts the upstream API call. */
	@Column(name = "dequeuedat", columnDefinition = "DATETIME(6)")
	private Instant dequeuedAt;

	/** Set when the first streamed token is received from the upstream model. */
	@Column(name = "firstTokenAt", columnDefinition = "DATETIME(6)")
	private Instant firstTokenAt;

	/** Set when the full response has been streamed and the request is done. */
	@Column(name = "completedAt", columnDefinition = "DATETIME(6)")
	private Instant completedAt;

	@Column(name = "error", columnDefinition = "TEXT")
	private String error;

	@OneToOne(mappedBy = "request", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private RequestMetrics metrics;

	protected AiRequest() {
	}

	private AiRequest(UserId userId, ConversationId conversationId, RequestStatusEntity status, Instant createdAt,
			Instant queuedAt) {
		this.id = null;
		this.userId = userId;
		this.conversationId = conversationId;
		this.status = status;
		this.createdAt = createdAt;
		this.queuedAt = queuedAt;
	}

	/**
	 * Creates a new AiRequest with a generated UUID, status QUEUED, and both
	 * created_at / queued_at set to now.
	 */
	public static AiRequest create(UserId userId, ConversationId conversationId, RequestStatusEntity queuedStatus) {
		Instant now = Instant.now();
		return new AiRequest(userId, conversationId, queuedStatus, now, now);
	}

	public LlmRequestId getId() {
		return new LlmRequestId(id);
	}

	public void setId(LlmRequestId id) {
		this.id = id == null ? null : id.value();
	}

	public RequestStatusEntity getStatus() {
		return status;
	}

	public void setStatus(RequestStatusEntity status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getQueuedAt() {
		return queuedAt;
	}

	public void setQueuedAt(Instant queuedAt) {
		this.queuedAt = queuedAt;
	}

	public Instant getDequeuedAt() {
		return dequeuedAt;
	}

	public void setDequeuedAt(Instant dequeuedAt) {
		this.dequeuedAt = dequeuedAt;
	}

	public Instant getFirstTokenAt() {
		return firstTokenAt;
	}

	public void setFirstTokenAt(Instant firstTokenAt) {
		this.firstTokenAt = firstTokenAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public RequestMetrics getMetrics() {
		return metrics;
	}

	public void setMetrics(RequestMetrics metrics) {
		this.metrics = metrics;
	}

	/** Shortcut to read the status as the application-layer enum. */
	public RequestStatus getStatusEnum() {
		return RequestStatus.fromDbValue(status.getStatus());
	}

	/**
	 * Produces an immutable {@link RequestSummary} from this entity's current
	 * state. Duration fields are computed here so that no layer above needs to
	 * understand the timestamp arithmetic.
	 */
	public RequestSummary toSummary() {
		return new RequestSummary(new LlmRequestId(id), userId, conversationId, getStatusEnum(), createdAt, queuedAt,
				dequeuedAt, firstTokenAt, completedAt, error, durationMs(queuedAt, dequeuedAt),
				durationMs(dequeuedAt, firstTokenAt), durationMs(createdAt, completedAt));
	}

	private static Double durationMs(Instant start, Instant end) {
		if (start == null || end == null)
			return null;
		// Use nanosecond precision where available, fall back to millis for the epoch
		// part
		return (end.toEpochMilli() - start.toEpochMilli()) + (end.getNano() - start.getNano()) / 1_000_000.0;
	}
}