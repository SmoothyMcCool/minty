package tom.meta.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Maps to the {@code request_metrics} table.
 *
 * <p>
 * This table is normally populated by the MariaDB trigger
 * {@code trg_compute_metrics} when a request transitions to {@code completed}.
 * However the entity is fully writable from JPA as well, e.g. to store token
 * counts after the trigger has already fired.
 *
 * <p>
 * All durations are stored in <em>microseconds</em> (divide by 1 000 for ms).
 * They are typed as {@code Long} (nullable) rather than primitive {@code long}
 * so that partially-recorded rows (e.g. a failed request missing
 * first_token_at) can be represented without a sentinel value.
 *
 * <pre>
 * queue_wait_us  = dequeued_at    - queued_at
 * ttft_us        = first_token_at - dequeued_at
 * total_time_us  = completed_at   - created_at
 * </pre>
 */
@Entity
@Table(name = "LlmRequestMetrics")
public class RequestMetrics {

	// ----------------------------------------------------------------
	// Primary key - same value as the owning AiRequest.id
	// ----------------------------------------------------------------

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "id", referencedColumnName = "id")
	private AiRequest request;

	/** Time the request spent waiting in the queue (dequeued_at − queued_at). */
	private Long queueWaitUs;

	/**
	 * Time to first token from the upstream model (first_token_at − dequeued_at).
	 */
	private Long ttftUs;

	/**
	 * Wall-clock time from request creation to completion (completed_at −
	 * created_at).
	 */
	private Long totalTimeUs;

	private Integer promptTokens;

	private Integer completionTokens;

	protected RequestMetrics() {
	}

	public RequestMetrics(AiRequest request) {
		this.request = request;
		this.id = request.getId().value();
	}

	public LlmRequestId getRequestId() {
		return id != null ? new LlmRequestId(id) : null;
	}

	public void setRequestId(LlmRequestId id) {
		this.id = id.value();
	}

	public AiRequest getRequest() {
		return request;
	}

	public void setRequest(AiRequest request) {
		this.request = request;
	}

	public Long getQueueWaitUs() {
		return queueWaitUs;
	}

	public void setQueueWaitUs(Long queueWaitUs) {
		this.queueWaitUs = queueWaitUs;
	}

	public Long getTtftUs() {
		return ttftUs;
	}

	public void setTtftUs(Long ttftUs) {
		this.ttftUs = ttftUs;
	}

	public Long getTotalTimeUs() {
		return totalTimeUs;
	}

	public void setTotalTimeUs(Long totalTimeUs) {
		this.totalTimeUs = totalTimeUs;
	}

	public Integer getPromptTokens() {
		return promptTokens;
	}

	public void setPromptTokens(Integer promptTokens) {
		this.promptTokens = promptTokens;
	}

	public Integer getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(Integer completionTokens) {
		this.completionTokens = completionTokens;
	}

	@Transient
	public Double getQueueWaitMs() {
		return queueWaitUs == null ? null : queueWaitUs / 1_000.0;
	}

	@Transient
	public Double getTtftMs() {
		return ttftUs == null ? null : ttftUs / 1_000.0;
	}

	@Transient
	public Double getTotalTimeMs() {
		return totalTimeUs == null ? null : totalTimeUs / 1_000.0;
	}
}