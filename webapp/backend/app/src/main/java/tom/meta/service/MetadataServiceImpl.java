package tom.meta.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import tom.meta.repository.MetadataRepository;
import tom.meta.repository.UserMeta;

@Service
public class MetadataServiceImpl implements MetadataService {

	private final MetadataRepository metadataRepository;

	public MetadataServiceImpl(MetadataRepository metadataRepository) {
		this.metadataRepository = metadataRepository;
	}

	@Override
	public void newAssistant(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalAssistantsCreated(meta.getTotalAssistantsCreated() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void userLoggedIn(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setLastLogin(LocalDate.now());
			meta.setTotalLogins(meta.getTotalLogins() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void taskCreated(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalTasksCreated(meta.getTotalTasksCreated() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void workflowCreated(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalWorkflowsCreated(meta.getTotalWorkflowsCreated() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void taskExecuted(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalTaskRuns(meta.getTotalTaskRuns() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void workflowExecuted(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalWorkflowRuns(meta.getTotalWorkflowRuns() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void newConversation(int userId) {
		Optional<UserMeta> maybeMeta = metadataRepository.findByUserId(userId);
		if (maybeMeta.isPresent()) {
			UserMeta meta = maybeMeta.get();
			meta.setTotalConversations(meta.getTotalConversations() + 1);
			metadataRepository.save(meta);
		}
	}

	@Override
	public void addUser(int userId) {
		UserMeta meta = new UserMeta();
		meta.setUserId(userId);
		meta.setId(null);
		metadataRepository.save(meta);
	}

}
