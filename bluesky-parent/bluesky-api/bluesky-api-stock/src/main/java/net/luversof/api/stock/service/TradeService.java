package net.luversof.api.stock.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class TradeService {

	@Setter(onMethod_ = @Autowired)
	private TradeRepository tradeRepository;
	
	public Trade createTrade(Trade trade) {
		return tradeRepository.save(trade);
	}
	
	public List<Trade> findByAccountId(UUID accountId) {
		return tradeRepository.findByAccountId(accountId);
	}
	
	public List<Trade> findByAccountIdAndStockItemId(UUID accountId, UUID stockItemId) {
		return tradeRepository.findByAccountIdAndStockItemId(accountId, stockItemId);
	}
}
