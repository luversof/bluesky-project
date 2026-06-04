package net.luversof.api.stock.domain;

import java.time.LocalDate;
import java.util.UUID;

public record StockItemTradeDate(UUID stockItemId, LocalDate tradeDate) {}
