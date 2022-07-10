package net.luversof.user.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;

public interface UserRepository extends CrudRepository<User, Long> {
	
	Optional<User> findByUserId(String userId);

	Optional<User> findByUserName(String userName);
	
	Optional<User> findByExternalIdAndUserType(String externalId, UserType userType);
	
	long deleteByUserId(String userId);
	
}
