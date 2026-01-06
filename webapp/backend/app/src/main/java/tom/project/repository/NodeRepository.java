package tom.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public interface NodeRepository extends JpaRepository<Node, UUID> {

	List<NodeInfoProjection> findAllProjectedByProjectId(UUID projectId);

	List<NodeInfoProjection> findAllProjectedByName(String name);

	List<Node> findAllByParentId(UUID parentId);

	@Modifying
	@Transactional
	@Query("UPDATE Node n SET n.parentId = :parentId, n.name = :name WHERE n.id = :nodeId")
	int updateNodeInfo(@Param("nodeId") UUID nodeId, @Param("parentId") UUID parentId, @Param("name") String name);
}
