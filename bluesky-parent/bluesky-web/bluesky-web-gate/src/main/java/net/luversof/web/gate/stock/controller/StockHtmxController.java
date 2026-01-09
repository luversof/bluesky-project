
package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;

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
	private TradeClient tradeClient;

	@Autowired
	private AccountClient accountClient;

	@Autowired
	private StockItemClient stockItemClient;

	@Autowired
	private DividendClient dividendClient;

	private record AnalyticsRow(String key, String subKey, BigDecimal value1, BigDecimal value2, BigDecimal value3,
			BigDecimal value4) {
	}

	private record ChartDataset(String label, List<BigDecimal> data, String backgroundColor, String borderColor,
			Integer borderWidth, List<Integer> borderDash) {
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

		int currentYear = LocalDate.now().getYear();
		List<Integer> years = new ArrayList<>();
		for (int i = currentYear + 1; i >= 2015; i--) {
			years.add(i);
		}
		model.addAttribute("years", years);
		model.addAttribute("currentYear", currentYear);
		model.addAttribute("currentMonth", LocalDate.now().getMonthValue());

		return "stock/htmx/analytics :: container";
	}

	@GetMapping("/analytics/data")
	public String analyticsData(
			@RequestParam(defaultValue = "PROFIT") String type, // PROFIT | DIVIDEND
			@RequestParam(defaultValue = "TOTAL") String timeScale, // TOTAL | MONTHLY | YEARLY
			@RequestParam(defaultValue = "STOCK") String groupBy, // STOCK | ACCOUNT
			@RequestParam(defaultValue = "2025") int year,
			@RequestParam(defaultValue = "1") int month,
			@RequestParam(required = false) UUID accountId,
			Model model) {

		UUID userId = UserUtil.getUserId();
		if (userId == null)
			return ERROR_VIEW;

		List<AnalyticsRow> rows = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		List<ChartDataset> datasets = new ArrayList<>();
		// Common Variables
		String chartTitle = "";
		String keyLabel = "";
		String subKeyLabel = null;
		String value1Label = "거래손익"; // Default
		String value2Label = "평가손익"; // Default
		String value3Label = null;
		String value4Label = null;
		String totalLabel = "합계";

		String chartType = "bar";

		List<String> palette = List.of("#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f", "#edc948", "#b07aa1",
				"#ff9da7", "#9c755f", "#bab0ac");

		// --- 1. PROFIT LOGIC ---
		if ("PROFIT".equals(type)) {
			value1Label = "실현 손익";
			value2Label = "보유 손익";
			value3Label = "평가 금액"; // New Label
			totalLabel = null; // Hide Total for Profit view

			TradeProfitRequest request = new TradeProfitRequest();
			request.setUserId(userId);
			if (accountId != null)
				request.setAccountIdList(List.of(accountId));

			if ("MONTHLY".equals(timeScale)) {
				// Monthly Profit Trend
				chartTitle = year + "년 월별 손익 추이";
				keyLabel = "월";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";

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
									subReq.setEndDate(LocalDate.of(year, m, 1).plusMonths(1)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									return getEnrichedTradeProfits(subReq);
								}));

				// 2. Identify Top Series (by Total Net Profit sum)
				// Note: For Trend, we only consider Actual Realized Profit for sorting/summing,
				// as Unrealized Profit is a current snapshot and meaningless to sum over
				// months.
				Map<String, BigDecimal> seriesTotals = new HashMap<>();
				monthData.values().stream().flatMap(List::stream).forEach(p -> {
					String name = "ACCOUNT".equals(groupBy) ? (p.accountName() != null ? p.accountName() : "Unknown")
							: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
					BigDecimal total = (p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO);
					seriesTotals.merge(name, total, BigDecimal::add);
				});

				List<String> topSeries = seriesTotals.entrySet().stream()
						.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
						.limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// 3. Build Datasets
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> realizedPoints = new ArrayList<>();
					// List<BigDecimal> unrealizedPoints = new ArrayList<>();

					for (int m = 1; m <= 12; m++) {
						List<TradeProfit> profitList = monthData.get(m);
						BigDecimal realizedSum = BigDecimal.ZERO;
						// BigDecimal unrealizedSum = BigDecimal.ZERO;

						for (TradeProfit p : profitList) {
							String name = "ACCOUNT".equals(groupBy)
									? (p.accountName() != null ? p.accountName() : "Unknown")
									: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
							if (series.equals(name)) {
								realizedSum = realizedSum
										.add(p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO);
								// unrealizedSum = unrealizedSum.add(p.evaluationProfitNet() != null ?
								// p.evaluationProfitNet() : BigDecimal.ZERO);
							}
						}
						realizedPoints.add(realizedSum);
						// unrealizedPoints.add(unrealizedSum);
					}
					String baseColor = palette.get(colorIdx++ % palette.size());
					datasets.add(
							new ChartDataset(series + " (실현)", realizedPoints, baseColor, baseColor, 2, List.of()));
					// datasets.add(new ChartDataset(series + " (보유)", unrealizedPoints, baseColor,
					// baseColor, 2, List.of(5, 5)));
				}

				// 4. Build Table Rows
				for (int m = 1; m <= 12; m++) {
					String timeLabel = labels.get(m - 1);
					List<TradeProfit> profitList = monthData.get(m);
					Map<String, BigDecimal> rMap = new HashMap<>();
					// Map<String, BigDecimal> uMap = new HashMap<>();
					// Map<String, BigDecimal> eMap = new HashMap<>();

					for (TradeProfit p : profitList) {
						String name = "ACCOUNT".equals(groupBy)
								? (p.accountName() != null ? p.accountName() : "Unknown")
								: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
						rMap.merge(name, p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO,
								BigDecimal::add);
						// uMap.merge(name, p.evaluationProfitNet() != null ? p.evaluationProfitNet() :
						// BigDecimal.ZERO, BigDecimal::add);
						// eMap.merge(name, p.evaluationAmount() != null ? p.evaluationAmount() :
						// BigDecimal.ZERO, BigDecimal::add);
					}

					java.util.Set<String> allKeys = new java.util.HashSet<>();
					allKeys.addAll(rMap.keySet());
					// allKeys.addAll(uMap.keySet());

					for (String name : allKeys) {
						BigDecimal r = rMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal u = uMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal e = eMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal total = r.add(u);
						if (r.abs().compareTo(BigDecimal.ZERO) > 0) {
							rows.add(new AnalyticsRow(timeLabel, name, r, BigDecimal.ZERO, BigDecimal.ZERO, r));
						}
					}
				}

			} else if ("YEARLY".equals(timeScale)) {
				// Yearly Profit Trend
				chartTitle = "연도별 손익 추이 (실현/보유 손익, 최근 5년)";
				keyLabel = "연도";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";
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
									subReq.setEndDate(LocalDate.of(y, 12, 31).plusDays(1)
											.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
									return getEnrichedTradeProfits(subReq);
								}));

				// Top Series
				// For Years: Sort by Realized Profit Only
				Map<String, BigDecimal> seriesTotals = new HashMap<>();
				yearData.values().stream().flatMap(List::stream).forEach(p -> {
					String name = "ACCOUNT".equals(groupBy) ? (p.accountName() != null ? p.accountName() : "Unknown")
							: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
					BigDecimal total = (p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO);
					seriesTotals.merge(name, total, BigDecimal::add);
				});

				List<String> topSeries = seriesTotals.entrySet().stream()
						.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
						.limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// Datasets
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> realizedPoints = new ArrayList<>();
					// List<BigDecimal> unrealizedPoints = new ArrayList<>();

					for (int y = year - 4; y <= year; y++) {
						List<TradeProfit> profitList = yearData.get(y);
						BigDecimal realizedSum = BigDecimal.ZERO;
						// BigDecimal unrealizedSum = BigDecimal.ZERO;

						for (TradeProfit p : profitList) {
							String name = "ACCOUNT".equals(groupBy)
									? (p.accountName() != null ? p.accountName() : "Unknown")
									: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
							if (series.equals(name)) {
								realizedSum = realizedSum
										.add(p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO);
								// unrealizedSum = unrealizedSum.add(p.evaluationProfitNet() != null ?
								// p.evaluationProfitNet() : BigDecimal.ZERO);
							}
						}
						realizedPoints.add(realizedSum);
						// unrealizedPoints.add(unrealizedSum);
					}
					String baseColor = palette.get(colorIdx++ % palette.size());
					datasets.add(
							new ChartDataset(series + " (실현)", realizedPoints, baseColor, baseColor, 2, List.of()));
					// datasets.add(new ChartDataset(series + " (보유)", unrealizedPoints, baseColor,
					// baseColor, 2, List.of(5, 5)));
				}

				// Table Rows
				for (int i = year - 4; i <= year; i++) {
					String timeLabel = String.valueOf(i);
					List<TradeProfit> profitList = yearData.get(i);
					Map<String, BigDecimal> rMap = new HashMap<>();
					// Map<String, BigDecimal> uMap = new HashMap<>();
					// Map<String, BigDecimal> eMap = new HashMap<>();

					for (TradeProfit p : profitList) {
						String name = "ACCOUNT".equals(groupBy)
								? (p.accountName() != null ? p.accountName() : "Unknown")
								: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
						rMap.merge(name, p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO,
								BigDecimal::add);
						// uMap.merge(name, p.evaluationProfitNet() != null ? p.evaluationProfitNet() :
						// BigDecimal.ZERO, BigDecimal::add);
						// eMap.merge(name, p.evaluationAmount() != null ? p.evaluationAmount() :
						// BigDecimal.ZERO, BigDecimal::add);
					}

					java.util.Set<String> allKeys = new java.util.HashSet<>();
					allKeys.addAll(rMap.keySet());
					// allKeys.addAll(uMap.keySet());

					for (String name : allKeys) {
						BigDecimal r = rMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal u = uMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal e = eMap.getOrDefault(name, BigDecimal.ZERO);
						// BigDecimal total = r.add(u);
						if (r.abs().compareTo(BigDecimal.ZERO) > 0) {
							rows.add(new AnalyticsRow(timeLabel, name, r, BigDecimal.ZERO, BigDecimal.ZERO, r));
						}
					}
				}

			} else {
				// TOTAL: Snapshot (Default)
				List<TradeProfit> profits = getEnrichedTradeProfits(request);
				chartTitle = "매매/보유 손익 (" + (groupBy.equals("ACCOUNT") ? "계좌별" : "종목별") + ")";
				keyLabel = "TOTAL";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";

				Map<String, BigDecimal> realizedMap = new HashMap<>();
				Map<String, BigDecimal> unrealizedMap = new HashMap<>();
				Map<String, BigDecimal> evaluationAmountMap = new HashMap<>();

				profits.forEach(p -> {
					String name = "ACCOUNT".equals(groupBy) ? (p.accountName() != null ? p.accountName() : "Unknown")
							: (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
					realizedMap.merge(name, p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO,
							BigDecimal::add);
					unrealizedMap.merge(name,
							p.evaluationProfitNet() != null ? p.evaluationProfitNet() : BigDecimal.ZERO,
							BigDecimal::add);

					BigDecimal evalAmt = BigDecimal.ZERO;
					if (p.currentPrice() != null) {
						// Assuming currentPrice is available in TradeProfit.
						// Wait, TradeProfit record definition needed?
						// It matches TradeProfit domain.
						// If evaluationAmount is already there, use it.
						// If not, p.holdingQuantity() * p.currentPrice()
						// Let's use evaluationAmount() if available, assuming it is total evaluation
						// amt.
						// p.evaluationAmount() typically exists in such DTOs.
						// Checking TradeProfit domain might be safer if not sure.
						// But for now let's assume evaluationAmount() exists or calculate.
						if (p.evaluationAmount() != null) {
							evalAmt = p.evaluationAmount();
						}
					}
					evaluationAmountMap.merge(name, evalAmt, BigDecimal::add);
				});

				// Collect all keys to ensure we don't miss any negative profit items
				java.util.Set<String> allKeys = new java.util.HashSet<>();
				allKeys.addAll(realizedMap.keySet());
				allKeys.addAll(unrealizedMap.keySet());

				rows.addAll(allKeys.stream()
						.map(name -> {
							BigDecimal r = realizedMap.getOrDefault(name, BigDecimal.ZERO);
							BigDecimal u = unrealizedMap.getOrDefault(name, BigDecimal.ZERO);
							BigDecimal e = evaluationAmountMap.getOrDefault(name, BigDecimal.ZERO);
							return new AnalyticsRow("전체", name, r, u, e, null);
						})
						.sorted((a, b) -> b.value1().compareTo(a.value1())) // Sort by Realized Profit (value1)
						// .limit(20)
						.toList());

				labels = rows.stream().map(AnalyticsRow::subKey).toList();
				List<BigDecimal> rData = rows.stream().map(AnalyticsRow::value1).toList();
				List<BigDecimal> uData = rows.stream().map(AnalyticsRow::value2).toList();

				datasets.add(new ChartDataset("실현 손익", rData, "#4e79a7", "#4e79a7", 1, List.of()));
				datasets.add(new ChartDataset("보유 손익", uData, "#f28e2b", "#f28e2b", 1, List.of()));
			}

			// --- 2. DIVIDEND LOGIC ---
		} else {
			value1Label = "배당금(세전)"; // value1
			value2Label = "배당금(세후)"; // value2 (Net Payment)
			value3Label = "세금"; // value3 (Gross - Net)
			value4Label = "과세금액"; // value4 (quantity * taxPerShare)

			totalLabel = null;

			DividendRequest request = new DividendRequest();
			request.setUserId(userId);
			if (accountId != null)
				request.setAccountIdList(List.of(accountId));

			if ("YEARLY".equals(timeScale)) {
				// Show Monthly stats for the specific YEAR
				// Start: year-01-01, End: year-12-31 (+1 day)
				request.setStartDate(
						LocalDate.of(year, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				request.setEndDate(
						LocalDate.of(year, 12, 31).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
								.toInstant());
				chartTitle = year + "년 월별 배당 (전체)";
				keyLabel = "월";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";
				chartType = "line";

				// Generate Labels (1월 to 12월)
				for (int i = 1; i <= 12; i++)
					labels.add(i + "월");

			} else if ("MONTHLY".equals(timeScale)) {
				// Show Daily stats for the specific MONTH
				LocalDate start = LocalDate.of(year, month, 1);
				LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

				request.setStartDate(start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
				request.setEndDate(end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

				chartTitle = year + "년 " + month + "월 일별 배당";
				keyLabel = "일";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";
				chartType = "line";

				// Generate Labels (1 to End)
				for (int i = 1; i <= start.lengthOfMonth(); i++)
					labels.add(i + "일");
			} else {
				// TOTAL
				keyLabel = "전체";
				chartTitle = "누적 배당 총합 (전체)";
				subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌명" : "종목명";
			}

			List<DividendResponse> dividends = Optional.ofNullable(dividendClient.findDividends(request.toParams()))
					.orElse(new ArrayList<>());

			// Resolve Names
			final Map<UUID, String> stockNames = new HashMap<>();

			List<Account> accounts = accountClient.getAccountsByUserId(userId);
			final Map<UUID, String> accountNames = accounts.stream()
					.collect(Collectors.toMap(Account::id, Account::name, (left, _) -> left, LinkedHashMap::new));
			final Map<UUID, Boolean> taxDeferredMap = accounts.stream().collect(Collectors.toMap(Account::id,
					a -> a.jsonConfig() != null && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
					(l, _) -> l));

			if (dividends.stream().anyMatch(d -> d.stockItemName() == null)) {
				dividends.stream().map(DividendResponse::stockItemId).filter(Objects::nonNull).distinct()
						.forEach(id -> stockNames.put(id,
								stockItemClient.getStockItemById(id).map(StockItem::name).orElse(UNKNOWN_LABEL)));
			}

			java.util.function.Function<DividendResponse, String> getSeriesName = d -> {
				if ("ACCOUNT".equals(groupBy))
					return accountNames.getOrDefault(d.accountId(), "Unknown");
				return d.stockItemName() != null ? d.stockItemName()
						: stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
			};

			// Build Chart Data
			if (!"TOTAL".equals(timeScale)) {
				Map<String, List<DividendResponse>> bySeries = dividends.stream()
						.filter(d -> d.payDate() != null)
						.collect(Collectors.groupingBy(getSeriesName));

				Map<String, BigDecimal> seriesTotals = new HashMap<>();
				bySeries.forEach((name, list) -> {
					BigDecimal sum = list.stream()
							.map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					seriesTotals.put(name, sum);
				});

				// REMOVED LIMIT lines
				List<String> topSeries = seriesTotals.entrySet().stream()
						.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
						// .limit(5)
						.map(Map.Entry::getKey)
						.toList();

				// Chart Datasets
				int colorIdx = 0;
				for (String series : topSeries) {
					List<BigDecimal> dataPoints = new ArrayList<>();
					List<DividendResponse> seriesData = bySeries.get(series);

					for (String label : labels) {
						BigDecimal pointSum = BigDecimal.ZERO;
						if ("YEARLY".equals(timeScale)) {
							// Check Month
							int m = Integer.parseInt(label.replace("월", ""));
							pointSum = seriesData.stream()
									.filter(d -> d.payDate().atZone(java.time.ZoneId.systemDefault()).getMonthValue() == m)
									.map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
						} else {
							// Check Day
							int dVal = Integer.parseInt(label.replace("일", ""));
							pointSum = seriesData.stream()
									.filter(d -> d.payDate().atZone(java.time.ZoneId.systemDefault())
											.getDayOfMonth() == dVal)
									.map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
									.reduce(BigDecimal.ZERO, BigDecimal::add);
						}
						dataPoints.add(pointSum);
					}
					String color = palette.get(colorIdx++ % palette.size());
					datasets.add(new ChartDataset(series, dataPoints, color, color, 2, List.of()));
				}

				// Table Rows
				for (int i = 0; i < labels.size(); i++) {
					String timeLabel = labels.get(i);
					Map<String, BigDecimal> periodGrossMap = new HashMap<>();
					Map<String, BigDecimal> periodTaxMap = new HashMap<>();
					Map<String, BigDecimal> periodTaxableMap = new HashMap<>();

					for (DividendResponse d : dividends) {
						if (d.payDate() == null)
							continue;
						boolean match = false;
						if ("YEARLY".equals(timeScale)) {
							int m = Integer.parseInt(timeLabel.replace("월", ""));
							if (d.payDate().atZone(java.time.ZoneId.systemDefault()).getMonthValue() == m)
								match = true;
						} else {
							int dVal = Integer.parseInt(timeLabel.replace("일", ""));
							if (d.payDate().atZone(java.time.ZoneId.systemDefault()).getDayOfMonth() == dVal)
								match = true;
						}
						if (match) {
							String sName = getSeriesName.apply(d);
							periodGrossMap.merge(sName, d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO,
									BigDecimal::add);

							boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
							BigDecimal tax = (d.tax() != null && !isDeferred) ? d.tax() : BigDecimal.ZERO;
							periodTaxMap.merge(sName, tax, BigDecimal::add);

							BigDecimal taxable = BigDecimal.ZERO;
							if (!isDeferred && d.taxPerShare() != null && d.quantity() != null) {
								taxable = d.taxPerShare().multiply(BigDecimal.valueOf(d.quantity()));
							}
							periodTaxableMap.merge(sName, taxable, BigDecimal::add);

						}
					}

					java.util.Set<String> allSeries = new java.util.HashSet<>();
					allSeries.addAll(periodGrossMap.keySet());

					for (String name : allSeries) {
						BigDecimal g = periodGrossMap.getOrDefault(name, BigDecimal.ZERO);
						BigDecimal t = periodTaxMap.getOrDefault(name, BigDecimal.ZERO);
						BigDecimal taxable = periodTaxableMap.getOrDefault(name, BigDecimal.ZERO);
						BigDecimal net = g.subtract(t);
						if (g.compareTo(BigDecimal.ZERO) > 0)
							rows.add(new AnalyticsRow(timeLabel, name, g, net, t, taxable));
					}
				}
			} else {
				// TOTAL
				rows.addAll(dividends.stream()
						.collect(Collectors.groupingBy(
								getSeriesName,
								Collectors.collectingAndThen(
										Collectors.toList(),
										list -> {
											BigDecimal g = list.stream()
													.map(d -> d.grossAmount() != null ? d.grossAmount()
															: BigDecimal.ZERO)
													.reduce(BigDecimal.ZERO, BigDecimal::add);
											BigDecimal t = list.stream()
													.map(d -> {
														boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(),
																false);
														return (d.tax() != null && !isDeferred) ? d.tax()
																: BigDecimal.ZERO;
													})
													.reduce(BigDecimal.ZERO, BigDecimal::add);
											BigDecimal taxable = list.stream()
													.map(d -> {
														boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(),
																false);
														if (!isDeferred && d.taxPerShare() != null
																&& d.quantity() != null) {
															return d.taxPerShare()
																	.multiply(BigDecimal.valueOf(d.quantity()));
														}
														return BigDecimal.ZERO;
													})
													.reduce(BigDecimal.ZERO, BigDecimal::add);
											return new java.math.BigDecimal[] { g, t, taxable };
										})))
						.entrySet().stream()
						.map(e -> {
							BigDecimal g = e.getValue()[0];
							BigDecimal t = e.getValue()[1];
							BigDecimal taxable = e.getValue()[2];
							BigDecimal net = g.subtract(t);
							return new AnalyticsRow("전체", e.getKey(), g, net, t, taxable);
						})
						.sorted((a, b) -> b.value1().compareTo(a.value1())) // Sort by Gross
						// REMOVED LIMIT
						//.limit(20)
						.toList());

				labels = rows.stream().map(AnalyticsRow::subKey).toList();
				List<BigDecimal> data = rows.stream().map(AnalyticsRow::value1).toList();
				datasets.add(new ChartDataset("배당금 (세전)", data, null, null, null, List.of()));
			}
		}

		BigDecimal totalValue = BigDecimal.ZERO;
		if (totalLabel != null) {
			totalValue = rows.stream()
					.map(AnalyticsRow::value4) // value4 is used for totals in Dividend (Net) or just use logic
					.filter(Objects::nonNull)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}

		model.addAttribute("chartTitle", chartTitle);
		model.addAttribute("keyLabel", keyLabel);
		model.addAttribute("subKeyLabel", subKeyLabel);
		model.addAttribute("value1Label", value1Label);
		model.addAttribute("value2Label", value2Label);
		model.addAttribute("value3Label", value3Label);
		model.addAttribute("value4Label", value4Label);
		model.addAttribute("totalLabel", totalLabel);
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

		// Allocation Map
		Map<String, BigDecimal> allocation = profitList.stream()
				.filter(p -> p.evaluationAmount() != null && p.evaluationAmount().compareTo(BigDecimal.ZERO) > 0)
				.collect(Collectors.groupingBy(
						p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
						Collectors.reducing(BigDecimal.ZERO, TradeProfit::evaluationAmount, BigDecimal::add)));

		BigDecimal totalAsset = profitList.stream().map(TradeProfit::evaluationAmount).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Use NET Profit (after fees/tax)
		BigDecimal totalRealizedVal = profitList.stream().map(TradeProfit::realizedProfitNet).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalUnrealizedVal = profitList.stream().map(TradeProfit::evaluationProfitNet)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Win Rate (Percentage of items with positive total profit)
		long winCount = profitList.stream()
				.filter(p -> p.totalProfitNet() != null && p.totalProfitNet().compareTo(BigDecimal.ZERO) > 0)
				.count();
		double winRate = profitList.isEmpty() ? 0.0 : (double) winCount / profitList.size() * 100;

		// 2. 배당 데이터 (전체 기간)
		DividendRequest dividendRequest = new DividendRequest();
		dividendRequest.setUserId(userId);
		List<DividendResponse> dividendList = dividendClient.findDividends(dividendRequest.toParams());

		// Fill names for dividends
		final Map<UUID, String> stockNames = new HashMap<>();
		final Map<UUID, String> accountNames = new HashMap<>();
		if (dividendList.stream().anyMatch(d -> d.stockItemName() == null)) {
			dividendList.stream().map(DividendResponse::stockItemId).filter(Objects::nonNull).distinct()
					.forEach(id -> stockNames.put(id,
							stockItemClient.getStockItemById(id).map(StockItem::name).orElse(UNKNOWN_LABEL)));
		}
		// DividendResponse does not have accountName, and we group by Stock for Top
		// List mostly.
		// If we needed account names, we would just fetch them all for the user.
		// accountClient.getAccountsByUserId(userId).forEach(a ->
		// accountNames.put(a.id(), a.name()));

		BigDecimal totalDividendVal = dividendList.stream().map(DividendResponse::grossAmount).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 3. Top Lists
		// Top Buying (Purchase Amount - Net Cost)
		List<TradeProfit> topBuying = profitList.stream()
				.filter(p -> p.totalBuyCost() != null)
				.sorted((p1, p2) -> p2.totalBuyCost().compareTo(p1.totalBuyCost()))
				.limit(4)
				.toList();
		
		// Bottom Buying
		List<TradeProfit> bottomBuying = profitList.stream()
				.filter(p -> p.totalBuyCost() != null)
				.sorted((p1, p2) -> p1.totalBuyCost().compareTo(p2.totalBuyCost()))
				.limit(4)
				.toList();


		// Top Realized Profit (Net) - Aggregated by Stock Item
		Map<String, BigDecimal> realizedByStock = profitList.stream()
				.filter(p -> p.realizedProfitNet() != null)
				.collect(Collectors.groupingBy(
						p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
						Collectors.reducing(BigDecimal.ZERO, TradeProfit::realizedProfitNet, BigDecimal::add)));

		List<TradeProfit> topRealized = realizedByStock.entrySet().stream()
				//.filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(4)
				.map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
				.toList();
		
		List<TradeProfit> bottomRealized = realizedByStock.entrySet().stream()
				//.filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
				.sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
				.limit(4)
				.map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
				.toList();
		
		
		// Top Unrealized Profit (Evaluation Profit)
		Map<String, BigDecimal> unrealizedByStock = profitList.stream()
				.filter(p -> p.evaluationProfitNet() != null)
				.collect(Collectors.groupingBy(
						p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
						Collectors.reducing(BigDecimal.ZERO, TradeProfit::evaluationProfitNet, BigDecimal::add)));
		
		List<TradeProfit> topUnrealized = unrealizedByStock.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(4)
				.map(e -> createSummaryTradeProfit2(e.getKey(), e.getValue()))
				.toList();
		
		List<TradeProfit> bottomUnrealized = unrealizedByStock.entrySet().stream()
				.sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
				.limit(4)
				.map(e -> createSummaryTradeProfit2(e.getKey(), e.getValue()))
				.toList();

		// Top Dividend (Gross) - Need to aggregate by Stock/Account first?
		// Usually "Top Dividend Stocks". Group by Series Name.
		Map<String, BigDecimal> dividendBySeries = new HashMap<>();
		dividendList.forEach(d -> {
			String name = d.stockItemName(); // Prefer stock name if available
			if (name == null)
				name = stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
			dividendBySeries.merge(name, d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO, BigDecimal::add);
		});

		List<Map.Entry<String, BigDecimal>> topDividend = dividendBySeries.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(4)
				.toList();
		
		List<Map.Entry<String, BigDecimal>> bottomDividend = dividendBySeries.entrySet().stream()
				.sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
				.limit(4)
				.toList();

		model.addAttribute("totalAsset", totalAsset);
		model.addAttribute("totalRealizedProfit", totalRealizedVal);
		model.addAttribute("totalUnrealizedProfit", totalUnrealizedVal);
		model.addAttribute("totalDividend", totalDividendVal);
		model.addAttribute("winRate", winRate);

		model.addAttribute("topBuying", topBuying);
		model.addAttribute("bottomBuying", bottomBuying);
		
		model.addAttribute("topRealized", topRealized);
		model.addAttribute("bottomRealized", bottomRealized);
		
		model.addAttribute("topUnrealized", topUnrealized);
		model.addAttribute("bottomUnrealized", bottomUnrealized);
		
		model.addAttribute("topDividend", topDividend);
		model.addAttribute("bottomDividend", bottomDividend);
		
		model.addAttribute("allocation", allocation);

		return "stock/htmx/fragments/summary :: summary";
	}
	
	private TradeProfit createSummaryTradeProfit(String stockItemName, BigDecimal realizedProfitNet) {
		return new TradeProfit(
				null,
				stockItemName,
				null,
				null,
				null,
				null,
				0,
				null,
				null,
				null,
				0,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				realizedProfitNet,
				null,
				null);
	}
	
	private TradeProfit createSummaryTradeProfit2(String stockItemName, BigDecimal evaluationProfitNet) {
		return new TradeProfit(
				null,
				stockItemName,
				null,
				null,
				null,
				null,
				0,
				null,
				null,
				null,
				0,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				evaluationProfitNet,
				null);
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
						Collectors.reducing(BigDecimal.ZERO,
								d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO,
								BigDecimal::add)));

		// Map을 Key(년월) 순으로 정렬
		Map<String, BigDecimal> sortedMonthly = new java.util.TreeMap<>(monthly);

		model.addAttribute("monthlyDividends", sortedMonthly);
		return "stock/htmx/fragments/charts :: dividend";
	}

	@GetMapping("/portfolio")
	public String portfolio(TradeProfitRequest request,
			@RequestParam(required = false) String sort,
			Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}
		request.setUserId(userId);
		List<TradeProfit> enrichedList = new ArrayList<>(getEnrichedTradeProfits(request));

		if (sort != null && !sort.isEmpty()) {
			String[] parts = sort.split(",");
			String field = parts[0];
			String direction = parts.length > 1 ? parts[1] : "asc";

			Comparator<TradeProfit> comparator = switch (field) {
				case "accountName" -> Comparator.comparing(TradeProfit::accountName,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "stockItemName" -> Comparator.comparing(TradeProfit::stockItemName,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "holdingQuantity" -> Comparator.comparing(TradeProfit::holdingQuantity,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "averageBuyPrice" -> Comparator.comparing(TradeProfit::averageBuyPrice,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "currentPrice" -> Comparator.comparing(TradeProfit::currentPrice,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "evaluationAmount" -> Comparator.comparing(TradeProfit::evaluationAmount,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "evaluationProfit" -> Comparator.comparing(TradeProfit::evaluationProfit,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "realizedProfit" -> Comparator.comparing(TradeProfit::realizedProfit,
						Comparator.nullsLast(Comparator.naturalOrder()));
				default -> null;
			};

			if (comparator != null) {
				if ("desc".equalsIgnoreCase(direction)) {
					comparator = comparator.reversed();
				}
				enrichedList.sort(comparator);
			}
		}

		model.addAttribute("tradeProfitList", enrichedList);
		model.addAttribute("sort", sort);
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
						(a, _) -> a)); // duplicate handling

		// Optimize: Bulk fetch all stock names instead of N+1 calls
		Map<UUID, String> stockItemNames = stockItemClient.getStockItems().stream()
				.collect(Collectors.toMap(StockItem::id, StockItem::name, (a, _) -> a));

		return tradeProfitList.stream()
				.map(profit -> new TradeProfit(
						profit.stockItemId(),
						stockItemNames.getOrDefault(profit.stockItemId(), UNKNOWN_LABEL),
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
	public String dividendList(DividendRequest request,
			@RequestParam(required = false) String sort,
			Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}

		request.setUserId(userId);

		List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

		List<Account> accounts = accountClient.getAccountsByUserId(userId);
		Map<UUID, String> accountNames = accounts.stream()
				.collect(Collectors.toMap(Account::id, Account::name, (left, _) -> left, LinkedHashMap::new));
		Map<UUID, Boolean> taxDeferredMap = accounts.stream().collect(Collectors.toMap(Account::id,
				a -> a.jsonConfig() != null && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
				(l, _) -> l));

		// 모든 stockItemId 수집 및 이름 조회
		// Optimize: Bulk fetch
		Map<UUID, String> stockItemNames = stockItemClient.getStockItems().stream()
				.collect(Collectors.toMap(StockItem::id, StockItem::name));

		List<DividendView> viewList = dividends.stream()
				.map(dividend -> {
					String accountName = accountNames.getOrDefault(dividend.accountId(), UNKNOWN_LABEL);
					String stockItemName = Optional.ofNullable(dividend.stockItemName())
							.orElse(Optional.ofNullable(dividend.stockItemId())
									.map(id -> stockItemNames.getOrDefault(id, UNKNOWN_LABEL))
									.orElse(UNKNOWN_LABEL));

					boolean isDeferred = taxDeferredMap.getOrDefault(dividend.accountId(), false);

					BigDecimal grossAmount = Optional.ofNullable(dividend.grossAmount()).orElse(BigDecimal.ZERO);
					BigDecimal tax = isDeferred ? BigDecimal.ZERO
							: Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
					BigDecimal netAmount = isDeferred ? grossAmount
							: Optional.ofNullable(dividend.netAmount()).orElse(grossAmount.subtract(tax));

					BigDecimal taxableAmount = BigDecimal.ZERO;
					if (!isDeferred && dividend.taxPerShare() != null && dividend.quantity() != null) {
						taxableAmount = dividend.taxPerShare().multiply(BigDecimal.valueOf(dividend.quantity()));
					}

					return new DividendView(
							dividend.id(),
							dividend.accountId(),
							accountName,
							dividend.stockItemId(),
							stockItemName,
							grossAmount,
							tax,
							taxableAmount,
							netAmount,
							dividend.recordDate(),
							dividend.payDate());
				})
				.collect(Collectors.toCollection(ArrayList::new));

		if (sort != null && !sort.isEmpty()) {
			String[] parts = sort.split(",");
			String field = parts[0];
			String direction = parts.length > 1 ? parts[1] : "asc";

			Comparator<DividendView> comparator = switch (field) {
				case "payDate" -> Comparator.comparing(DividendView::payDate,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "accountName" -> Comparator.comparing(DividendView::accountName,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "stockItemName" -> Comparator.comparing(DividendView::stockItemName,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "grossAmount" -> Comparator.comparing(DividendView::grossAmount,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "netAmount" -> Comparator.comparing(DividendView::netAmount,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "tax" -> Comparator.comparing(DividendView::tax,
						Comparator.nullsLast(Comparator.naturalOrder()));
				case "taxableAmount" -> Comparator.comparing(DividendView::taxableAmount,
						Comparator.nullsLast(Comparator.naturalOrder()));
				default -> null;
			};

			if (comparator != null) {
				if ("desc".equalsIgnoreCase(direction)) {
					comparator = comparator.reversed();
				}
				viewList.sort(comparator);
			}
		}

		model.addAttribute("dividendList", viewList);
		model.addAttribute("sort", sort);
		return "stock/htmx/fragments/tabs :: dividendHistory";
	}

	@GetMapping("/trade/list")
	public String tradeList(
			@RequestHeader(value = "userId", required = false) String userIdStr,
			@RequestParam(required = false) List<UUID> accountIdList,
			@RequestParam(required = false) List<UUID> stockItemIdList,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate,
			Model model) {

		UUID userId = UserUtil.getUserId();
		if (userId == null && userIdStr != null) {
			try {
				userId = UUID.fromString(userIdStr);
			} catch (Exception e) {
			}
		}

		if (userId == null) {
			model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
			return ERROR_VIEW;
		}

		LocalDate end = (endDate == null) ? LocalDate.now() : endDate;
		LocalDate start = (startDate == null) ? end.minusMonths(1) : startDate;

		java.time.Instant startInst = start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
		java.time.Instant endInst = end.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

		TradeSearchRequest request = new TradeSearchRequest(userId, accountIdList, stockItemIdList, startInst, endInst);
		List<TradeResponse> tradeList = tradeClient.findTrades(request.toParams());

		List<Account> accounts = accountClient.getAccountsByUserId(userId);
		Map<UUID, String> accountNames = accounts.stream()
				.collect(Collectors.toMap(Account::id, Account::name, (left, r) -> left));

		model.addAttribute("tradeList", tradeList);
		model.addAttribute("accountNames", accountNames);
		model.addAttribute("startDate", start);
		model.addAttribute("endDate", end);

		return "stock/htmx/trade";
	}

}
