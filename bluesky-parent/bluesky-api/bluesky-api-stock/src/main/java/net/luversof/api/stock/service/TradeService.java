package net.luversof.api.stock.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class TradeService {

	@Autowired
	private TradeRepository tradeRepository;

	public void setTradeRepository(TradeRepository tradeRepository) {
		this.tradeRepository = tradeRepository;
	}

	public Trade createTrade(Trade trade) {
		return tradeRepository.save(trade);
	}

	public List<Trade> findByAccountId(UUID accountId) {
		return tradeRepository.findByAccountId(accountId);
	}

	public List<Trade> findByAccountIdIn(List<UUID> accountIdList) {
		return tradeRepository.findByAccountIdIn(accountIdList);
	}

	public List<Trade> findByAccountIdInAndTradeDateBetween(List<UUID> accountIdList, Instant startDate,
			Instant endDate) {
		return tradeRepository.findByAccountIdInAndTradeDateBetween(accountIdList, startDate, endDate);
	}

	public List<Trade> findByAccountIdInAndStockItemIdIn(List<UUID> accountIdList, List<UUID> stockItemIdList) {
		return tradeRepository.findByAccountIdInAndStockItemIdIn(accountIdList, stockItemIdList);
	}

	public List<Trade> findByAccountIdInAndStockItemIdInAndTradeDateBetween(List<UUID> accountIdList,
			List<UUID> stockItemIdList, Instant startDate, Instant endDate) {
		return tradeRepository.findByAccountIdInAndStockItemIdInAndTradeDateBetween(accountIdList, stockItemIdList,
				startDate, endDate);
	}

}
