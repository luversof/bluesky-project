package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockPriceService {
	BigDecimal getCurrentPrice(UUID stockItemId);
}
