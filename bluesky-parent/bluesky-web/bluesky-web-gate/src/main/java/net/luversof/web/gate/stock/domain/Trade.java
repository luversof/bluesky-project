package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import net.luversof.web.gate.stock.constant.TradeType;

public record Trade(
	UUID id,
	UUID accountId,
	UUID stockItemId,
	TradeType type, // "BUY" or "SELL"
	int quantity,
	BigDecimal price,
	BigDecimal fee,
	BigDecimal tax,
	Instant tradeDate
	) {

}
