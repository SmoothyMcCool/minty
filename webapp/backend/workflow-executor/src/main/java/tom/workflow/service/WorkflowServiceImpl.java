package tom.workflow.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.UserId;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.api.task.TaskLogger;
import tom.config.MintyConfiguration;
import tom.task.model.TaskRequest;
import tom.task.registry.TaskRegistryService;
import tom.user.model.ResourceSharingSelection;
import tom.user.model.UserSelection;
import tom.user.service.UserServiceInternal;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.executor.WorkflowRunner;
import tom.workflow.model.Workflow;
import tom.workflow.model.joins.UserWorkflowId;
import tom.workflow.model.joins.UserWorkflowLink;
import tom.workflow.repository.UserWorkflowLinkRepository;
import tom.workflow.repository.WorkflowRepository;
import tom.workflow.tracking.controller.model.WorkflowState;
import tom.workflow.tracking.service.WorkflowTrackingService;

@Service
public class WorkflowServiceImpl implements WorkflowService {

	private final Logger logger = LogManager.getLogger(WorkflowServiceImpl.class);

	private final WorkflowRepository workflowRepository;
	private final UserWorkflowLinkRepository linkRepository;
	private final TaskRegistryService taskRegistryService;
	private final WorkflowTrackingService workflowTrackingService;
	private final UserServiceInternal userService;
	private final AsyncTaskExecutor taskExecutor;
	private final Path workflowLoggingFolder;
	private final MintyConfiguration properties;

	public WorkflowServiceImpl(WorkflowRepository workflowRepository, UserWorkflowLinkRepository linkRepository,
			TaskRegistryService taskRegistryService, WorkflowTrackingService workflowTrackingService,
			UserServiceInternal userService, @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
			MintyConfiguration properties) {
		this.workflowRepository = workflowRepository;
		this.linkRepository = linkRepository;
		this.taskRegistryService = taskRegistryService;
		this.workflowTrackingService = workflowTrackingService;
		this.userService = userService;
		this.taskExecutor = taskExecutor;
		this.properties = properties;
		workflowLoggingFolder = properties.getConfig().fileStores().workflowLogs();
		if (workflowLoggingFolder == null) {
			throw new RuntimeException("Workflow log folder location is not defined.");
		}
	}

