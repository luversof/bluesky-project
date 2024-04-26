package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.EntryTransactionType;

public interface EntryTransactionTypeRepository extends JpaRepository<EntryTransactionType, UUID> {

	List<EntryTransactionType> findByBookkeepingId(UUID bookkeepingId);

}
