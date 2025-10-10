package net.luversof.web.gate.stock.domain;

import java.util.UUID;

public record StockItem(
	UUID id,
	String ticker,
	String name,
	String market) {

}
