package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeResponse;

/**
 * 통합 주식 손익 계산 서비스
 * 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공
 */
@Service
public class TradeProfitService {

	private static final Logger log = LoggerFactory.getLogger(TradeProfitService.class);

	@Autowired
	private AccountService accountService;

	@Autowired
	private TradeService tradeService;

	@Autowired
	private StockPriceService stockPriceService;

	@Autowired
	private ProfitCalculator profitCalculator;

	@Autowired
	private StockItemService stockItemService;

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
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(account -> {
					if (!account.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.getUserId(), account.getId());
					}
				});

				yield tradeService.findByAccountIdIn(request.getAccountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(x -> {
					if (!x.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException();
					}
				});

				yield tradeService.findByAccountIdInAndStockItemIdIn(accountList.stream().map(Account::getId).toList(),
						request.getStockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}

				accountList.stream().forEach(account -> {
					if (!account.getUserId().equals(request.getUserId())) {
						StockErrorCode.INVALID_USER_ID.throwException(request.getUserId(), account.getId());
					}
				});

				yield tradeService.findByAccountIdInAndStockItemIdIn(request.getAccountIdList(),
						request.getStockItemIdList());
			}
		};

		// 그룹별로 기본 손익 계산
		List<TradeProfit> base = switch (request.getGroupBy()) {
			case ACCOUNT_AND_STOCKITEM -> calculateProfitByAccountAndStock(tradeList, request);
			case STOCKITEM -> calculateProfitByStock(tradeList, request);
		};

		if (base.isEmpty())
			return base;

		return base;
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

			TradeProfit profit = profitCalculator.calculate(group, request, stockPriceService);
			if (request.hasDateRange()) {
				// Include if Realized Profit != 0 OR if there was any Sell Activity OR any Buy
				// Activity
				boolean hasProfit = profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0;
				boolean hasSell = profit.getTotalSellAmount() != null
						&& profit.getTotalSellAmount().compareTo(BigDecimal.ZERO) > 0;
				boolean hasBuy = profit.getTotalBuyAmount() != null
						&& profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0;
				if (hasProfit || hasSell || hasBuy) {
					result.add(profit);
				}
			} else {
				if (!isEmptyProfit(profit)) {
					result.add(profit);
				}
			}
		}
		return result;
	}

	/**
	 * stockItemId별 통합 손익 통계 (accountId 무시, 실현손익 + 미실현손익)
	 * StockItem Symbol이 같으면 통합하여 계산 (중복 데이터 보정)
	 */
	public List<TradeProfit> calculateProfitByStock(List<Trade> tradeList, TradeProfitRequest request) {
		// 1. StockItem 정보 조회 (Symbol 기준 병합을 위해)
		var stockItemIds = tradeList.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
		Map<UUID, StockItem> stockItemMap = new HashMap<>();
		stockItemService.findAllById(stockItemIds).forEach(si -> stockItemMap.put(si.getId(), si));

		// 2. 그룹핑 (Symbol -> Name -> ID 순으로 식별)
		Map<String, List<Trade>> grouped = tradeList.stream().collect(Collectors.groupingBy(t -> {
			var si = stockItemMap.get(t.getStockItemId());
			if (si != null) {
				if (si.getSymbol() != null && !si.getSymbol().isBlank()) {
					return "S:" + si.getSymbol(); // Symbol Prefix
				}
				// 2026-01-17: Name match fallback for inconsistent data
				// Remove spaces to ensure better matching (e.g. "Samsung Electronics" vs
				// "SamsungElectronics")
				// But risking collision? TIGER REITs name is specific enough.
				if (si.getName() != null && !si.getName().isBlank()) {
					return "N:" + si.getName().trim();
				}
			}
			return "I:" + t.getStockItemId().toString();
		}));

		List<TradeProfit> result = new ArrayList<>();

		for (List<Trade> group : grouped.values()) {
			if (group.isEmpty())
				continue;

			// 대표 ID 사용 (첫번째 Trade의 StockItemId)
			UUID stockItemId = group.get(0).getStockItemId();

			TradeProfit profit = profitCalculator.calculate(group, request, stockPriceService);
			if (request.hasDateRange()) {
				// Include if Realized Profit != 0 OR if there was any Sell Activity OR any Buy
				// Activity
				boolean hasProfit = profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0;
				boolean hasSell = profit.getTotalSellAmount() != null
						&& profit.getTotalSellAmount().compareTo(BigDecimal.ZERO) > 0;
				boolean hasBuy = profit.getTotalBuyAmount() != null
						&& profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0;
				if (hasProfit || hasSell || hasBuy) {
					result.add(profit);
				}
			} else {
				if (!isEmptyProfit(profit)) {
					result.add(profit);
				}
			}
		}
		return result;
	}

	private boolean isEmptyProfit(TradeProfit profit) {
		return profit.getHoldingQuantity() == 0
				&& profit.getTotalSellQuantity() == 0
				&& (profit.getTotalBuyAmount() == null || profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) == 0);
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	/**
	 * 시간 시계열 집계: 전체 거래 내역을 바탕으로 Rolling WMA 계산을 수행한 후,
	 * 요청된 기간(start ~ end)에 해당하는 일별 누적 실현손익 스냅샷을 반환합니다.
	 */
	public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(TradeProfitRequest request, String granularity) {
		Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
		Instant start = request.getStartDate() != null ? request.getStartDate() : end.minus(90, ChronoUnit.DAYS);

		// 1) 전체 트레이드 조회 (날짜 제한 없이 전체 로딩)
		List<Trade> allTrades = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdIn(request.getAccountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.getUserId());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdInAndStockItemIdIn(accountList.stream().map(Account::getId).toList(),
						request.getStockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
				var accountList = accountService.findByIdIn(request.getAccountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.INVALID_USER_ID.throwException();
				}
				yield tradeService.findByAccountIdInAndStockItemIdIn(request.getAccountIdList(),
						request.getStockItemIdList());
			}
		};

		if (allTrades.isEmpty()) {
			return new ArrayList<>();
		} // 2) StockItem 정보 로딩 및 그룹핑 키 생성 (calculateProfitByStock과 동일 로직)
		var stockItemIds = allTrades.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
		Map<UUID, StockItem> stockItemMap = new HashMap<>();
		stockItemService.findAllById(stockItemIds).forEach(si -> stockItemMap.put(si.getId(), si));

		// 그룹핑 키 생성 함수
		java.util.function.Function<Trade, String> getGroupKey = t -> {
			var si = stockItemMap.get(t.getStockItemId());
			if (si != null) {
				if (si.getSymbol() != null && !si.getSymbol().isBlank()) {
					return "S:" + si.getSymbol();
				}
				if (si.getName() != null && !si.getName().isBlank()) {
					return "N:" + si.getName().trim();
				}
			}
			return "I:" + t.getStockItemId().toString();
		};

		// 3) 거래 정렬 (날짜 오름차순)
		// 같은 날짜 내에서는 BUY 먼저 처리 (논리적 재고 확보)
		allTrades.sort((t1, t2) -> {
			int dateCompare = t1.getTradeDate().compareTo(t2.getTradeDate());
			if (dateCompare != 0)
				return dateCompare;
			if (t1.getType() == t2.getType())
				return 0;
			return t1.getType() == TradeType.BUY ? -1 : 1;
		});

		// 4) 시뮬레이션 상태 관리 (WMA)
		class WmaState {
			long quantity = 0;
			BigDecimal totalCost = BigDecimal.ZERO; // Gross
			BigDecimal totalCostNet = BigDecimal.ZERO; // Net (Fee included)
			UUID stockItemId;
		}
		Map<String, WmaState> stateMap = new HashMap<>();

		BigDecimal globalCumulativeRealized = BigDecimal.ZERO;

		// 5) 시뮬레이션 루프
		// 시작일: 데이터가 있는 첫 날짜부터 시작 (Cost Basis 구축을 위해)
		Instant firstTradeDate = allTrades.get(0).getTradeDate().truncatedTo(ChronoUnit.DAYS);
		Instant simulationStart = firstTradeDate;
		// 출력 시작일: 요청된 start 날짜
		Instant outputStart = start.truncatedTo(ChronoUnit.DAYS);
		Instant outputEnd = end.truncatedTo(ChronoUnit.DAYS);

		// Price History (Bulk Load)
		// List<StockPriceHistory> priceHistory =
		// stockPriceService.getPriceHistory(stockItemIds, outputStart, outputEnd);
		// Map<Instant, Map<UUID, BigDecimal>> dailyPriceMap = new HashMap<>();
		// for (StockPriceHistory h : priceHistory) {
		// dailyPriceMap.computeIfAbsent(h.getPriceDate().truncatedTo(ChronoUnit.DAYS),
		// k -> new HashMap<>())
		// .put(h.getStockItemId(), h.getPrice());
		// }
		Map<Instant, Map<UUID, BigDecimal>> dailyPriceMap = new HashMap<>();

		Map<UUID, BigDecimal> lastKnownPrices = new HashMap<>();

		List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
		Iterator<Trade> it = allTrades.iterator();
		Trade nextTrade = it.hasNext() ? it.next() : null;

		Instant currentDay = simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
		// ※ 단, simulationStart가 outputStart보다 늦으면 (데이터가 미래에 시작),
		// outputStart ~ simulationStart 구간은 데이터 없음(0)으로 채워야 함.
		// 편의상 currentDay를 Math.min(simulationStart, outputStart)로 잡고 진행.
		if (outputStart.isBefore(simulationStart)) {
			currentDay = outputStart;
		} else {
			currentDay = simulationStart;
		}

		while (!currentDay.isAfter(outputEnd)) {
			// 해당 일자(currentDay)에 포함되는 모든 거래 처리
			long dailyTradeCount = 0;
			long dailyVolume = 0;
			BigDecimal dailyRealizedGain = BigDecimal.ZERO;

			// nextTrade가 currentDay의 끝(inclusive)까지인지 확인
			// tradeDate는 시분초 포함이므로, truncatedTo(DAYS) 결과가 currentDay와 같거나 이전이면 처리
			while (nextTrade != null) {
				Instant tradeDay = nextTrade.getTradeDate().truncatedTo(ChronoUnit.DAYS);
				if (tradeDay.isAfter(currentDay)) {
					break; // 미래의 거래는 대기
				}

				// 거래 처리 logic (WMA)
				Trade trade = nextTrade;
				String key = getGroupKey.apply(trade);
				WmaState state = stateMap.computeIfAbsent(key, k -> {
					WmaState s = new WmaState();
					s.stockItemId = trade.getStockItemId();
					return s;
				});
				if (state.stockItemId == null)
					state.stockItemId = trade.getStockItemId();

				BigDecimal fee = nz(trade.getFee());
				BigDecimal tax = nz(trade.getTax());
				int q = trade.getQuantity();
				BigDecimal price = trade.getPrice();
				BigDecimal amount = price.multiply(BigDecimal.valueOf(q));

				lastKnownPrices.put(trade.getStockItemId(), price);

				if (trade.getType() == TradeType.BUY) {
					if (q > 0) {
						state.quantity += q;
						state.totalCost = state.totalCost.add(amount);
						state.totalCostNet = state.totalCostNet.add(amount).add(fee);
					}
				} else if (trade.getType() == TradeType.SELL) {
					BigDecimal realProfit = nz(trade.getRealizedProfit());
					BigDecimal tradeSellAmount = price.multiply(BigDecimal.valueOf(q));

					// Deduce COGS from DB Profit for consistent holdings
					BigDecimal sellProceeds = tradeSellAmount.subtract(fee).subtract(tax);
					BigDecimal cogs = sellProceeds.subtract(realProfit);

					if (state.quantity > 0) {
						// Update State
						if (state.quantity >= q) {
							state.quantity -= q;
							state.totalCost = state.totalCost.subtract(cogs);
						} else {
							state.quantity = 0;
							state.totalCost = BigDecimal.ZERO;
						}

						if (state.quantity == 0) {
							state.totalCost = BigDecimal.ZERO;
						}
					}

					dailyRealizedGain = dailyRealizedGain.add(realProfit);

					dailyTradeCount++;
					dailyVolume += q;
				}

				nextTrade = it.hasNext() ? it.next() : null;
			}

			// 하루 마감 -> Global Cumulative Update
			globalCumulativeRealized = globalCumulativeRealized.add(dailyRealizedGain);

			// 출력 범위 내인지 확인 후 추가
			if (!currentDay.isBefore(outputStart)) {
				// Update Last Known Prices from History
				Map<UUID, BigDecimal> dayPrices = dailyPriceMap.getOrDefault(currentDay, Map.of());
				lastKnownPrices.putAll(dayPrices);

				// Calculate Holdings Value
				BigDecimal totalHoldingsValue = BigDecimal.ZERO;
				BigDecimal totalHoldingsCost = BigDecimal.ZERO;

				for (WmaState state : stateMap.values()) {
					if (state.quantity > 0) {
						totalHoldingsCost = totalHoldingsCost.add(state.totalCost);

						BigDecimal price = lastKnownPrices.get(state.stockItemId);
						if (price == null)
							price = BigDecimal.ZERO;

						BigDecimal value = price.multiply(BigDecimal.valueOf(state.quantity));
						totalHoldingsValue = totalHoldingsValue.add(value);
					}
				}

				BigDecimal cumulativeTotalProfit = globalCumulativeRealized
						.add(totalHoldingsValue.subtract(totalHoldingsCost));

				series.add(new TradeProfitTimeSeriesPoint(
						currentDay,
						globalCumulativeRealized,
						dailyRealizedGain,
						dailyTradeCount,
						dailyVolume,
						totalHoldingsValue,
						totalHoldingsCost,
						cumulativeTotalProfit));
			}

			currentDay = currentDay.plus(1, ChronoUnit.DAYS);
		}

		return series;
	}

	public List<TradeResponse> getTradeHistory(TradeSearchRequest request) {
		// 1. Fetch all trades for the accounts/stockItems
		List<Trade> tradeList = null;

		List<Account> accountList = accountService.findByUserId(request.userId());
		if (accountList.isEmpty()) {
			return List.of();
		}

		List<UUID> validAccountIds = accountList.stream().map(Account::getId).toList();

		if (request.accountIdList() != null && !request.accountIdList().isEmpty()) {
			// Validate requested accounts belong to user
			if (!validAccountIds.containsAll(request.accountIdList())) {
				StockErrorCode.INVALID_USER_ID.throwException();
			}
			if (request.stockItemIdList() != null && !request.stockItemIdList().isEmpty()) {
				tradeList = tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(),
						request.stockItemIdList());
			} else {
				tradeList = tradeService.findByAccountIdIn(request.accountIdList());
			}
		} else {
			// All user accounts
			if (request.stockItemIdList() != null && !request.stockItemIdList().isEmpty()) {
				tradeList = tradeService.findByAccountIdInAndStockItemIdIn(validAccountIds, request.stockItemIdList());
			} else {
				tradeList = tradeService.findByAccountIdIn(validAccountIds);
			}
		}

		if (tradeList == null || tradeList.isEmpty()) {
			return List.of();
		}

		// Map of StockItem Names
		Map<UUID, String> stockItemNames = new HashMap<>();
		stockItemService.findAll().forEach(item -> stockItemNames.put(item.getId(), item.getName()));

		List<TradeResponse> result = new ArrayList<>();

		// Group by accountId and stockItemId to calculate realized profit dynamically
		Map<String, List<Trade>> groupedTrades = tradeList.stream()
				.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));

		for (List<Trade> group : groupedTrades.values()) {
			// Sort ascending by date for calculation
			group.sort(Comparator.comparing(Trade::getTradeDate));

			long currentQuantity = 0;
			BigDecimal currentTotalCostNet = BigDecimal.ZERO;

			for (Trade trade : group) {
				BigDecimal fee = trade.getFee() != null ? trade.getFee() : BigDecimal.ZERO;
				BigDecimal tax = trade.getTax() != null ? trade.getTax() : BigDecimal.ZERO;
				int q = trade.getQuantity();
				BigDecimal price = trade.getPrice() != null ? trade.getPrice() : BigDecimal.ZERO;
				BigDecimal amount = price.multiply(BigDecimal.valueOf(q));

				BigDecimal calculatedRealizedProfit = null;

				if (trade.getType() == TradeType.BUY) {
					if (q > 0) {
						currentQuantity += q;
						currentTotalCostNet = currentTotalCostNet.add(amount).add(fee);
					}
				} else if (trade.getType() == TradeType.SELL) {
					BigDecimal sellProceeds = amount.subtract(fee).subtract(tax);
					BigDecimal unitCostNet = BigDecimal.ZERO;
					if (currentQuantity > 0) {
						unitCostNet = currentTotalCostNet.divide(BigDecimal.valueOf(currentQuantity), 10,
								RoundingMode.HALF_UP);
					}
					BigDecimal costOfGoodsSoldNet = unitCostNet.multiply(BigDecimal.valueOf(q));
					calculatedRealizedProfit = sellProceeds.subtract(costOfGoodsSoldNet);

					if (currentQuantity >= q) {
						currentQuantity -= q;
						currentTotalCostNet = currentTotalCostNet.subtract(costOfGoodsSoldNet);
					} else {
						currentQuantity = 0;
						currentTotalCostNet = BigDecimal.ZERO;
					}
					if (currentQuantity == 0) {
						currentTotalCostNet = BigDecimal.ZERO;
					}
				}

				// Filter by Date Range and Add to Result
				boolean inRange = true;
				if (request.startDate() != null && trade.getTradeDate().isBefore(request.startDate()))
					inRange = false;
				if (request.endDate() != null && trade.getTradeDate().isAfter(request.endDate()))
					inRange = false;

				if (inRange) {
					result.add(new TradeResponse(
							trade.getId(),
							trade.getAccountId(),
							trade.getStockItemId(),
							stockItemNames.getOrDefault(trade.getStockItemId(), ""),
							trade.getType(),
							trade.getQuantity(),
							trade.getPrice(),
							trade.getFee(),
							trade.getTax(),
							amount,
							calculatedRealizedProfit,
							trade.getTradeDate()));
				}
			}
		}

		// Global sort by date descending
		result.sort(Comparator.comparing(TradeResponse::tradeDate).reversed());

		return result;
	}

}