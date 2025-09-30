package net.luversof.api.stock.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class TradeRepositoryService {

	@Setter(onMethod_ = @Autowired)
	private TradeRepository tradeRepository;
	
	public Trade createTrade(Trade trade) {
		return tradeRepository.save(trade);
	}
	
}
