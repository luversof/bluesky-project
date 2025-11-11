package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.stock.domain.StockPrice;
import net.luversof.api.stock.repository.StockPriceRepository;

@Service
public class StockPriceService {

	@Setter(onMethod_ = @Autowired)
	private StockPriceRepository stockPriceRepository;

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
}
