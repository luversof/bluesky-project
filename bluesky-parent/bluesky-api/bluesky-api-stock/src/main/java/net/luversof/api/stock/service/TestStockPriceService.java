package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class TestStockPriceService implements StockPriceService {

	@Override
	public BigDecimal getCurrentPrice(UUID stockItemId) {
		return BigDecimal.valueOf(10000);
	}

}
