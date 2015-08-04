package net.luversof.user.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	User findByUsername(String username);
}
