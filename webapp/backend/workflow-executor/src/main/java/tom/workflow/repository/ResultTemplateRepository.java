package tom.workflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import tom.workflow.model.ResultTemplate;

@Service
public interface ResultTemplateRepository extends JpaRepository<ResultTemplate, UUID> {

	ResultTemplate findByName(String templateName);

	@Query("select rt.name from ResultTemplate rt")
	List<String> findAllTemplateNames();

}
