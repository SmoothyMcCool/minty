package tom.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;
import tom.project.model.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findByOwnerId(UserId ownerId);

}
