package tom.meta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;

public interface MetadataRepository extends JpaRepository<UserMeta, Integer> {

	Optional<UserMeta> findByUserId(UserId userId);
}
