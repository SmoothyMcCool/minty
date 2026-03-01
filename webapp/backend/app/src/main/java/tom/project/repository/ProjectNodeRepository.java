package tom.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;
import tom.api.model.project.NodeContent;

public interface ProjectNodeRepository extends JpaRepository<ProjectNodeEntity, UUID> {

	Optional<ProjectNodeEntity> findByProjectIdAndPathAndOwnerId(UUID projectId, String path, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndParentIdAndOwnerId(UUID projectId, UUID parentId, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndOwnerIdOrderByPathAsc(UUID projectId, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndPathStartingWithAndOwnerId(UUID projectId, String pathPrefix,
			UserId ownerId);

	NodeContent findByProjectIdAndOwnerId(UUID projectId, UserId ownerId);
}