	@Override
	public String executeWorkflow(UserId userId, WorkflowRequest request) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(request.getId());
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + request.getId() + ". Cannot run.");
			return null;
		}

		Workflow workflow = maybeWorkflow.get().toModelWorkflow(userId);
		List<TaskRequest> steps = workflow.getSteps();

		Map<String, String> defaults = userService.getUserDefaults(userId);
		defaults.putAll(properties.getSystemDefaults());

		if (steps.size() != request.getTaskConfigurationList().size()) {
			logger.warn("Expected to get " + steps.size() + " configuration objects, but instead got "
					+ request.getTaskConfigurationList().size());
			return null;
		}

		for (int i = 0; i < steps.size(); i++) {
			Map<String, Object> config = request.getTaskConfigurationList().get(i);
			config.forEach((k, v) -> {
				if (defaults.containsKey(k)) {
					config.put(k, defaults.get(k));
				}
			});
			steps.get(i).setConfiguration(config);
		}

		if (workflow.getOutputStep() != null) {
			Map<String, Object> config = request.getOutputConfiguration();
			config.forEach((k, v) -> {
				if (defaults.containsKey(k)) {
					config.put(k, defaults.get(k));
				}
			});
			workflow.getOutputStep().setConfiguration(config);
		}

		WorkflowRunner runner = new WorkflowRunner(userId, workflow,
				TaskLogger.LogLevel.fromString(request.getLogLevel()), taskRegistryService, workflowTrackingService,
				taskExecutor, workflowLoggingFolder);

		workflowTrackingService.trackWorkflow(runner);

		try {
			runner.start();
			return runner.getWorkflowName();
		} catch (Exception e) {
			logger.warn("Failed to start workflow " + runner.getWorkflowName(), e);
		}

		return null;
	}

	@Override
	@Transactional
	public Workflow createWorkflow(UserId userId, Workflow workflow) throws NotOwnedException {
		if (workflowRepository.findByName(workflow.getName()).isPresent()) {
			throw new NotOwnedException(workflow.getName());
		}

		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setOwnerId(userId);
		dbWorkflow.setId(null);
		dbWorkflow = workflowRepository.save(dbWorkflow);

		UserWorkflowLink uwl = new UserWorkflowLink();
		UserWorkflowId uwi = new UserWorkflowId();
		uwi.setUserId(userId.getValue());
		uwi.setWorkflowId(dbWorkflow.getId());
		uwl.setId(uwi);
		uwl.setWorkflow(dbWorkflow);
		linkRepository.save(uwl);

		return dbWorkflow.toModelWorkflow(userId);
	}

	@Override
	@Transactional
	public Workflow updateWorkflow(UserId userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setOwnerId(userId);
		return workflowRepository.save(dbWorkflow).toModelWorkflow(userId);
	}

	@Override
	@Transactional
	public List<Workflow> listWorkflows(UserId userId) {
		List<UserWorkflowLink> workflows = linkRepository
				.findById_UserIdIn(List.of(userId.getValue(), ResourceSharingSelection.AllUsersId.getValue()));

		return workflows.stream().map(link -> link.getWorkflow().toModelWorkflow(userId))
				.collect(Collectors.toMap(Workflow::getId, w -> w, (a, b) -> a, LinkedHashMap::new)).values().stream()
				.toList();
	}

	@Override
	@Transactional
	public void shareWorkflow(UserId userId, ResourceSharingSelection selection) throws NotFoundException {

		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository
				.findByName(selection.getResource());

		if (maybeWorkflow.isEmpty()) {
			throw new NotFoundException(selection.getResource());
		}

		tom.workflow.repository.Workflow workflow = maybeWorkflow.get();

		if (!workflow.getOwnerId().equals(userId)) {
			throw new NotFoundException(selection.getResource());
		}

		UserWorkflowLink uwl = new UserWorkflowLink();
		uwl.setWorkflow(workflow);

		// Remove any other sharing first.
		List<UserWorkflowLink> workflows = linkRepository.findById_WorkflowId(UUID.fromString(selection.getResource()));
		workflows = workflows.stream().filter(link -> !link.getUserId().equals(userId.getValue())).toList();
		workflows.forEach(link -> {
			linkRepository.delete(link);
		});

		// Now share to those that should get it.
		if (selection.getUserSelection().isAllUsers()) {
			UserId sharingTargetUser = ResourceSharingSelection.AllUsersId;
			UserWorkflowId uwi = new UserWorkflowId();
			uwi.setWorkflowId(workflow.getId());
			uwi.setUserId(sharingTargetUser.getValue());
			uwl.setId(uwi);
			linkRepository.save(uwl);

		} else {

			for (String username : selection.getUserSelection().getSelectedUsers()) {
				try {
					UserId sharingTargetUser = userService.getUserFromName(username).orElseThrow().getId();
					UserWorkflowId uwi = new UserWorkflowId();
					uwi.setWorkflowId(workflow.getId());
					uwi.setUserId(sharingTargetUser.getValue());
					uwl.setId(uwi);
					linkRepository.save(uwl);
				} catch (Exception e) {
					// oh well. sharing failed to that user.
				}
			}
		}
	}

	@Override
	@Transactional
	public UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findByName(name);

		if (maybeWorkflow.isEmpty()) {
			throw new NotFoundException(name);
		}

		tom.workflow.repository.Workflow workflow = maybeWorkflow.get();

		if (!workflow.getOwnerId().equals(userId)) {
			throw new NotOwnedException(workflow.getName());
		}

		List<UserWorkflowLink> sharedWith = linkRepository.findById_WorkflowId(workflow.getId());

		UserSelection selection = new UserSelection();
		selection.setAllUsers(false);
		selection.setSelectedUsers(new ArrayList<>());

		for (UserWorkflowLink share : sharedWith) {
			try {
				if (share.getUserId().equals(ResourceSharingSelection.AllUsersId.getValue())) {
					selection.setAllUsers(true);
					selection.setSelectedUsers(null);
					return selection;
				}
				selection.getSelectedUsers()
						.add(userService.getUserFromId(new UserId(share.getUserId())).orElseThrow().getName());
			} catch (Exception e) {
				// Just ignore. Bad name in given list.
			}
		}

		return selection;
	}

	@Override
	@Transactional
	public Workflow getWorkflow(UserId userId, UUID workflowId) {
		Optional<UserWorkflowLink> maybeWorkflow = linkRepository.findById_WorkflowIdAndId_UserIdIn(workflowId,
				List.of(userId.getValue(), ResourceSharingSelection.AllUsersId.getValue()));

		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + workflowId);
			return null;
		}

		return maybeWorkflow.get().getWorkflow().toModelWorkflow(userId);
	}

	@Override
	@Transactional
	public void deleteWorkflow(UserId userId, UUID workflowId) {
		workflowRepository.deleteById(workflowId);
	}

	@Override
	public void cancelWorkflow(UserId userId, String name) throws NotOwnedException {
		if (!isRunningWorkflowOwned(userId, name)) {
			throw new NotOwnedException("User does not own this workflow.");
		}
		workflowTrackingService.cancelWorkflow(userId, name);
	}

	@Override
	public boolean isAllowedToExecute(UUID workflowId, UserId userId) {
		return this.getWorkflow(userId, workflowId) != null;
	}

	@Override
	public boolean workflowExists(UUID workflowId) {

		List<UserWorkflowLink> workflow = linkRepository.findById_WorkflowId(workflowId);

		return !workflow.isEmpty();
	}

	@Override
	@Transactional
	public boolean isWorkflowOwned(UUID workflowId, UserId userId) {

		Optional<UserWorkflowLink> workflow = linkRepository.findById_WorkflowIdAndId_UserId(workflowId,
				userId.getValue());

		if (workflow.isEmpty()) {
			return false;
		}

		return workflow.get().getWorkflow().getOwnerId().equals(userId);
	}

	@Override
	public boolean isRunningWorkflowOwned(UserId userId, String name) {
		List<WorkflowState> runningWorkflows = workflowTrackingService.getWorkflowList(userId);
		boolean found = false;
		for (WorkflowState workflow : runningWorkflows) {
			if (workflow.name().equals(name)) {
				found = true;
				break;
			}
		}
		return found;
	}

}
