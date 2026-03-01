package tom.tag.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.tag.model.MintyTag;

public interface TagRepository extends JpaRepository<MintyTag, UUID> {
}
