package tom.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface UserRepository extends JpaRepository<EncryptedUser, Integer> {

	EncryptedUser findByAccount(String account);

}
