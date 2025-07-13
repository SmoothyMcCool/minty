package tom.user.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface UserRepository extends CrudRepository<EncryptedUser, Integer> {

	EncryptedUser findByAccount(String account);

}
