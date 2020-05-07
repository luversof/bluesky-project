package net.luversof.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;

@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUsername(String username);
	
	Optional<User> findByExternalIdAndUserType(String externalId, UserType userType);
	
	List<User> findByIdIn(Collection<UUID> ids);
}
