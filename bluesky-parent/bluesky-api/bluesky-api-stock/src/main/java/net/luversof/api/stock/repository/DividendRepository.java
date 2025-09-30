package net.luversof.api.stock.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Dividend;

public interface DividendRepository extends CrudRepository<Dividend, UUID> {

	long deleteByAccountId(UUID accountId);

}
