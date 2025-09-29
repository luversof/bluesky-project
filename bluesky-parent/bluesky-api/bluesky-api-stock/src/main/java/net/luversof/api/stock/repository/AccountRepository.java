package net.luversof.api.stock.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Account;

public interface AccountRepository extends CrudRepository<Account, UUID> {

}
