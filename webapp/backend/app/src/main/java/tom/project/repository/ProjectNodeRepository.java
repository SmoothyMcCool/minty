package tom.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tom.api.UserId;
import tom.api.model.project.NodeType;
import tom.project.model.ProjectNodeEntity;

public interface ProjectNodeRepository extends JpaRepository<ProjectNodeEntity, UUID> {

	Optional<ProjectNodeEntity> findByProjectIdAndPathAndOwnerId(UUID projectId, String path, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndOwnerId(UUID projectId, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndParentIdAndOwnerId(UUID projectId, UUID parentId, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndOwnerIdOrderByPathAsc(UUID projectId, UserId ownerId);

	List<ProjectNodeEntity> findByProjectIdAndPathStartingWithAndOwnerId(UUID projectId, String pathPrefix,
			UserId ownerId);

	@Query("""
			SELECT n
			  FROM ProjectNodeEntity n
			 WHERE n.projectId = :projectId
			   AND n.ownerId = :ownerId
			   AND (n.name LIKE :filter OR n.path LIKE :filter)
			""")
	List<ProjectNodeEntity> searchByFilter(@Param("projectId") UUID projectId, @Param("ownerId") UserId ownerId,
			@Param("filter") String filter);

	@Query("""
			SELECT n
			  FROM ProjectNodeEntity n
			 WHERE n.projectId = :projectId
			   AND n.ownerId = :ownerId
			   AND (:pathPrefix = '/' OR n.path LIKE :pathPattern)
			   AND (:namePattern IS NULL OR n.name LIKE :namePattern)
			   AND (:type IS NULL OR n.type = :type)
			 ORDER BY n.path ASC
			""")
	List<ProjectNodeEntity> findNodes(@Param("projectId") UUID projectId, @Param("ownerId") UserId ownerId,
			@Param("pathPrefix") String pathPrefix, @Param("pathPattern") String pathPattern,
			@Param("namePattern") String namePattern, @Param("type") NodeType type);
}
