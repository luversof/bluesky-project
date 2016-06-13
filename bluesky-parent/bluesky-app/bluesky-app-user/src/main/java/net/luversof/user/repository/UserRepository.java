package net.luversof.user.repository;

import static net.luversof.user.UserConstants.USER_TRANSACTIONMANAGER;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;

@Transactional(USER_TRANSACTIONMANAGER)
public interface UserRepository extends JpaRepository<User, Long> {
	
	@Transactional(value = USER_TRANSACTIONMANAGER, readOnly = true)
	User findByUsername(String username);
	
	@Transactional(value = USER_TRANSACTIONMANAGER, readOnly = true)
	User findByExternalIdAndUserType(String externalId, UserType userType);
}
