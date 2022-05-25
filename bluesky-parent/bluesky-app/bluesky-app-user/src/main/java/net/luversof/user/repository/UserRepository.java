package net.luversof.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;

@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {
	
	Optional<User> findByUserId(String userId);

	Optional<User> findByUserName(String userName);
	
	Optional<User> findByExternalIdAndUserType(String externalId, UserType userType);
	
	List<User> findByUserIdIn(Collection<String> userIds);
}
