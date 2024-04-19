package net.luversof.api.bookkeeping.base.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.AccountType;

public interface AccountTypeRepository extends JpaRepository<AccountType, UUID> {

	List<AccountType> findByBookkeepingId(UUID bookkeepingId);

}
