package net.luversof.opensource.data.jpa.repository;



import net.luversof.opensource.data.jpa.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	User findByUsername(String username);
}
