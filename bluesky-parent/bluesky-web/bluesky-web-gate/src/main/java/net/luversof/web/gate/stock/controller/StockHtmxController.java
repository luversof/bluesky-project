package net.luversof.web.gate.stock.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.Setter;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.openfeign.AccountClient;
import net.luversof.web.gate.stock.openfeign.StockItemClient;
import net.luversof.web.gate.stock.openfeign.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockHtmxController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitClient tradeProfitClient;

	@Setter(onMethod_ = @Autowired)
	private AccountClient accountClient;

	@Setter(onMethod_ = @Autowired)
	private StockItemClient stockItemClient;

	@GetMapping("/calculateProfit")
	public String calculateProfit(TradeProfitRequest request, Model model) {
		// 로그인한 유저의 userId 설정
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute("error", "로그인이 필요합니다.");
			return "stock/htmx/error";
		}
		
		// request에 userId 설정
		request.setUserId(userId);

		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request);

		// Account와 StockItem 이름 정보 조회
		Map<UUID, String> accountNames = tradeProfitList.stream()
				.map(TradeProfit::accountId)
				.filter(id -> id != null)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> accountClient.getAccountById(id)
								.map(Account::name)
								.orElse("알 수 없음")));

		Map<UUID, String> stockItemNames = tradeProfitList.stream()
				.map(TradeProfit::stockItemId)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id)
								.map(StockItem::name)
								.orElse("알 수 없음")));

		// TradeProfit에 이름 정보 추가
		List<TradeProfit> enrichedList = tradeProfitList.stream()
				.map(profit -> new TradeProfit(
						profit.stockItemId(),
						stockItemNames.get(profit.stockItemId()),
						profit.accountId(),
						profit.accountId() != null ? accountNames.get(profit.accountId()) : null,
						profit.totalBuyAmount(),
						profit.averageBuyPrice(),
						profit.totalSellQuantity(),
						profit.averageSellPrice(),
						profit.totalSellAmount(),
						profit.realizedProfit(),
						profit.holdingQuantity(),
						profit.currentPrice(),
						profit.evaluationAmount(),
						profit.evaluationProfit(),
						profit.totalProfit(),
						profit.totalBuyFee(),
						profit.totalSellFee(),
						profit.totalSellTax(),
						profit.totalBuyCost(),
						profit.totalSellProceeds(),
						profit.averageBuyPriceNet(),
						profit.averageSellPriceNet(),
						profit.realizedProfitNet(),
						profit.evaluationProfitNet(),
						profit.totalProfitNet()))
				.toList();

		model.addAttribute("tradeProfitList", enrichedList);
		return "stock/htmx/calculateProfit";
	}

	@GetMapping("/dashboard")
	public String dashboard(TradeProfitRequest request, Model model) {
		// 로그인한 유저의 userId 설정
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute("error", "로그인이 필요합니다.");
			return "stock/htmx/error";
		}
		
		// request에 userId 설정
		request.setUserId(userId);
		
		// calculateProfit와 동일한 로직으로 데이터 조회 및 enrichment
		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request);

		Map<UUID, String> accountNames = tradeProfitList.stream()
				.map(TradeProfit::accountId)
				.filter(id -> id != null)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> accountClient.getAccountById(id)
								.map(Account::name)
								.orElse("알 수 없음")));

		Map<UUID, String> stockItemNames = tradeProfitList.stream()
				.map(TradeProfit::stockItemId)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id)
								.map(StockItem::name)
								.orElse("알 수 없음")));

		List<TradeProfit> enrichedList = tradeProfitList.stream()
				.map(profit -> new TradeProfit(
						profit.stockItemId(),
						stockItemNames.get(profit.stockItemId()),
						profit.accountId(),
						profit.accountId() != null ? accountNames.get(profit.accountId()) : null,
						profit.totalBuyAmount(),
						profit.averageBuyPrice(),
						profit.totalSellQuantity(),
						profit.averageSellPrice(),
						profit.totalSellAmount(),
						profit.realizedProfit(),
						profit.holdingQuantity(),
						profit.currentPrice(),
						profit.evaluationAmount(),
						profit.evaluationProfit(),
						profit.totalProfit(),
						profit.totalBuyFee(),
						profit.totalSellFee(),
						profit.totalSellTax(),
						profit.totalBuyCost(),
						profit.totalSellProceeds(),
						profit.averageBuyPriceNet(),
						profit.averageSellPriceNet(),
						profit.realizedProfitNet(),
						profit.evaluationProfitNet(),
						profit.totalProfitNet()))
				.toList();

		model.addAttribute("tradeProfitList", enrichedList);
		
		// 계좌별 그룹화
		Map<String, List<TradeProfit>> byAccount = enrichedList.stream()
				.filter(tp -> tp.accountName() != null)
				.collect(Collectors.groupingBy(TradeProfit::accountName));
		model.addAttribute("tradeProfitByAccount", byAccount);
		
		// 종목별 그룹화
		Map<String, List<TradeProfit>> byStock = enrichedList.stream()
				.collect(Collectors.groupingBy(TradeProfit::stockItemName));
		model.addAttribute("tradeProfitByStock", byStock);
		
		return "stock/htmx/dashboard";
	}

}
