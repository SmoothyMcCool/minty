package tom.config;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.meta.model.AiRequest;
import tom.meta.model.RequestStatus;
import tom.meta.repository.AiRequestRepository;
import tom.meta.repository.RequestStatusRepository;
import tom.meta.service.AiRequestMetricsService;

@Service
public class StartupService {

	private static final Logger logger = LogManager.getLogger(StartupService.class);

	private final AiRequestMetricsService aiRequestMetricsService;
	private final AiRequestRepository requestRepository;
	private final RequestStatusRepository statusRepository;

	public StartupService(AiRequestMetricsService aiRequestMetricsService, AiRequestRepository requestRepository,
			RequestStatusRepository statusRepository) {
		this.aiRequestMetricsService = aiRequestMetricsService;
		this.requestRepository = requestRepository;
		this.statusRepository = statusRepository;
	}

	@PostConstruct
	public void failInflightRequests() {
		List<AiRequest> inflight = requestRepository
				.findByStatusIn(List.of(statusRepository.getByStatus(RequestStatus.QUEUED),
						statusRepository.getByStatus(RequestStatus.PROCESSING)));

		if (inflight.isEmpty()) {
			logger.info("No inflight requests found on startup");
			return;
		}

		logger.warn("Found {} inflight request(s) on startup, marking as failed", inflight.size());
		inflight.forEach(r -> {
			logger.warn("Failing request {} (status={})", r.getId(), r.getStatusEnum());
			aiRequestMetricsService.markFailed(r.getId(), "server restart");
		});
		logger.info("Startup cleanup complete");
	}
}
