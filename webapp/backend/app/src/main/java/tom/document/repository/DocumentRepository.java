package tom.document.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.document.model.MintyDoc;

@Service
public interface DocumentRepository extends JpaRepository<MintyDoc, UUID> {

	boolean existsByTitle(String title);
}
