package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.base.domain.Ledger;

@Transactional(readOnly = true)
public interface LedgerRepository extends JpaRepository<Ledger, UUID> {
	
	List<Ledger> findByUserId(UUID userId);

}
