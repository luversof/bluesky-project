
package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockHtmxController {

	private static final String ERROR_ATTRIBUTE = "error";
	private static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요합니다.";
	private static final String ERROR_VIEW = "stock/htmx/error";
	private static final String UNKNOWN_LABEL = "종목 정보 없음";

	@Autowired
	private TradeProfitClient tradeProfitClient;

	@Autowired
	private AccountClient accountClient;

	@Autowired
	private StockItemClient stockItemClient;

	@Autowired
	private DividendClient dividendClient;

	@GetMapping("/dashboard")
	public String dashboard() {
		return "stock/htmx/dashboard";
	}

	@GetMapping("/summary")
	public String summary(TradeProfitRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) return ERROR_VIEW;
		request.setUserId(userId);

		// 1. 자산/손익 데이터
		List<TradeProfit> profitList = tradeProfitClient.calculateProfit(request.toParams());
		BigDecimal totalAsset = profitList.stream().map(TradeProfit::evaluationAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalRealizedVal = profitList.stream().map(TradeProfit::realizedProfit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalUnrealizedVal = profitList.stream().map(TradeProfit::evaluationProfit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

		// 2. 배당 데이터 (전체 기간 조회라 가정 - 필요시 날짜 필터링)
		// DividendRequest 사용하여 전체 조회
		DividendRequest dividendRequest = new DividendRequest();
		dividendRequest.setUserId(userId);
		List<DividendResponse> dividendList = dividendClient.findDividends(dividendRequest.toParams());
		BigDecimal totalDividendVal = dividendList.stream().map(DividendResponse::price).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

		model.addAttribute("totalAsset", totalAsset);
		model.addAttribute("totalRealizedProfit", totalRealizedVal);
		model.addAttribute("totalUnrealizedProfit", totalUnrealizedVal);
		model.addAttribute("totalDividend", totalDividendVal);

		return "stock/htmx/fragments/summary :: summary";
	}

	@GetMapping("/charts/allocation")
	public String allocationChart(TradeProfitRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) return ERROR_VIEW;
		request.setUserId(userId);

		List<TradeProfit> profitList = getEnrichedTradeProfits(request);
		
		// 종목별 비중 (Top 5 + Others)
		Map<String, BigDecimal> allocation = profitList.stream()
			.filter(p -> p.evaluationAmount() != null)
			.collect(Collectors.toMap(
				p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
				TradeProfit::evaluationAmount,
				BigDecimal::add
			));
		
		model.addAttribute("allocation", allocation);
		
		return "stock/htmx/fragments/charts :: allocation";
	}

	@GetMapping("/charts/dividend")
	public String dividendChart(Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) return ERROR_VIEW;
		
		DividendRequest request = new DividendRequest();
		request.setUserId(userId);
		List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

		// 월별 그룹화 (최근 12개월 or 전체) - 여기서는 전체 월별 합계
		Map<String, BigDecimal> monthly = dividends.stream()
			.filter(d -> d.payDate() != null)
			.collect(Collectors.groupingBy(
				d -> d.payDate().atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
				Collectors.reducing(BigDecimal.ZERO, d -> d.price() != null ? d.price() : BigDecimal.ZERO, BigDecimal::add)
			));
		
		// Map을 Key(년월) 순으로 정렬
		Map<String, BigDecimal> sortedMonthly = new java.util.TreeMap<>(monthly);

		model.addAttribute("monthlyDividends", sortedMonthly);
		return "stock/htmx/fragments/charts :: dividend";
	}
	
	@GetMapping("/portfolio")
	public String portfolio(TradeProfitRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}
		request.setUserId(userId);
		List<TradeProfit> enrichedList = getEnrichedTradeProfits(request);
		model.addAttribute("tradeProfitList", enrichedList);
		return "stock/htmx/fragments/tabs :: portfolio";
	}


	// Helper to get enriched data
	private List<TradeProfit> getEnrichedTradeProfits(TradeProfitRequest request) {
		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request.toParams());

		Map<UUID, String> accountNames = tradeProfitList.stream()
				.map(TradeProfit::accountId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> accountClient.getAccountById(id).map(Account::name).orElse(UNKNOWN_LABEL),
						(a, b) -> a)); // duplicate handling

		Map<UUID, String> stockItemNames = tradeProfitList.stream()
				.map(TradeProfit::stockItemId)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id).map(StockItem::name).orElse(UNKNOWN_LABEL),
						(a, b) -> a)); // duplicate handling

		return tradeProfitList.stream()
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
	}


	@GetMapping("/dividend/list")
	public String dividendList(DividendRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}

		request.setUserId(userId);

		List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

		Map<UUID, String> accountNames = accountClient.getAccountsByUserId(userId).stream()
				.collect(Collectors.toMap(Account::id, Account::name, (left, _) -> left, LinkedHashMap::new));

		// 모든 stockItemId 수집 및 이름 조회
		Map<UUID, String> stockItemNames = dividends.stream()
				.map(DividendResponse::stockItemId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id)
								.map(StockItem::name)
								.orElse(UNKNOWN_LABEL)));

		List<DividendView> viewList = dividends.stream()
				.map(dividend -> {
					String accountName = accountNames.getOrDefault(dividend.accountId(), UNKNOWN_LABEL);
					String stockItemName = Optional.ofNullable(dividend.stockItemName())
							.orElse(Optional.ofNullable(dividend.stockItemId())
									.map(id -> stockItemNames.getOrDefault(id, UNKNOWN_LABEL))
									.orElse(UNKNOWN_LABEL));
					BigDecimal price = Optional.ofNullable(dividend.price()).orElse(BigDecimal.ZERO);
					BigDecimal tax = Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
					return new DividendView(
							dividend.id(),
							dividend.accountId(),
							accountName,
							dividend.stockItemId(),
							stockItemName,
							price,
							tax,
							price.subtract(tax),
							dividend.recordDate(),
							dividend.payDate());
				})
				.toList();

		model.addAttribute("dividendList", viewList);
		return "stock/htmx/fragments/tabs :: dividendHistory";
	}

}
