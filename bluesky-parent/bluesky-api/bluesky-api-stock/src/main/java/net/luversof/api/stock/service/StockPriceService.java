package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockPrice;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.StockPriceRepository;
import java.time.Instant;

@Service
public class StockPriceService {

	@Autowired
	private StockPriceRepository stockPriceRepository;

	@Autowired
	private StockPriceHistoryRepository stockPriceHistoryRepository;

	public void setStockPriceRepository(StockPriceRepository stockPriceRepository) {
		this.stockPriceRepository = stockPriceRepository;
	}

	public void setStockPriceHistoryRepository(StockPriceHistoryRepository stockPriceHistoryRepository) {
		this.stockPriceHistoryRepository = stockPriceHistoryRepository;
	}

	/**
	 * 종목의 현재가 조회
	 * 
	 * @param stockItemId 종목 ID
	 * @return 현재가 (없으면 0)
	 */
	public BigDecimal getCurrentPrice(UUID stockItemId) {
		return stockPriceRepository.findByStockItemId(stockItemId)
				.map(StockPrice::getPrice)
				.orElse(BigDecimal.ZERO);
	}

	/**
	 * 요청 시점(at)에 가장 근접한(같거나 이전) 일별 종가를 반환합니다.
	 * 히스토리가 없으면 현재가로 폴백합니다.
	 */
	public BigDecimal getPriceAt(UUID stockItemId, Instant at) {
		if (at == null)
			return getCurrentPrice(stockItemId);
		return stockPriceHistoryRepository
				.findTopByStockItemIdAndPriceDateLessThanEqualOrderByPriceDateDesc(stockItemId, at)
				.map(StockPriceHistory::getPrice)
				.orElseGet(() -> getCurrentPrice(stockItemId));
	}

	/**
	 * 종목의 현재가 저장/업데이트
	 * 
	 * @param stockItemId 종목 ID
	 * @param price       현재가
	 * @return 저장된 StockPrice
	 */
	public StockPrice savePrice(UUID stockItemId, BigDecimal price) {
		StockPrice stockPrice = stockPriceRepository.findByStockItemId(stockItemId)
				.orElse(new StockPrice());

		stockPrice.setStockItemId(stockItemId);
		stockPrice.setPrice(price);

		return stockPriceRepository.save(stockPrice);
	}

	public List<StockPriceHistory> getPriceHistory(Iterable<UUID> stockItemIdList, Instant start, Instant end) {
		return stockPriceHistoryRepository.findByStockItemIdInAndPriceDateBetween(stockItemIdList, start, end);
	}
}
