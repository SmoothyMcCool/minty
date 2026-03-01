package tom.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;
import tom.document.model.MintyDoc;

public interface DocumentRepository extends JpaRepository<MintyDoc, UUID> {

	boolean existsByTitle(String title);

	List<MintyDoc> findAllByOwnerId(UserId ownerId);
}
