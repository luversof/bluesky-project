package net.luversof.user.repository;

import static net.luversof.user.UserConstants.USER_TRANSACTIONMANAGER;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.UserAuthority;

@Transactional(USER_TRANSACTIONMANAGER)
public interface UserAuthorityRepository extends JpaRepository<UserAuthority, Long> {
}
