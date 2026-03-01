package tom.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

	List<ProjectEntity> findByOwnerId(UserId ownerId);

}
