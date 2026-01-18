package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestGroup;

/**
 * Decorator service: adds fee/tax-aware NET calculations on top of the base
 * TradeProfitService.
 * Marked @Primary so it is injected in place of the base service.
 */
@Service
@Primary
public class EnhancedTradeProfitService extends TradeProfitService {

	private final AccountService accountService;
	private final TradeService tradeService;

	public EnhancedTradeProfitService(AccountService accountService, TradeService tradeService) {
		this.accountService = accountService;
		this.tradeService = tradeService;
	}

	@Override
	public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
		// 1) Let the base service compute the legacy fields
		List<TradeProfit> base = super.calculateProfit(request);
		if (base.isEmpty())
			return base;

		// 2) Fetch the exact trades used by the base calculation (same switch logic)
		List<Trade> tradeList = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndTradeDateBetween(
								accountList.stream().map(Account::getId).toList(), request.getStartDate(),
								request.getEndDate())
						: tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				accountList.forEach(account -> {
					if (!account.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.getUserId(), account.getId());
					}
				});
				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndTradeDateBetween(request.getAccountIdList(),
								request.getStartDate(), request.getEndDate())
						: tradeService.findByAccountIdIn(request.getAccountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				accountList.forEach(x -> {
					if (!x.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException();
					}
				});
				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndStockItemIdInAndTradeDateBetween(
								accountList.stream().map(Account::getId).toList(),
								request.getStockItemIdList(), request.getStartDate(), request.getEndDate())
						: tradeService.findByAccountIdInAndStockItemIdIn(
								accountList.stream().map(Account::getId).toList(),
								request.getStockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				accountList.forEach(account -> {
					if (!account.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.getUserId(), account.getId());
					}
				});
				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndStockItemIdInAndTradeDateBetween(request.getAccountIdList(),
								request.getStockItemIdList(), request.getStartDate(), request.getEndDate())
						: tradeService.findByAccountIdInAndStockItemIdIn(request.getAccountIdList(),
								request.getStockItemIdList());
			}
		};

		// 3) Build grouping maps to quickly fetch trades per result row
		Map<String, List<Trade>> byAccountAndStock = new HashMap<>();
		Map<UUID, List<Trade>> byStock = new HashMap<>();
		if (request.getGroupBy() == TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM) {
			byAccountAndStock = tradeList.stream()
					.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
		} else {
			byStock = tradeList.stream().collect(Collectors.groupingBy(Trade::getStockItemId));
		}

		// 4) Enrich each profit with NET fields
		List<TradeProfit> enriched = new ArrayList<>(base.size());
		for (TradeProfit p : base) {
			List<Trade> tradesForRow;
			if (request.getGroupBy() == TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM) {
				tradesForRow = byAccountAndStock.getOrDefault(p.getAccountId() + "-" + p.getStockItemId(), List.of());
			} else {
				tradesForRow = byStock.getOrDefault(p.getStockItemId(), List.of());
			}
			enrichWithNet(tradesForRow, p);
			if (request.hasDateRange()) {
				p.setEvaluationProfitNet(BigDecimal.ZERO);
				p.setTotalProfitNet(p.getRealizedProfitNet());
			}
			enriched.add(p);
		}
		return enriched;
	}

	private void enrichWithNet(List<Trade> trades, TradeProfit profit) {
		if (trades.isEmpty()) {
			profit.setTotalBuyFee(BigDecimal.ZERO);
			profit.setTotalSellFee(BigDecimal.ZERO);
			profit.setTotalSellTax(BigDecimal.ZERO);
			profit.setTotalBuyCost(profit.getTotalBuyAmount() == null ? BigDecimal.ZERO : profit.getTotalBuyAmount());
			profit.setTotalSellProceeds(
					profit.getTotalSellAmount() == null ? BigDecimal.ZERO : profit.getTotalSellAmount());
			profit.setAverageBuyPriceNet(profit.getAverageBuyPrice());
			profit.setAverageSellPriceNet(profit.getAverageSellPrice());
			profit.setRealizedProfitNet(profit.getRealizedProfit());
			profit.setEvaluationProfitNet(profit.getEvaluationProfit());
			profit.setTotalProfitNet(profit.getTotalProfit());
			return;
		}

		int totalBuyQuantity = trades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity)
				.sum();
		int totalSellQuantity = trades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity)
				.sum();

		BigDecimal totalBuyAmount = trades.stream().filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSellAmount = trades.stream().filter(t -> t.getType() == TradeType.SELL)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalBuyFee = trades.stream().filter(t -> t.getType() == TradeType.BUY)
				.map(t -> nz(t.getFee()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSellFee = trades.stream().filter(t -> t.getType() == TradeType.SELL)
				.map(t -> nz(t.getFee()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSellTax = trades.stream().filter(t -> t.getType() == TradeType.SELL)
				.map(t -> nz(t.getTax()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalBuyCost = totalBuyAmount.add(totalBuyFee);
		BigDecimal totalSellProceeds = totalSellAmount.subtract(totalSellFee).subtract(totalSellTax);

		BigDecimal averageBuyPriceNet = totalBuyQuantity > 0
				? totalBuyCost.divide(BigDecimal.valueOf(totalBuyQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;
		BigDecimal averageSellPriceNet = totalSellQuantity > 0
				? totalSellProceeds.divide(BigDecimal.valueOf(totalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// Use the DB Realized Profit (already in profit object from base service) as the Net Profit source of truth
		BigDecimal realizedProfitNet = profit.getRealizedProfit();
		
		BigDecimal evaluationProfitNet = profit.getEvaluationAmount()
				.subtract(averageBuyPriceNet.multiply(BigDecimal.valueOf(profit.getHoldingQuantity())));
		BigDecimal totalProfitNet = realizedProfitNet.add(evaluationProfitNet);

		profit.setTotalBuyFee(totalBuyFee);
		profit.setTotalSellFee(totalSellFee);
		profit.setTotalSellTax(totalSellTax);
		profit.setTotalBuyCost(totalBuyCost);
		profit.setTotalSellProceeds(totalSellProceeds);
		profit.setAverageBuyPriceNet(averageBuyPriceNet);
		profit.setAverageSellPriceNet(averageSellPriceNet);
		profit.setRealizedProfitNet(realizedProfitNet);
		profit.setEvaluationProfitNet(evaluationProfitNet);
		profit.setTotalProfitNet(totalProfitNet);
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}
}
