package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	List<Account> findByBookkeepingId(UUID bookkeepingId);

}
