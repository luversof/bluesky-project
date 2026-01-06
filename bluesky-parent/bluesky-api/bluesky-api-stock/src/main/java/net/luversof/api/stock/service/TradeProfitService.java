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

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestGroup;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * 통합 주식 손익 계산 서비스
 * 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공
 */
@Service
public class TradeProfitService {

	@Autowired
	private AccountService accountService;

	@Autowired
	private TradeService tradeService;

	@Autowired
	private StockPriceService stockPriceService;

	public void setAccountService(AccountService accountService) {
		this.accountService = accountService;
	}

	public void setTradeService(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	public void setStockPriceService(StockPriceService stockPriceService) {
		this.stockPriceService = stockPriceService;
	}

	public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
		// 요청 기준으로 tradeList를 조회
		// 기간 요청이 있더라도 평단가 계산을 위해 전체 데이터를 조회해야 함
		List<Trade> tradeList = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
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

				yield tradeService.findByAccountIdIn(request.accountIdList());
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

				yield tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
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

				yield tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
						request.stockItemIdList());
			}
		};

		// 그룹별로 기본 손익 계산
		List<TradeProfit> base = switch (request.groupBy()) {
			case ACCOUNT_AND_STOCKITEM -> calculateProfitByAccountAndStock(tradeList, request);
			case STOCKITEM -> calculateProfitByStock(tradeList, request);
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
			enrichWithNet(tradesForRow, p, request);
			enriched.add(p);
		}
		return enriched;
	}

	/**
	 * accountId+stockItemId별 통합 손익 통계 (실현손익 + 미실현손익)
	 */
	public List<TradeProfit> calculateProfitByAccountAndStock(List<Trade> tradeList, TradeProfitRequest request) {
		Map<String, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
		List<TradeProfit> result = new ArrayList<>();

		for (List<Trade> group : grouped.values()) {
			Trade first = group.get(0);
			UUID accountId = first.getAccountId();
			UUID stockItemId = first.getStockItemId();

			TradeProfit profit = calculateStockProfit(group, accountId, stockItemId, request);
			result.add(profit);
		}
		return result;
	}

	/**
	 * stockItemId별 통합 손익 통계 (accountId 무시, 실현손익 + 미실현손익)
	 */
	public List<TradeProfit> calculateProfitByStock(List<Trade> tradeList, TradeProfitRequest request) {
		Map<UUID, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(Trade::getStockItemId));
		List<TradeProfit> result = new ArrayList<>();

		for (Map.Entry<UUID, List<Trade>> entry : grouped.entrySet()) {
			UUID stockItemId = entry.getKey();
			List<Trade> group = entry.getValue();

			TradeProfit profit = calculateStockProfit(group, null, stockItemId, request);
			result.add(profit);
		}
		return result;
	}

	/**
	 * 개별 그룹에 대한 통합 손익 계산
	 */
	private TradeProfit calculateStockProfit(List<Trade> trades, UUID accountId, UUID stockItemId, TradeProfitRequest request) {
		// 1. 전체 기간 통계 (평단가 계산용 - Cost Basis)
		int globalTotalBuyQuantity = trades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity)
				.sum();
		BigDecimal globalTotalBuyAmount = trades.stream()
				.filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal globalAverageBuyPrice = globalTotalBuyQuantity > 0
				? globalTotalBuyAmount.divide(BigDecimal.valueOf(globalTotalBuyQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		int globalTotalSellQuantity = trades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity)
				.sum();

		// 2. 조회 기간 필터링
		List<Trade> periodTrades = trades;
		if (request.hasDateRange()) {
			periodTrades = trades.stream().filter(t -> {
				boolean afterStart = (request.startDate() == null) || !t.getTradeDate().isBefore(request.startDate());
				boolean beforeEnd = (request.endDate() == null) || !t.getTradeDate().isAfter(request.endDate());
				return afterStart && beforeEnd;
			}).toList();
		}

		// 3. 조회 기간 통계 (표시용 매수, 매도 합계)
		int totalBuyQuantity = periodTrades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity)
				.sum();
		BigDecimal totalBuyAmount = periodTrades.stream()
				.filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		int totalSellQuantity = periodTrades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity)
				.sum();
		BigDecimal totalSellAmount = periodTrades.stream()
				.filter(t -> t.getType() == TradeType.SELL)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal averageSellPrice = totalSellQuantity > 0
				? totalSellAmount.divide(BigDecimal.valueOf(totalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;
		
		// 4. 실현손익 계산: 기간 내 매도금액 - (전체평단가 * 기간내 매도수량)
		BigDecimal realizedProfit = totalSellAmount
				.subtract(globalAverageBuyPrice.multiply(BigDecimal.valueOf(totalSellQuantity)));

		// 5. 보유 관련 계산 (미실현 손익) - 현재 시점 기준 (Snapshot)
		int holdingQuantity = globalTotalBuyQuantity - globalTotalSellQuantity;
		BigDecimal currentPrice = stockPriceService.getCurrentPrice(stockItemId);
		BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));
		BigDecimal evaluationProfit = evaluationAmount
				.subtract(globalAverageBuyPrice.multiply(BigDecimal.valueOf(holdingQuantity)));

		// 총 손익 계산 (기간 실현 + 현재 미실현)
		BigDecimal totalProfit = realizedProfit.add(evaluationProfit);

		TradeProfit profit = new TradeProfit();
		profit.setStockItemId(stockItemId);
		profit.setAccountId(accountId);
		profit.setTotalBuyAmount(totalBuyAmount); // Period
		profit.setAverageBuyPrice(globalAverageBuyPrice); // Global Cost Basis
		profit.setTotalSellQuantity(totalSellQuantity); // Period
		profit.setAverageSellPrice(averageSellPrice); // Period
		profit.setTotalSellAmount(totalSellAmount); // Period
		profit.setRealizedProfit(realizedProfit); // Period Realized
		profit.setHoldingQuantity(holdingQuantity); // Global/Current
		profit.setCurrentPrice(currentPrice);
		profit.setEvaluationAmount(evaluationAmount); // Global/Current
		profit.setEvaluationProfit(evaluationProfit); // Global/Current
		profit.setTotalProfit(totalProfit);

		return profit;
	}

	/**
	 * 수수료/세금을 고려한 NET 필드 계산 및 설정
	 */
	private void enrichWithNet(List<Trade> trades, TradeProfit profit, TradeProfitRequest request) {
		// 1. GLOBAL Custom for Net Avg Price
		int globalTotalBuyQuantity = trades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity).sum();
		BigDecimal globalTotalBuyAmount = trades.stream().filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal globalTotalBuyFee = trades.stream().filter(t -> t.getType() == TradeType.BUY)
				.map(t -> nz(t.getFee()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal globalTotalBuyCost = globalTotalBuyAmount.add(globalTotalBuyFee);
		BigDecimal globalAverageBuyPriceNet = globalTotalBuyQuantity > 0
				? globalTotalBuyCost.divide(BigDecimal.valueOf(globalTotalBuyQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// 2. PERIOD Statistics
		List<Trade> periodTrades = trades;
		if (request.hasDateRange()) {
			periodTrades = trades.stream().filter(t -> {
				boolean afterStart = (request.startDate() == null) || !t.getTradeDate().isBefore(request.startDate());
				boolean beforeEnd = (request.endDate() == null) || !t.getTradeDate().isAfter(request.endDate());
				return afterStart && beforeEnd;
			}).toList();
		}

		// Calculate Period totals
		int totalSellQuantity = periodTrades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity).sum();

		BigDecimal totalBuyFee = periodTrades.stream().filter(t -> t.getType() == TradeType.BUY)
				.map(t -> nz(t.getFee()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSellFee = periodTrades.stream().filter(t -> t.getType() == TradeType.SELL)
				.map(t -> nz(t.getFee()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSellTax = periodTrades.stream().filter(t -> t.getType() == TradeType.SELL)
				.map(t -> nz(t.getTax()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		profit.setTotalBuyFee(totalBuyFee);
		profit.setTotalSellFee(totalSellFee);
		profit.setTotalSellTax(totalSellTax);

		BigDecimal totalBuyCost = (profit.getTotalBuyAmount() == null ? BigDecimal.ZERO : profit.getTotalBuyAmount())
				.add(totalBuyFee);
		
		// TotalSellProceeds = PeriodSellAmount - Fees - Taxes
		BigDecimal totalSellProceeds = (profit.getTotalSellAmount() == null ? BigDecimal.ZERO : profit.getTotalSellAmount())
				.subtract(totalSellFee).subtract(totalSellTax);
		
		profit.setTotalBuyCost(totalBuyCost);
		profit.setTotalSellProceeds(totalSellProceeds);

		BigDecimal averageSellPriceNet = totalSellQuantity > 0
				? totalSellProceeds.divide(BigDecimal.valueOf(totalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// Realized Net = PeriodSellProceeds - (GlobalAvgBuyNet * PeriodSellQty)
		BigDecimal realizedProfitNet = totalSellProceeds
				.subtract(globalAverageBuyPriceNet.multiply(BigDecimal.valueOf(totalSellQuantity)));
		
		// Evaluation Net = EvaluationAmount - (GlobalAvgBuyNet * HoldingQuantity)
		BigDecimal evaluationProfitNet = profit.getEvaluationAmount()
				.subtract(globalAverageBuyPriceNet.multiply(BigDecimal.valueOf(profit.getHoldingQuantity())));
		
		BigDecimal totalProfitNet = realizedProfitNet.add(evaluationProfitNet);

		profit.setAverageBuyPriceNet(globalAverageBuyPriceNet);
		profit.setAverageSellPriceNet(averageSellPriceNet);
		profit.setRealizedProfitNet(realizedProfitNet);
		profit.setEvaluationProfitNet(evaluationProfitNet);
		profit.setTotalProfitNet(totalProfitNet);
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	/**
	 * 시간 시계열 집계: start ~ end (inclusive) 일별 포트폴리오 가치(실현 + 미실현) 스냅샷을 반환합니다.
	 * 현재의 제약: 과거 일별 시세가 없으므로 미실현 평가액은 현재가(`stockPriceService.getCurrentPrice`)를 사용한
	 * 근사치입니다.
	 */
	public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(TradeProfitRequest request, String granularity) {
		Instant end = request.endDate() != null ? request.endDate() : Instant.now();
		Instant start = request.startDate() != null ? request.startDate() : end.minus(90, ChronoUnit.DAYS);

		// 1) 조회 대상 트레이드: 기존 calculateProfit과 유사한 기준으로 대상 계좌/종목의 전체 트레이드를 조회(범위 제한 없이)
		List<Trade> allTrades = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.accountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(request.accountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdInAndStockItemIdIn(accountList.stream().map(Account::getId).toList(),
						request.stockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
				var accountList = accountService.findByIdIn(request.accountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
						request.stockItemIdList());
			}
		};

		// 정렬: 거래일 기준 오름차순
		allTrades.sort(Comparator.comparing(Trade::getTradeDate));

		// 누적 상태를 관리: 종목별 누적 합계
		record Cumul(long buyQty, long sellQty, java.math.BigDecimal buyAmount, java.math.BigDecimal sellAmount) {
		}

		Map<java.util.UUID, Cumul> cumulMap = new TreeMap<>();

		List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();

		Iterator<Trade> it = allTrades.iterator();
		Trade nextTrade = it.hasNext() ? it.next() : null;

		java.math.BigDecimal prevCumulativeRealized = null;

		Instant cur = start.truncatedTo(ChronoUnit.DAYS);
		while (!cur.isAfter(end)) {
			// advance trades up to cur (inclusive)
			long tradesCountForDay = 0L;
			long tradesVolumeForDay = 0L;
			while (nextTrade != null && !nextTrade.getTradeDate().isAfter(cur)) {
				java.util.UUID sid = nextTrade.getStockItemId();
				Cumul c = cumulMap.get(sid);
				if (c == null)
					c = new Cumul(0L, 0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);

				if (nextTrade.getType() == TradeType.BUY) {
					c = new Cumul(c.buyQty + nextTrade.getQuantity(), c.sellQty, c.buyAmount.add(
							nz(nextTrade.getPrice().multiply(java.math.BigDecimal.valueOf(nextTrade.getQuantity())))),
							c.sellAmount);
				} else {
					c = new Cumul(c.buyQty, c.sellQty + nextTrade.getQuantity(), c.buyAmount, c.sellAmount.add(
							nz(nextTrade.getPrice().multiply(java.math.BigDecimal.valueOf(nextTrade.getQuantity())))));
				}
				cumulMap.put(sid, c);
				tradesCountForDay++;
				tradesVolumeForDay += nextTrade.getQuantity();
				nextTrade = it.hasNext() ? it.next() : null;
			}

			// 계산: 가격 의존성을 제거하고, 누적 실현손익 및 일별 실현증가/거래 통계만 계산
			java.math.BigDecimal cumulativeRealized = java.math.BigDecimal.ZERO;
			for (Map.Entry<java.util.UUID, Cumul> e : cumulMap.entrySet()) {
				Cumul c = e.getValue();
				java.math.BigDecimal realized = c.sellAmount.subtract(c.buyAmount);
				cumulativeRealized = cumulativeRealized.add(realized);
			}

			java.math.BigDecimal dailyRealized = prevCumulativeRealized == null ? java.math.BigDecimal.ZERO
					: cumulativeRealized.subtract(prevCumulativeRealized);

			series.add(new TradeProfitTimeSeriesPoint(cur, cumulativeRealized, dailyRealized, tradesCountForDay,
					tradesVolumeForDay));

			prevCumulativeRealized = cumulativeRealized;
			cur = cur.plus(1, ChronoUnit.DAYS);
		}

		return series;
	}

}