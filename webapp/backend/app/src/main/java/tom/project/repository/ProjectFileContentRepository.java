package tom.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tom.project.model.ProjectFileContent;
import tom.project.service.FileSearchRow;

public interface ProjectFileContentRepository extends JpaRepository<ProjectFileContent, UUID> {

	Optional<ProjectFileContent> findTopByNodeIdOrderByVersionDesc(UUID nodeId);

	List<ProjectFileContent> findByNodeIdOrderByVersionDesc(UUID nodeId);

	@Query(value = """
			SELECT
				n.path AS path,
				c.content AS content
			FROM ProjectFileContent c
			JOIN ProjectNode n
			  ON n.id = c.nodeId
			 AND c.version = n.version
			WHERE n.projectId = :projectId
			  AND n.ownerId = :ownerId
			  AND n.type = 'File'
			  AND LOWER(c.content) LIKE LOWER(:pattern)
			  AND n.path LIKE :pathPattern
			ORDER BY n.path
			""", nativeQuery = true)
	List<FileSearchRow> searchCurrentFileContents(@Param("projectId") UUID projectId, @Param("ownerId") UUID ownerId,
			@Param("pattern") String pattern, @Param("pathPattern") String pathPattern);
}
