package tom.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<EncryptedUser, UUID> {

	EncryptedUser findByAccount(String account);

}
