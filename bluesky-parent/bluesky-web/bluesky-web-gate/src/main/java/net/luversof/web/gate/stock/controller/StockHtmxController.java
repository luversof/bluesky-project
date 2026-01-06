
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

	@GetMapping("/calculateProfit")
	public String calculateProfit(TradeProfitRequest request, Model model) {
		// 로그인한 유저의 userId 설정
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}

		// request에 userId 설정
		request.setUserId(userId);

		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request.toParams());

		// Account와 StockItem 이름 정보 조회
		Map<UUID, String> accountNames = tradeProfitList.stream()
				.map(TradeProfit::accountId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> accountClient.getAccountById(id)
								.map(Account::name)
								.orElse(UNKNOWN_LABEL)));

		Map<UUID, String> stockItemNames = tradeProfitList.stream()
				.map(TradeProfit::stockItemId)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id)
								.map(StockItem::name)
								.orElse(UNKNOWN_LABEL)));

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
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}

		// request에 userId 설정
		request.setUserId(userId);

		// calculateProfit와 동일한 로직으로 데이터 조회 및 enrichment
		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request.toParams());

		Map<UUID, String> accountNames = tradeProfitList.stream()
				.map(TradeProfit::accountId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> accountClient.getAccountById(id)
								.map(Account::name)
								.orElse(UNKNOWN_LABEL)));

		Map<UUID, String> stockItemNames = tradeProfitList.stream()
				.map(TradeProfit::stockItemId)
				.distinct()
				.collect(Collectors.toMap(
						id -> id,
						id -> stockItemClient.getStockItemById(id)
								.map(StockItem::name)
								.orElse(UNKNOWN_LABEL)));

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
		return "stock/htmx/dividendList";
	}

}
