package net.luversof.security.repository;

import net.luversof.security.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, QueryDslPredicateExecutor<User> {
	User findByUsername(String username);
}
