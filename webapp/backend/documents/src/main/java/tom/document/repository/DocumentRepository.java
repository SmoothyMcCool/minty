package tom.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.document.model.MintyDoc;

public interface DocumentRepository extends JpaRepository<MintyDoc, UUID> {

	boolean existsByTitle(String title);

	List<MintyDoc> findAllByOwnerIdAndProjectId(UserId userId, ProjectId projectId);

	Optional<MintyDoc> findByOwnerIdAndProjectIdAndTitleIgnoreCase(UserId ownerId, ProjectId projectId, String title);

	Optional<MintyDoc> findByTitleAndProjectId(String title, ProjectId projectId);
}
