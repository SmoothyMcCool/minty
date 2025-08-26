package tom.meta.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface MetadataRepository extends JpaRepository<UserMeta, UUID> {

	Optional<UserMeta> findByUserId(UUID userId);
}
