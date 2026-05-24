package tom.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.project.model.ProjectFileContent;

public interface ProjectFileContentRepository extends JpaRepository<ProjectFileContent, UUID> {

	Optional<ProjectFileContent> findTopByNodeIdOrderByVersionDesc(UUID nodeId);

	List<ProjectFileContent> findByNodeIdOrderByVersionDesc(UUID nodeId);
}
