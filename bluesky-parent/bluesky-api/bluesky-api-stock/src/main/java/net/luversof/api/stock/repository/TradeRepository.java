package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Trade;

public interface TradeRepository extends CrudRepository<Trade, UUID> {
	
	List<Trade> findByAccountId(UUID accountId);
	
	List<Trade> findByAccountIdIn(List<UUID> accountIdList);
	
	List<Trade> findByAccountIdInAndTradeDateBetween(List<UUID> accountIdList, Instant startDate, Instant endDate);
	
	List<Trade> findByAccountIdInAndStockItemIdIn(List<UUID> accountIdList, List<UUID> stockItemIdList);
	
	List<Trade> findByAccountIdInAndStockItemIdInAndTradeDateBetween(List<UUID> accountIdList, List<UUID> stockItemIdList, Instant startDate, Instant endDate);

	long deleteByAccountId(UUID accountId);

}
