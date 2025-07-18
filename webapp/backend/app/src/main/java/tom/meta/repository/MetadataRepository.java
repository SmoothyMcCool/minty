package tom.meta.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface MetadataRepository extends CrudRepository<UserMeta, Integer> {

	Optional<UserMeta> findByUserId(int userId);
}
