package tom.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.api.UserId;
import tom.document.model.MintyDoc;

@Service
public interface DocumentRepository extends JpaRepository<MintyDoc, UUID> {

	boolean existsByTitle(String title);

	List<MintyDoc> findAllByOwnerId(UserId ownerId);
}
