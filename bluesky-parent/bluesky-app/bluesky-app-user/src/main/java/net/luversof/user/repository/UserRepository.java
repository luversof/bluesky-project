package net.luversof.user.repository;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	User findByUsername(String username);
	User findByExternalIdAndUserType(String externalId, UserType userType);
}
