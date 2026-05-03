package net.luversof.web.gate.stock.domain;

import java.util.List;
import java.util.UUID;

public record StockItem(UUID id, String symbol, String name, String market, List<String> tags) {

	public StockItem {
		tags = tags != null ? List.copyOf(tags) : List.of();
	}
}
