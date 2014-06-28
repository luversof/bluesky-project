package net.luversof.user.repository;

import net.luversof.user.domain.UserAuthority;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthorityRepository extends JpaRepository<UserAuthority, Long> {
}
