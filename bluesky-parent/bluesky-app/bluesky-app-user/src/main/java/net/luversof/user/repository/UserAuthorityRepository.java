package net.luversof.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.user.domain.UserAuthority;

public interface UserAuthorityRepository extends JpaRepository<UserAuthority, Long> {
}
