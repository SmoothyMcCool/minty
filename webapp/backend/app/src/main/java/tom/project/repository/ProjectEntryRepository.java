package tom.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface ProjectEntryRepository extends JpaRepository<ProjectEntry, UUID> {

	List<ProjectEntryInfoProjection> findAllProjectedByProjectId(UUID projectId);

	List<ProjectEntryInfoProjection> findAllProjectedByName(String name);

}
