package net.luversof.api.stock.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Account;

public interface AccountRepository extends CrudRepository<Account, UUID> {

	List<Account> findByUserId(UUID userId);

}
