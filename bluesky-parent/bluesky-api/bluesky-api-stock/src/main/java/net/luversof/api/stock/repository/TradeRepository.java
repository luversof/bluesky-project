package net.luversof.api.stock.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Trade;

public interface TradeRepository extends CrudRepository<Trade, UUID> {
	
	List<Trade> findByAccountId(UUID accountId);
	
	List<Trade> findByAccountIdAndStockItemId(UUID accountId, UUID stockItemId); 

	long deleteByAccountId(UUID accountId);

}
