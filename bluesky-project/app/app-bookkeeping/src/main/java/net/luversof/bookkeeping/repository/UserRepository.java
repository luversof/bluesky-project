package net.luversof.bookkeeping.repository;

import net.luversof.bookkeeping.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
