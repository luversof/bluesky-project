package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
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
		// 요청 기준으로 tradeList를 조회한 후 계산 로직 호출
		List<Trade> tradeList = switch (request.getRequestType()) {
			case USER -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.NOT_EXIST_USER_ACCOUNT.throwException();;
				}
				yield request.hasDateRange()
					? tradeService.findByAccountIdInAndTradeDateBetween(accountList.stream().map(Account::getId).toList(), request.startDate(), request.endDate())
					: tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
			}
			case USER_ACCOUNT -> {
				var accountList = accountService.findByIdIn(request.accountIdList());
				if (accountList.isEmpty()) {
					StockErrorCode.NOT_EXIST_USER_ACCOUNT.throwException();;
				}
				
				accountList.stream().forEach(account -> {
					if (!account.getUserId().equals(request.userId())) {
						StockErrorCode.NOT_USER_ACCOUNT.throwException(request.userId(), account.getId());
					}
				});
				
				yield request.hasDateRange()
					? tradeService.findByAccountIdInAndTradeDateBetween(request.accountIdList(), request.startDate(), request.endDate())
					: tradeService.findByAccountIdIn(request.accountIdList());
			}
			case USER_STOCKITEM -> {
				var accountList = accountService.findByUserId(request.userId());
				if (accountList.isEmpty()) {
					StockErrorCode.NOT_EXIST_USER_ACCOUNT.throwException();;
				}
				
				accountList.stream().forEach(x -> {
					if (!x.getUserId().equals(request.userId())) {
						StockErrorCode.NOT_USER_ACCOUNT.throwException();
					}
				});
				
				yield request.hasDateRange()
					? tradeService.findByAccountIdInAndStockItemIdInAndTradeDateBetween(request.accountIdList(), request.stockItemIdList(), request.startDate(), request.endDate())
					: tradeService.findByAccountIdInAndStockItemIdIn(request.accountIdList(), request.stockItemIdList());
			}
			case USER_ACCOUNT_STOCKITEM -> {
					yield Collections.emptyList();
			}
		};
		
		
		tradeList.forEach(t -> System.out.println(t));
		
		return Collections.emptyList();
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
		int totalBuyQuantity = trades.stream().filter(t -> t.getType() == TradeType.BUY).mapToInt(Trade::getQuantity).sum();
		BigDecimal totalBuyAmount = trades.stream()
				.filter(t -> t.getType() == TradeType.BUY)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal averageBuyPrice = totalBuyQuantity > 0 ? 
				totalBuyAmount.divide(BigDecimal.valueOf(totalBuyQuantity), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		
		// 매도 관련 계산 (실현 손익)
		int totalSellQuantity = trades.stream().filter(t -> t.getType() == TradeType.SELL).mapToInt(Trade::getQuantity).sum();
		BigDecimal totalSellAmount = trades.stream()
				.filter(t -> t.getType() == TradeType.SELL)
				.map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal averageSellPrice = totalSellQuantity > 0 ? 
				totalSellAmount.divide(BigDecimal.valueOf(totalSellQuantity), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		BigDecimal realizedProfit = totalSellAmount.subtract(totalBuyAmount);
		
		// 보유 관련 계산 (미실현 손익)
		int holdingQuantity = totalBuyQuantity - totalSellQuantity;
		BigDecimal currentPrice = stockPriceService.getCurrentPrice(stockItemId);
		BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));
		BigDecimal evaluationProfit = evaluationAmount.subtract(averageBuyPrice.multiply(BigDecimal.valueOf(holdingQuantity)));
		
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
}