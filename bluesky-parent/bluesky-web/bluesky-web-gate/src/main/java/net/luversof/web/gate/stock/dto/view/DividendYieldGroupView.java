package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.Instant;

public record DividendYieldGroupView(
		String label,
		BigDecimal totalNetAmount,
		BigDecimal averagePrincipalCost,
		BigDecimal averagePrincipalMarketValue,
		BigDecimal yieldOnCostPct,
		BigDecimal yieldOnMarketPct,
		long dividendCount,
		Instant lastDividendDate) {
}