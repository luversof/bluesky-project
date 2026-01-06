
package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

	private record AnalyticsRow(String key, BigDecimal value) {
	}

	private record ChartDataset(String label, List<BigDecimal> data, String backgroundColor, String borderColor,
			Integer borderWidth) {
	}

	@GetMapping("/dashboard")
	public String dashboard(@RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
		if (hxRequest) {
			return "stock/htmx/dashboard :: dashboardContent";
		}
		return "stock/htmx/dashboard";
	}

	@GetMapping("/analytics/view")
	public String analyticsView(Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId != null) {
			model.addAttribute("accounts", accountClient.getAccountsByUserId(userId));
		}
		return "stock/htmx/analytics :: container";
	}

	@GetMapping("/analytics/data")
	public String analyticsData(
			@RequestParam(defaultValue = "PROFIT") String type, // PROFIT | DIVIDEND
			@RequestParam(defaultValue = "TOTAL") String timeScale, // TOTAL | MONTHLY | YEARLY
			@RequestParam(defaultValue = "STOCK") String groupBy, // STOCK | ACCOUNT
			@RequestParam(defaultValue = "2025") int year,
			@RequestParam(required = false) UUID accountId,
			Model model) {

		UUID userId = UserUtil.getUserId();
		if (userId == null)
			return ERROR_VIEW;

		List<AnalyticsRow> rows = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		List<ChartDataset> datasets = new ArrayList<>();
		String keyLabel = "";
		String valueLabel = "금액 (원)";
		String chartType = "bar";
		String chartTitle = "";

		List<String> palette = List.of("#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f", "#edc948", "#b07aa1",
				"#ff9da7", "#9c755f", "#bab0ac");

		// --- 1. PROFIT LOGIC ---
		if ("PROFIT".equals(type)) {
			TradeProfitRequest request = new TradeProfitRequest();
			request.setUserId(userId);
			if (accountId != null)
				request.setAccountIdList(List.of(accountId));

			if ("MONTHLY".equals(timeScale)) {
				// Monthly Profit Trend (Realized Profit Only)
				chartTitle = year + "년 월별 손익 추이 (실현 손익)";
				keyLabel = "월";
				chartType = "line";
				for (int i = 1; i <= 12; i++)
					labels.add(i + "월");

				// 1. Fetch Parallel Data for each month
				Map<Integer, List<TradeProfit>> monthData = java.util.stream.IntStream.rangeClosed(1, 12).parallel()
						.boxed()
						.collect(Collectors.toMap(
								m -> m,
								m -> {
									TradeProfitRequest subReq = new TradeProfitRequest();
									subReq.setUserId(userId);
									if (accountId != null)
										subReq.setAccountIdList(List.of(accountId));
									subReq.setStartDate(LocalDate.of(year, m, 1)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									subReq.setEndDate(LocalDate.of(year, m, 1).plusMonths(1).minusDays(1)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									return getEnrichedTradeProfits(subReq);
								}));

				// 2. Identify Top Series (by Yearly Total Realized Profit)
				// We need to aggregate all months to find top stocks/accounts
				Map<String, BigDecimal> seriesTotals = new java.util.HashMap<>();
				monthData.values().stream().flatMap(List::stream).forEach(p -> {
					String name = "ACCOUNT".equals(groupBy) ? (p.accountName() != null ? p.accountName() : "Unknown")
							: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
					seriesTotals.merge(name, p.realizedProfit(), BigDecimal::add); // Use Realized Profit
				});

				List<String> topSeries = seriesTotals.entrySet().stream()
						.sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Descending
						.limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// 3. Build Datasets
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> dataPoints = new ArrayList<>();
					for (int m = 1; m <= 12; m++) {
						List<TradeProfit> profitList = monthData.get(m);
						BigDecimal sum = profitList.stream()
								.filter(p -> {
									String name = "ACCOUNT".equals(groupBy)
											? (p.accountName() != null ? p.accountName() : "Unknown")
											: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
									return series.equals(name);
								})
								.map(TradeProfit::realizedProfit)
								.reduce(BigDecimal.ZERO, BigDecimal::add);
						dataPoints.add(sum);
					}
					String color = palette.get(colorIdx++ % palette.size());
					datasets.add(new ChartDataset(series, dataPoints, color, color, 2));
				}

				// 4. Build Table Rows (Monthly Totals)
				for (int i = 0; i < labels.size(); i++) {
					final int m = i + 1;
					BigDecimal total = monthData.get(m).stream()
							.map(TradeProfit::realizedProfit)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					rows.add(new AnalyticsRow(labels.get(i), total));
				}

			} else if ("YEARLY".equals(timeScale)) {
				// Yearly Profit Trend
				chartTitle = "연도별 손익 추이 (실현 손익, 최근 5년)";
				keyLabel = "연도";
				chartType = "line";
				for (int i = year - 4; i <= year; i++)
					labels.add(String.valueOf(i));

				Map<Integer, List<TradeProfit>> yearData = java.util.stream.IntStream.rangeClosed(year - 4, year)
						.parallel()
						.boxed()
						.collect(Collectors.toMap(
								y -> y,
								y -> {
									TradeProfitRequest subReq = new TradeProfitRequest();
									subReq.setUserId(userId);
									if (accountId != null)
										subReq.setAccountIdList(List.of(accountId));
									subReq.setStartDate(LocalDate.of(y, 1, 1)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									subReq.setEndDate(LocalDate.of(y, 12, 31)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									return getEnrichedTradeProfits(subReq);
								}));

				// Top Series
				Map<String, BigDecimal> seriesTotals = new java.util.HashMap<>();
				yearData.values().stream().flatMap(List::stream).forEach(p -> {
					String name = "ACCOUNT".equals(groupBy) ? (p.accountName() != null ? p.accountName() : "Unknown")
							: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
					seriesTotals.merge(name, p.realizedProfit(), BigDecimal::add);
				});

				List<String> topSeries = seriesTotals.entrySet().stream()
						.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
						.limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// Datasets
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> dataPoints = new ArrayList<>();
					for (int y = year - 4; y <= year; y++) {
						BigDecimal sum = yearData.get(y).stream()
								.filter(p -> {
									String name = "ACCOUNT".equals(groupBy)
											? (p.accountName() != null ? p.accountName() : "Unknown")
											: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
									return series.equals(name);
								})
								.map(TradeProfit::realizedProfit)
								.reduce(BigDecimal.ZERO, BigDecimal::add);
						dataPoints.add(sum);
					}
					String color = palette.get(colorIdx++ % palette.size());
					datasets.add(new ChartDataset(series, dataPoints, color, color, 2));
				}

				// Table Rows
				for (int i = 0; i < labels.size(); i++) {
					final int y = Integer.parseInt(labels.get(i));
					BigDecimal total = yearData.get(y).stream()
							.map(TradeProfit::realizedProfit)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					rows.add(new AnalyticsRow(labels.get(i), total));
				}

			} else {
				// TOTAL: Snapshot (Default)
				List<TradeProfit> profits = getEnrichedTradeProfits(request);
				chartTitle = "매매/보유 손익 (" + (groupBy.equals("ACCOUNT") ? "계좌별" : "종목별") + ")";

				if ("ACCOUNT".equals(groupBy)) {
					keyLabel = "계좌명";
					rows = profits.stream()
							.collect(Collectors.groupingBy(
									p -> p.accountName() != null ? p.accountName() : "Unknown",
									Collectors.reducing(BigDecimal.ZERO, TradeProfit::totalProfit, BigDecimal::add)))
							.entrySet().stream()
							.map(e -> new AnalyticsRow(e.getKey(), e.getValue()))
							.sorted((a, b) -> b.value().compareTo(a.value()))
							.toList();
				} else { // STOCK
					keyLabel = "종목명";
					rows = profits.stream()
							.collect(Collectors.groupingBy(
									p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
									Collectors.reducing(BigDecimal.ZERO, TradeProfit::totalProfit, BigDecimal::add)))
							.entrySet().stream()
							.map(e -> new AnalyticsRow(e.getKey(), e.getValue()))
							.sorted((a, b) -> b.value().compareTo(a.value()))
							.limit(20)
							.toList();
				}

				// Build Single Dataset for Profit
				// labels = rows.stream().map(AnalyticsRow::key).toList();
				// List<BigDecimal> data = rows.stream().map(AnalyticsRow::value).toList();
				// datasets.add(new ChartDataset("손익", data, null, null, null));
			}

			// --- 2. DIVIDEND LOGIC ---
		} else {
			DividendRequest request = new DividendRequest();
			request.setUserId(userId);
			if (accountId != null)
				request.setAccountIdList(List.of(accountId));

			if ("YEARLY".equals(timeScale)) {
				request.setStartDate(
						LocalDate.of(year - 4, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				request.setEndDate(
						LocalDate.of(year, 12, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				chartTitle = "연도별 배당 추이 (최근 5년)";
				keyLabel = "연도";
				chartType = "line";

				// Generate Labels (Year-4 to Year)
				for (int i = year - 4; i <= year; i++)
					labels.add(String.valueOf(i));

			} else if ("MONTHLY".equals(timeScale)) {
				request.setStartDate(
						LocalDate.of(year, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				request.setEndDate(
						LocalDate.of(year, 12, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				chartTitle = year + "년 월별 배당 내역";
				keyLabel = "월";
				chartType = "line";

				// Generate Labels (1월 to 12월)
				for (int i = 1; i <= 12; i++)
					labels.add(i + "월");
			} else {
				// TOTAL
				request.setStartDate(
						LocalDate.of(year, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				request.setEndDate(
						LocalDate.of(year, 12, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				chartTitle = year + "년 배당 총합";
				keyLabel = groupBy.equals("ACCOUNT") ? "계좌명" : "종목명";
			}

			List<DividendResponse> dividends = Optional.ofNullable(dividendClient.findDividends(request.toParams()))
					.orElse(new ArrayList<>());

			// Enrich Names if not present
			final Map<UUID, String> stockNames = new java.util.HashMap<>();
			final Map<UUID, String> accountNames = new java.util.HashMap<>();

			if (dividends.stream().anyMatch(d -> d.stockItemName() == null)) {
				dividends.stream().map(DividendResponse::stockItemId).filter(Objects::nonNull).distinct()
						.forEach(id -> stockNames.put(id,
								stockItemClient.getStockItemById(id).map(StockItem::name).orElse(UNKNOWN_LABEL)));
			}
			if (groupBy.equals("ACCOUNT")) {
				accountClient.getAccountsByUserId(userId).forEach(a -> accountNames.put(a.id(), a.name()));
			}

			// Helper to get Series Name
			java.util.function.Function<DividendResponse, String> getSeriesName = d -> {
				if ("ACCOUNT".equals(groupBy))
					return accountNames.getOrDefault(d.accountId(), "Unknown");
				return d.stockItemName() != null ? d.stockItemName()
						: stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
			};

			// Build Chart Data
			if (!"TOTAL".equals(timeScale)) {
				// Multi-Series Logic (Trend)
				// 1. Group by Series (Stock/Account)
				Map<String, List<DividendResponse>> bySeries = dividends.stream()
						.filter(d -> d.payDate() != null)
						.collect(Collectors.groupingBy(getSeriesName));

				// 2. Identify Top 5 Series by Total Amount
				List<String> topSeries = bySeries.entrySet().stream()
						.sorted((a, b) -> {
							BigDecimal sumA = a.getValue().stream()
									.map(d -> d.price() != null ? d.price() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
							BigDecimal sumB = b.getValue().stream()
									.map(d -> d.price() != null ? d.price() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
							return sumB.compareTo(sumA);
						})
						.limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// 3. Create Datasets for Top Series
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> dataPoints = new ArrayList<>();
					List<DividendResponse> seriesData = bySeries.get(series);

					for (String label : labels) {
						BigDecimal pointSum = BigDecimal.ZERO;
						if ("YEARLY".equals(timeScale)) {
							// Label is Year String "2025"
							int y = Integer.parseInt(label);
							pointSum = seriesData.stream()
									.filter(d -> d.payDate().atZone(java.time.ZoneId.systemDefault()).getYear() == y)
									.map(d -> d.price() != null ? d.price() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
						} else {
							// Label is Month String "1월"
							int m = Integer.parseInt(label.replace("월", ""));
							pointSum = seriesData.stream()
									.filter(d -> d.payDate().atZone(java.time.ZoneId.systemDefault())
											.getMonthValue() == m)
									.map(d -> d.price() != null ? d.price() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
						}
						dataPoints.add(pointSum);
					}

					String color = palette.get(colorIdx++ % palette.size());
					datasets.add(new ChartDataset(series, dataPoints, color, color, 2));
				}

				// Populate Table Rows (For Table View, showing Aggregate Total per Period)
				// e.g. "1월", Total Amount
				for (int i = 0; i < labels.size(); i++) {
					String label = labels.get(i);
					BigDecimal total = BigDecimal.ZERO;
					for (ChartDataset ds : datasets) {
						total = total.add(ds.data().get(i));
					}
					rows.add(new AnalyticsRow(label, total));
				}

			} else {
				// TOTAL: Snapshot Bar Chart
				Map<String, BigDecimal> aggregated = dividends.stream()
						.collect(Collectors.groupingBy(
								getSeriesName,
								Collectors.reducing(BigDecimal.ZERO,
										d -> d.price() != null ? d.price() : BigDecimal.ZERO, BigDecimal::add)));

				rows = aggregated.entrySet().stream()
						.map(e -> new AnalyticsRow(e.getKey(), e.getValue()))
						.sorted((a, b) -> b.value().compareTo(a.value()))
						.limit(20)
						.toList();

				labels = rows.stream().map(AnalyticsRow::key).toList();
				List<BigDecimal> data = rows.stream().map(AnalyticsRow::value).toList();
				datasets.add(new ChartDataset("배당금", data, null, null, null));
			}
		}

		BigDecimal totalValue = rows.stream().map(AnalyticsRow::value).reduce(BigDecimal.ZERO, BigDecimal::add);

		model.addAttribute("chartTitle", chartTitle);
		model.addAttribute("keyLabel", keyLabel);
		model.addAttribute("valueLabel", valueLabel);
		model.addAttribute("tableData", rows);
		model.addAttribute("totalValue", totalValue);
		model.addAttribute("chartType", chartType);
		model.addAttribute("chartLabels", labels);
		model.addAttribute("chartDatasets", datasets);

		return "stock/htmx/analytics :: data-content";
	}

	@GetMapping("/summary")
	public String summary(TradeProfitRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null)
			return ERROR_VIEW;
		request.setUserId(userId);

		// 1. 자산/손익 데이터 (Enriched to get names for Top Gainers)
		List<TradeProfit> profitList = getEnrichedTradeProfits(request);

		BigDecimal totalAsset = profitList.stream().map(TradeProfit::evaluationAmount).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalRealizedVal = profitList.stream().map(TradeProfit::realizedProfit).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalUnrealizedVal = profitList.stream().map(TradeProfit::evaluationProfit).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Win Rate (Percentage of items with positive total profit)
		long winCount = profitList.stream()
				.filter(p -> p.totalProfit() != null && p.totalProfit().compareTo(BigDecimal.ZERO) > 0)
				.count();
		double winRate = profitList.isEmpty() ? 0.0 : (double) winCount / profitList.size() * 100;

		// Top 3 Gainers
		List<TradeProfit> topGainers = profitList.stream()
				.filter(p -> p.totalProfit() != null)
				.sorted((p1, p2) -> p2.totalProfit().compareTo(p1.totalProfit()))
				.limit(3)
				.toList();

		// 2. 배당 데이터 (전체 기간 조회라 가정 - 필요시 날짜 필터링)
		// DividendRequest 사용하여 전체 조회
		DividendRequest dividendRequest = new DividendRequest();
		dividendRequest.setUserId(userId);
		List<DividendResponse> dividendList = dividendClient.findDividends(dividendRequest.toParams());
		BigDecimal totalDividendVal = dividendList.stream().map(DividendResponse::price).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		model.addAttribute("totalAsset", totalAsset);
		model.addAttribute("totalRealizedProfit", totalRealizedVal);
		model.addAttribute("totalUnrealizedProfit", totalUnrealizedVal);
		model.addAttribute("totalDividend", totalDividendVal);
		model.addAttribute("winRate", winRate);
		model.addAttribute("topGainers", topGainers);

		return "stock/htmx/fragments/summary :: summary";
	}

	@GetMapping("/charts/allocation")
	public String allocationChart(TradeProfitRequest request, Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null)
			return ERROR_VIEW;
		request.setUserId(userId);

		List<TradeProfit> profitList = getEnrichedTradeProfits(request);

		// 종목별 비중 (Top 5 + Others)
		Map<String, BigDecimal> allocation = profitList.stream()
				.filter(p -> p.evaluationAmount() != null)
				.collect(Collectors.toMap(
						p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
						TradeProfit::evaluationAmount,
						BigDecimal::add));

		model.addAttribute("allocation", allocation);

		return "stock/htmx/fragments/charts :: allocation";
	}

	@GetMapping("/charts/dividend")
	public String dividendChart(Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null)
			return ERROR_VIEW;

		DividendRequest request = new DividendRequest();
		request.setUserId(userId);
		List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

		// 월별 그룹화 (최근 12개월 or 전체) - 여기서는 전체 월별 합계
		Map<String, BigDecimal> monthly = dividends.stream()
				.filter(d -> d.payDate() != null)
				.collect(Collectors.groupingBy(
						d -> d.payDate().atZone(java.time.ZoneId.systemDefault())
								.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
						Collectors.reducing(BigDecimal.ZERO, d -> d.price() != null ? d.price() : BigDecimal.ZERO,
								BigDecimal::add)));

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
