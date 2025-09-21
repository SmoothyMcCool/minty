package tom.tag.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.tag.model.MintyTag;

@Service
public interface TagRepository extends JpaRepository<MintyTag, UUID> {
}
