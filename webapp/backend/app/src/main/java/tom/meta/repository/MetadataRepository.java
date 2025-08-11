package tom.meta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface MetadataRepository extends JpaRepository<UserMeta, Integer> {

	Optional<UserMeta> findByUserId(int userId);
}
