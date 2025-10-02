package tom.meta.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import tom.api.UserId;
import tom.meta.repository.MetadataRepository;
import tom.meta.repository.UserMeta;

@Service
public class MetadataServiceImpl implements MetadataService {

	private final MetadataRepository metadataRepository;

	public MetadataServiceImpl(MetadataRepository metadataRepository) {
		this.metadataRepository = metadataRepository;
	}

	@Override
	public void newAssistant(UserId userId) {
		UserMeta meta = getUser(userId);
		meta.setTotalAssistantsCreated(meta.getTotalAssistantsCreated() + 1);
		metadataRepository.save(meta);
	}

	@Override
	public void userLoggedIn(UserId userId) {
		UserMeta meta = getUser(userId);
		meta.setLastLogin(LocalDate.now());
		meta.setTotalLogins(meta.getTotalLogins() + 1);
		metadataRepository.save(meta);
	}

	@Override
	public void workflowCreated(UserId userId) {
		UserMeta meta = getUser(userId);
		meta.setTotalWorkflowsCreated(meta.getTotalWorkflowsCreated() + 1);
		metadataRepository.save(meta);
	}

	@Override
	public void workflowExecuted(UserId userId) {
		UserMeta meta = getUser(userId);
		meta.setTotalWorkflowRuns(meta.getTotalWorkflowRuns() + 1);
		metadataRepository.save(meta);
	}

	@Override
	public void newConversation(UserId userId) {
		UserMeta meta = getUser(userId);
		meta.setTotalConversations(meta.getTotalConversations() + 1);
		metadataRepository.save(meta);
	}

	@Override
	public void addUser(UserId userId) {
		UserMeta meta = new UserMeta();
		meta.setUserId(userId);
		meta.setId(null);
		metadataRepository.save(meta);
	}

	private UserMeta getUser(UserId userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (!maybeMeta.isPresent()) {
			addUser(userId);
			maybeMeta = metadataRepository.findByUserId(userId);
			if (!maybeMeta.isPresent()) {
				throw new RuntimeException("Failed to create metadata for user!");
			}
		}
		return maybeMeta.get();
	}

}
