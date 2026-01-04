package tom.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.api.UserId;

@Service
public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findByOwnerId(UserId ownerId);

	List<Project> findAllByOwnerId(UserId value);
}
