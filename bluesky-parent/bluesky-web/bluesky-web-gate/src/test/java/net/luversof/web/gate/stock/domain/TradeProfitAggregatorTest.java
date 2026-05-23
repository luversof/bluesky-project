package net.luversof.web.gate.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class TradeProfitAggregatorTest {

	@Test
	void avgBuyPriceUsesCurrentHoldingBasisInsteadOfHistoricalBuyTotals() {
		var sums = TradeProfitAggregator.aggregate(
				List.of(tradeProfit("466231000", "71886.79", "71887.13", 5043)));

		assertThat(sums.avgBuyPrice()).isEqualByComparingTo("71886.79");
		assertThat(sums.avgBuyPriceNet()).isEqualByComparingTo("71887.13");
	}

	@Test
	void avgBuyPriceUsesWeightedAverageAcrossHoldings() {
		var sums = TradeProfitAggregator.aggregate(
				List.of(
						tradeProfit("120000000", "70000.00", "70010.00", 1000),
						tradeProfit("180000000", "80000.00", "80020.00", 500)));

		assertThat(sums.avgBuyPrice()).isEqualByComparingTo("73333.33");
		assertThat(sums.avgBuyPriceNet()).isEqualByComparingTo("73346.67");
	}

	private TradeProfit tradeProfit(
			String totalBuyAmount,
			String averageBuyPrice,
			String averageBuyPriceNet,
			int holdingQuantity) {
		return new TradeProfit(
				null,
				"삼성전자",
				null,
				"한국투자증권 위탁",
				new BigDecimal(totalBuyAmount),
				new BigDecimal(averageBuyPrice),
				0,
				null,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				holdingQuantity,
				null,
				null,
				null,
				null,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal(totalBuyAmount),
				BigDecimal.ZERO,
				new BigDecimal(averageBuyPriceNet),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO);
	}
}