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
import java.util.TreeMap;
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
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeResponse;

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

			TradeProfit profit = calculateStockProfit(group, accountId, stockItemId, request);
			if (request.hasDateRange()) {
				if (profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0) {
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
	 */
	public List<TradeProfit> calculateProfitByStock(List<Trade> tradeList, TradeProfitRequest request) {
		Map<UUID, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(Trade::getStockItemId));
		List<TradeProfit> result = new ArrayList<>();

		for (Map.Entry<UUID, List<Trade>> entry : grouped.entrySet()) {
			UUID stockItemId = entry.getKey();
			List<Trade> group = entry.getValue();

			TradeProfit profit = calculateStockProfit(group, null, stockItemId, request);
			if (request.hasDateRange()) {
				if (profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0) {
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

	/**
	 * 개별 그룹에 대한 통합 손익 계산 (FIFO Cost Basis)
	 */
	private TradeProfit calculateStockProfit(List<Trade> trades, UUID accountId, UUID stockItemId,
			TradeProfitRequest request) {
		// 1. Sort by date ascending to ensure FIFO order
		List<Trade> sortedTrades = new ArrayList<>(trades);
		sortedTrades.sort(Comparator.comparing(Trade::getTradeDate));

		// 2. FIFO Queue for tracking cost basis
		// Stores: Price, Remaining Quantity, Fee per share (to preserve precision as
		// much as possible)
		record BuyBlock(BigDecimal price, int quantity, BigDecimal feePerShare) {
		}
		java.util.Deque<BuyBlock> inventory = new java.util.ArrayDeque<>();

		// 3. Period accumulators for "Period" stats
		BigDecimal periodTotalBuyAmount = BigDecimal.ZERO;
		BigDecimal periodTotalBuyFee = BigDecimal.ZERO;

		int periodTotalSellQuantity = 0;
		BigDecimal periodTotalSellAmount = BigDecimal.ZERO;
		BigDecimal periodTotalSellFee = BigDecimal.ZERO;
		BigDecimal periodTotalSellTax = BigDecimal.ZERO;

		BigDecimal periodRealizedProfit = BigDecimal.ZERO; // Gross
		BigDecimal periodRealizedProfitNet = BigDecimal.ZERO; // Net

		// Date filter helpers
		Instant start = request.getStartDate();
		Instant end = request.getEndDate();

		for (Trade trade : sortedTrades) {
			
			if (end != null && trade.getTradeDate().isAfter(end))
				break;
			
			boolean inPeriod = true;
			if (start != null && trade.getTradeDate().isBefore(start))
				inPeriod = false;

			BigDecimal fee = nz(trade.getFee());
			BigDecimal tax = nz(trade.getTax());
			int q = trade.getQuantity();
			BigDecimal price = trade.getPrice();

			if (trade.getType() == TradeType.BUY) {
				if (q > 0) {
					// Use 10 decimal places for fee per share precision
					BigDecimal feePerShare = fee.divide(BigDecimal.valueOf(q), 10, RoundingMode.HALF_UP);
					inventory.addLast(new BuyBlock(price, q, feePerShare));
				}

				if (inPeriod) {
					periodTotalBuyAmount = periodTotalBuyAmount.add(price.multiply(BigDecimal.valueOf(q)));
					periodTotalBuyFee = periodTotalBuyFee.add(fee);
				}
			} else if (trade.getType() == TradeType.SELL) {
				BigDecimal tradeSellAmount = price.multiply(BigDecimal.valueOf(q));

				// Accumulate period stats
				if (inPeriod) {
					periodTotalSellQuantity += q;
					periodTotalSellAmount = periodTotalSellAmount.add(tradeSellAmount);
					periodTotalSellFee = periodTotalSellFee.add(fee);
					periodTotalSellTax = periodTotalSellTax.add(tax);
				}

				// Calculate Cost Basis (FIFO)
				BigDecimal tradeCost = BigDecimal.ZERO;
				BigDecimal tradeCostNet = BigDecimal.ZERO;
				int remainingToSell = q;

				while (remainingToSell > 0 && !inventory.isEmpty()) {
					BuyBlock block = inventory.peekFirst();
					int matchQty = Math.min(remainingToSell, block.quantity());

					BigDecimal blockCost = block.price().multiply(BigDecimal.valueOf(matchQty));
					BigDecimal blockFee = block.feePerShare().multiply(BigDecimal.valueOf(matchQty));

					tradeCost = tradeCost.add(blockCost);
					tradeCostNet = tradeCostNet.add(blockCost).add(blockFee);

					remainingToSell -= matchQty;

					if (matchQty == block.quantity()) {
						inventory.pollFirst();
					} else {
						// Partially consumed, replace head with remaining
						inventory.pollFirst();
						inventory.addFirst(
								new BuyBlock(block.price(), block.quantity() - matchQty, block.feePerShare()));
					}
				}

				// Calculate Realized Profit if in period
				if (inPeriod) {
					// Gross Realized Profit
					BigDecimal tradeProfit = tradeSellAmount.subtract(tradeCost);
					periodRealizedProfit = periodRealizedProfit.add(tradeProfit);

					// Net Realized Profit = (SellAmount - SellFee - SellTax) - (BuyCost +
					// BuyFeePart)
					BigDecimal tradeProceedsNet = tradeSellAmount.subtract(fee).subtract(tax);
					BigDecimal tradeProfitNet = tradeProceedsNet.subtract(tradeCostNet);
					periodRealizedProfitNet = periodRealizedProfitNet.add(tradeProfitNet);
				}
			}
		}

		// 4. Calculate Holdings (Snapshot at end of processing)
		int holdingQuantity = 0;
		BigDecimal holdingTotalCost = BigDecimal.ZERO;
		BigDecimal holdingTotalCostNet = BigDecimal.ZERO;

		for (BuyBlock block : inventory) {
			holdingQuantity += block.quantity();
			BigDecimal blockVal = block.price().multiply(BigDecimal.valueOf(block.quantity()));
			BigDecimal blockFee = block.feePerShare().multiply(BigDecimal.valueOf(block.quantity()));
			holdingTotalCost = holdingTotalCost.add(blockVal);
			holdingTotalCostNet = holdingTotalCostNet.add(blockVal).add(blockFee);
		}

		BigDecimal averageBuyPrice = holdingQuantity > 0
				? holdingTotalCost.divide(BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal averageBuyPriceNet = holdingQuantity > 0
				? holdingTotalCostNet.divide(BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// 5. Current Evaluation
		BigDecimal currentPrice = BigDecimal.ZERO;
		BigDecimal evaluationAmount = BigDecimal.ZERO;
		BigDecimal evaluationProfit = BigDecimal.ZERO;
		BigDecimal evaluationProfitNet = BigDecimal.ZERO;

		if (!request.hasDateRange()) {
			currentPrice = stockPriceService.getCurrentPrice(stockItemId);
			evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));

			// Profit on Holdings
			evaluationProfit = evaluationAmount.subtract(holdingTotalCost);
			evaluationProfitNet = evaluationAmount.subtract(holdingTotalCostNet);
		}

		// 6. Period Averages & Derived Stats
		BigDecimal averageSellPrice = periodTotalSellQuantity > 0
				? periodTotalSellAmount.divide(BigDecimal.valueOf(periodTotalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal periodTotalSellProceeds = periodTotalSellAmount.subtract(periodTotalSellFee)
				.subtract(periodTotalSellTax);
		BigDecimal averageSellPriceNet = periodTotalSellQuantity > 0
				? periodTotalSellProceeds.divide(BigDecimal.valueOf(periodTotalSellQuantity), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal periodTotalBuyCost = periodTotalBuyAmount.add(periodTotalBuyFee);

		BigDecimal totalProfit = periodRealizedProfit.add(evaluationProfit);
		BigDecimal totalProfitNet = periodRealizedProfitNet.add(evaluationProfitNet);

		// 7. Populate Result
		TradeProfit profit = new TradeProfit();
		profit.setStockItemId(stockItemId);
		profit.setAccountId(accountId);

		// Base Fields
		profit.setTotalBuyAmount(periodTotalBuyAmount);
		profit.setAverageBuyPrice(averageBuyPrice); // Average Cost of Holdings
		profit.setTotalSellQuantity(periodTotalSellQuantity);
		profit.setAverageSellPrice(averageSellPrice);
		profit.setTotalSellAmount(periodTotalSellAmount);
		profit.setRealizedProfit(periodRealizedProfit);
		profit.setHoldingQuantity(holdingQuantity);
		profit.setCurrentPrice(currentPrice);
		profit.setEvaluationAmount(evaluationAmount);
		profit.setEvaluationProfit(evaluationProfit);
		profit.setTotalProfit(totalProfit);

		// Net Fields
		profit.setTotalBuyFee(periodTotalBuyFee);
		profit.setTotalSellFee(periodTotalSellFee);
		profit.setTotalSellTax(periodTotalSellTax);
		profit.setTotalBuyCost(periodTotalBuyCost);
		profit.setTotalSellProceeds(periodTotalSellProceeds);
		profit.setAverageBuyPriceNet(averageBuyPriceNet);
		profit.setAverageSellPriceNet(averageSellPriceNet);
		profit.setRealizedProfitNet(periodRealizedProfitNet);
		profit.setEvaluationProfitNet(evaluationProfitNet);
		profit.setTotalProfitNet(totalProfitNet);

		return profit;
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
		Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
		Instant start = request.getStartDate() != null ? request.getStartDate() : end.minus(90, ChronoUnit.DAYS);

		// 1) 조회 대상 트레이드: 기존 calculateProfit과 유사한 기준으로 대상 계좌/종목의 전체 트레이드를 조회(범위 제한 없이)
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

		// 2. Group by Account & StockItem to perform FIFO calculation
		Map<String, List<Trade>> grouped = tradeList.stream()
				.collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));

		List<TradeResponse> result = new ArrayList<>();

		for (List<Trade> group : grouped.values()) {
			// Sort by date ASC for FIFO
			group.sort(Comparator.comparing(Trade::getTradeDate));

			record BuyBlock(BigDecimal price, int quantity, BigDecimal feePerShare) {
			}
			java.util.Deque<BuyBlock> inventory = new java.util.ArrayDeque<>();

			for (Trade trade : group) {
				BigDecimal realizedProfit = null;

				BigDecimal fee = nz(trade.getFee());
				int q = trade.getQuantity();
				BigDecimal price = trade.getPrice();

				if (trade.getType() == TradeType.BUY) {
					if (q > 0) {
						BigDecimal feePerShare = fee.divide(BigDecimal.valueOf(q), 10, RoundingMode.HALF_UP);
						inventory.addLast(new BuyBlock(price, q, feePerShare));
					}
				} else if (trade.getType() == TradeType.SELL) {
					// FIFO Calculation
					BigDecimal tradeCostNet = BigDecimal.ZERO;
					int remainingToSell = q;

					while (remainingToSell > 0 && !inventory.isEmpty()) {
						BuyBlock block = inventory.peekFirst();
						int matchQty = Math.min(remainingToSell, block.quantity());

						BigDecimal blockCost = block.price().multiply(BigDecimal.valueOf(matchQty));
						BigDecimal blockFee = block.feePerShare().multiply(BigDecimal.valueOf(matchQty));

						tradeCostNet = tradeCostNet.add(blockCost).add(blockFee);
						remainingToSell -= matchQty;

						if (matchQty == block.quantity()) {
							inventory.pollFirst();
						} else {
							// Update head
							inventory.pollFirst();
							inventory.addFirst(
									new BuyBlock(block.price(), block.quantity() - matchQty, block.feePerShare()));
						}
					}

					// Sell Proceeds Net = (Price * Qty) - Fee - Tax
					BigDecimal proceeds = price.multiply(BigDecimal.valueOf(q)).subtract(fee)
							.subtract(nz(trade.getTax()));
					realizedProfit = proceeds.subtract(tradeCostNet);
				}

				// 3. Filter by Date Range and Add to Result
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
							price.multiply(BigDecimal.valueOf(q)),
							realizedProfit,
							trade.getTradeDate()));
				}
			}
		}

		// Global sort by date descending
		result.sort(Comparator.comparing(TradeResponse::tradeDate).reversed());

		return result;
	}

}