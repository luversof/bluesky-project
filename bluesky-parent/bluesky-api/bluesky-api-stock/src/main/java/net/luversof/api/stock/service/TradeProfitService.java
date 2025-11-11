package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestGroup;

/**
 * 통합 주식 손익 계산 서비스
 * 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공
 */
@Service
public class TradeProfitService {

	@Setter(onMethod_ = @Autowired)
	private AccountService accountService;

	@Setter(onMethod_ = @Autowired)
	private TradeService tradeService;

	@Setter(onMethod_ = @Autowired)
	private StockPriceService stockPriceService;

	public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
		// 요청 기준으로 tradeList를 조회
		List<Trade> tradeList = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndTradeDateBetween(
								accountList.stream().map(Account::getId).toList(), request.startDate(),
								request.endDate())
						: tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.accountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(account -> {
					if (!account.getUserId().equals(request.userId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.userId(), account.getId());
					}
				});

				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndTradeDateBetween(request.accountIdList(),
								request.startDate(), request.endDate())
						: tradeService.findByAccountIdIn(request.accountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(x -> {
					if (!x.getUserId().equals(request.userId())) {
						StockErrorCode.INVALID_USER_ID.throwException();
					}
				});

				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndStockItemIdInAndTradeDateBetween(request.accountIdList(),
								request.stockItemIdList(), request.startDate(), request.endDate())
						: tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
								request.stockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
				var accountList = accountService.findByIdIn(request.accountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(account -> {
					if (!account.getUserId().equals(request.userId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.userId(), account.getId());
					}
				});

				yield request.hasDateRange()
						? tradeService.findByAccountIdInAndStockItemIdInAndTradeDateBetween(request.accountIdList(),
								request.stockItemIdList(), request.startDate(), request.endDate())
						: tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
								request.stockItemIdList());
			}
		};

		// 그룹별로 기본 손익 계산
		List<TradeProfit> base = switch (request.groupBy()) {
			case ACCOUNT_AND_STOCKITEM -> calculateProfitByAccountAndStock(tradeList);
			case STOCKITEM -> calculateProfitByStock(tradeList);
		};

		if (base.isEmpty())
			return base;

		// NET 필드 추가 (수수료/세금 고려)
		Map<String, List<Trade>> byAccountAndStock = new HashMap<>();
		Map<UUID, List<Trade>> byStock = new HashMap<>();
		if (request.groupBy() == TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM) {
			byAccountAndStock = tradeList.stream()
					.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
		} else {
			byStock = tradeList.stream().collect(Collectors.groupingBy(Trade::getStockItemId));
		}

		List<TradeProfit> enriched = new ArrayList<>(base.size());
		for (TradeProfit p : base) {
			List<Trade> tradesForRow;
			if (request.groupBy() == TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM) {
				tradesForRow = byAccountAndStock.getOrDefault(p.getAccountId() + "-" + p.getStockItemId(), List.of());
			} else {
				tradesForRow = byStock.getOrDefault(p.getStockItemId(), List.of());
			}
			enrichWithNet(tradesForRow, p);
			enriched.add(p);
		}
		return enriched;
	}

	/**
	 * accountId+stockItemId별 통합 손익 통계 (실현손익 + 미실현손익)
	 */
	public List<TradeProfit> calculateProfitByAccountAndStock(List<Trade> tradeList) {
		Map<String, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
		List<TradeProfit> result = new ArrayList<>();

		for (List<Trade> group : grouped.values()) {
			Trade first = group.get(0);
			UUID accountId = first.getAccountId();
			UUID stockItemId = first.getStockItemId();

			TradeProfit profit = calculateStockProfit(group, accountId, stockItemId);
			result.add(profit);
		}
		return result;
	}

	/**
	 * stockItemId별 통합 손익 통계 (accountId 무시, 실현손익 + 미실현손익)
	 */
	public List<TradeProfit> calculateProfitByStock(List<Trade> tradeList) {
		Map<UUID, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(Trade::getStockItemId));
		List<TradeProfit> result = new ArrayList<>();

		for (Map.Entry<UUID, List<Trade>> entry : grouped.entrySet()) {
			UUID stockItemId = entry.getKey();
			List<Trade> group = entry.getValue();

			TradeProfit profit = calculateStockProfit(group, null, stockItemId);
			result.add(profit);
		}
		return result;
	}

	/**
	 * 개별 그룹에 대한 통합 손익 계산
	 */
	private TradeProfit calculateStockProfit(List<Trade> trades, UUID accountId, UUID stockItemId) {
		// 매수 관련 계산
		int totalBuyQuantity = trades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity)
				.sum();
		BigDecimal totalBuyAmount = trades.stream()
				.filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal averageBuyPrice = totalBuyQuantity > 0
				? totalBuyAmount.divide(BigDecimal.valueOf(totalBuyQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// 매도 관련 계산 (실현 손익)
		int totalSellQuantity = trades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity)
				.sum();
		BigDecimal totalSellAmount = trades.stream()
				.filter(t -> t.getType() == TradeType.SELL)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal averageSellPrice = totalSellQuantity > 0
				? totalSellAmount.divide(BigDecimal.valueOf(totalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;
		BigDecimal realizedProfit = totalSellAmount.subtract(totalBuyAmount);

		// 보유 관련 계산 (미실현 손익)
		int holdingQuantity = totalBuyQuantity - totalSellQuantity;
		BigDecimal currentPrice = stockPriceService.getCurrentPrice(stockItemId);
		BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));
		BigDecimal evaluationProfit = evaluationAmount
				.subtract(averageBuyPrice.multiply(BigDecimal.valueOf(holdingQuantity)));

		// 총 손익 계산 (실현 + 미실현)
		BigDecimal totalProfit = realizedProfit.add(evaluationProfit);

		TradeProfit profit = new TradeProfit();
		profit.setStockItemId(stockItemId);
		profit.setAccountId(accountId);
		profit.setTotalBuyAmount(totalBuyAmount);
		profit.setAverageBuyPrice(averageBuyPrice);
		profit.setTotalSellQuantity(totalSellQuantity);
		profit.setAverageSellPrice(averageSellPrice);
		profit.setTotalSellAmount(totalSellAmount);
		profit.setRealizedProfit(realizedProfit);
		profit.setHoldingQuantity(holdingQuantity);
		profit.setCurrentPrice(currentPrice);
		profit.setEvaluationAmount(evaluationAmount);
		profit.setEvaluationProfit(evaluationProfit);
		profit.setTotalProfit(totalProfit);

		return profit;
	}

	/**
	 * 수수료/세금을 고려한 NET 필드 계산 및 설정
	 */
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

		BigDecimal realizedProfitNet = totalSellProceeds
				.subtract(averageBuyPriceNet.multiply(BigDecimal.valueOf(totalSellQuantity)));
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