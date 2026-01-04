package tom.project.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.NotFoundException;
import tom.NotOwnedException;
import tom.api.ProjectEntryId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.Project;
import tom.api.model.project.ProjectEntry;
import tom.api.model.project.ProjectEntryInfo;
import tom.api.services.ProjectService;
import tom.config.MintyConfiguration;
import tom.project.repository.ProjectEntryInfoProjection;
import tom.project.repository.ProjectEntryRepository;
import tom.project.repository.ProjectRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectEntryRepository projectEntryRepository;

	public ProjectServiceImpl(ProjectRepository projectRepository, ProjectEntryRepository projectEntryRepository,
			MintyConfiguration properties) {
		this.projectRepository = projectRepository;
		this.projectEntryRepository = projectEntryRepository;
	}

	@Override
	@Transactional
	public Project createProject(UserId userId, String name) {
		tom.project.repository.Project project = new tom.project.repository.Project();
		project.setName(name);
		project.setOwnerId(userId);
		project = projectRepository.save(project);
		return project.toModel();
	}

	@Override
	@Transactional
	public void deleteProject(UserId userId, ProjectId projectId) {
		validateProjectAccess(userId, projectId);

		projectRepository.deleteById(projectId.value());
	}

	@Override
	public List<Project> listProjects(UserId id) {
		return projectRepository.findAllByOwnerId(id).stream().map(project -> project.toModel()).toList();
	}

	@Override
	public Project getProject(UserId userId, ProjectId projectId) {
		Optional<tom.project.repository.Project> maybeProject = projectRepository.findById(projectId.getValue());

		if (maybeProject.isPresent() && maybeProject.get().getOwnerId().equals(userId)) {
			return maybeProject.get().toModel();
		}
		throw new NotFoundException("Project does not exist");
	}

	@Override
	public List<ProjectEntryInfo> listProjectEntries(UserId userId, ProjectId projectId) throws IOException {
		validateProjectAccess(userId, projectId);

		List<ProjectEntryInfoProjection> entries = projectEntryRepository
				.findAllProjectedByProjectId(projectId.value());
		return entries.stream()
				.map(entry -> new ProjectEntryInfo(new ProjectEntryId(entry.getId()), entry.getType(), entry.getName()))
				.toList();
	}

	@Override
	public ProjectEntry getProjectEntry(UserId userId, ProjectId projectId, ProjectEntryId entryId) {
		tom.project.repository.ProjectEntry entry = getProjectEntryInternal(userId, projectId, entryId);
		return new ProjectEntry(new ProjectEntryInfo(entryId, entry.getType(), entry.getName()), entry.getData());
	}

	private tom.project.repository.ProjectEntry getProjectEntryInternal(UserId userId, ProjectId projectId,
			ProjectEntryId entryId) {
		validateProjectAccess(userId, projectId);

		Optional<tom.project.repository.ProjectEntry> maybeEntry = projectEntryRepository.findById(entryId.value());

		if (maybeEntry.isEmpty()) {
			throw new NotFoundException("File " + entryId.value() + " does not exist");
		}

		tom.project.repository.ProjectEntry entry = maybeEntry.get();

		if (!entry.getProjectId().equals(projectId.value())) {
			throw new NotOwnedException("Requested ProjectEntry not part of this project.");
		}

		return entry;
	}

	@Override
	@Transactional
	public void createOrUpdateProjectEntry(UserId userId, ProjectId projectId, ProjectEntry projectEntry) {
		validateProjectAccess(userId, projectId);

		if (entryExists(projectEntry.info())) {
			// This will throw if we are not allowed to access the entry. We already know it
			// exists.
			tom.project.repository.ProjectEntry entry = getProjectEntryInternal(userId, projectId,
					projectEntry.info().getId());
			entry.setName(projectEntry.info().getName());
			if (projectEntry.info().getParent() != null) {
				entry.setParentId(projectEntry.info().getParent().value());
			} else {
				entry.setParentId(null);
			}
			entry.setType(projectEntry.info().getType());
			entry.setData(projectEntry.data());
			projectEntryRepository.save(entry);

		} else {
			tom.project.repository.ProjectEntry entry = new tom.project.repository.ProjectEntry();
			entry.setName(projectEntry.info().getName());
			if (projectEntry.info().getParent() != null) {
				entry.setParentId(projectEntry.info().getParent().value());
			} else {
				entry.setParentId(null);
			}
			entry.setProjectId(projectId.value());
			entry.setType(projectEntry.info().getType());
			entry.setData(projectEntry.data());
			projectEntryRepository.save(entry);

		}
	}

	@Override
	@Transactional
	public void deleteProjectEntry(UserId userId, ProjectId projectId, ProjectEntryId entryId) {
		validateProjectAccess(userId, projectId);

		ProjectEntry entry = getProjectEntry(userId, projectId, entryId);

		if (entry == null) {
			return;
		}

		projectEntryRepository.deleteById(entry.info().getId().value());
	}

	private void validateProjectAccess(UserId userId, ProjectId projectId) {
		Optional<tom.project.repository.Project> project = projectRepository.findById(projectId.getValue());

		if (project.isEmpty()) {
			throw new NotFoundException("Project " + projectId.toString() + " does not exist");
		}

		if (!project.get().getOwnerId().equals(userId)) {
			throw new NotOwnedException("Project " + projectId.toString() + " isn't owned by this user");
		}
	}

	private boolean entryExists(ProjectEntryInfo entryInfo) {
		if (entryInfo.getId() == null) {
			return false;
		}
		return projectEntryRepository.existsById(entryInfo.getId().value());
	}

	@Override
	public ProjectEntry getProjectEntryByName(UserId userId, ProjectId projectId, String fileName) {
		validateProjectAccess(userId, projectId);

		List<ProjectEntryInfoProjection> entries = projectEntryRepository.findAllProjectedByName(fileName);

		for (ProjectEntryInfoProjection entry : entries) {
			if (entry.getProjectId().equals(projectId.value())) {
				return getProjectEntry(userId, projectId, new ProjectEntryId(entry.getId()));
			}
		}

		return null;
	}

}
