package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.TransactionType;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, UUID> {

	List<TransactionType> findByLedgerId(UUID ledgerId);

}
