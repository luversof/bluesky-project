package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.common.menu.domain.Pagination;
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
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockHtmxController {

  private static final String ERROR_ATTRIBUTE = "error";
  private static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요합니다";
  private static final String ERROR_VIEW = "stock/htmx/error";
  private static final String UNKNOWN_LABEL = "종목 정보 없음";

  @Autowired private TradeProfitClient tradeProfitClient;

  @Autowired private TradeClient tradeClient;

  @Autowired private AccountClient accountClient;

  @Autowired private StockItemClient stockItemClient;

  @Autowired private DividendClient dividendClient;

  private BigDecimal calculateDividendTax(DividendResponse d, boolean isDeferred) {
    if (isDeferred) {
      return BigDecimal.ZERO;
    }
    return d.tax() != null ? d.tax() : BigDecimal.ZERO;
  }

  private BigDecimal calculateDividendTaxable(DividendResponse d, boolean isDeferred) {
    if (isDeferred) {
      return BigDecimal.ZERO;
    }
    // 스프레드시트에서 직접 저장한 과세금액을 우선 사용
    if (d.taxableAmount() != null) {
      return d.taxableAmount();
    }
    // fallback: taxPerShare × quantity
    if (d.taxPerShare() != null && d.quantity() != null) {
      return d.taxPerShare().multiply(BigDecimal.valueOf(d.quantity()));
    }
    return BigDecimal.ZERO;
  }

  public record AnalyticsRow(
      String key,
      String subKey,
      BigDecimal value1,
      BigDecimal value2,
      BigDecimal value3,
      BigDecimal value4,
      BigDecimal value5,
      BigDecimal value6,
      BigDecimal value7) {}

  public record ChartDataset(
      String label,
      List<BigDecimal> data,
      String backgroundColor,
      String borderColor,
      Integer borderWidth,
      List<Integer> borderDash) {}

  @BlueskyPreAuthorize
  @GetMapping("/dashboard")
  public String dashboard(
      @RequestHeader(value = "HX-Request", required = false) boolean hxRequest, Model model) {
    if (hxRequest) {
      return "stock/htmx/dashboardContent";
    }
    return "stock/htmx/dashboard";
  }

  @BlueskyPreAuthorize
  @GetMapping("/daily-summary/view")
  public String dailySummaryView(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId != null) {
      model.addAttribute("accounts", accountClient.getAccountsByUserId(userId));
    }

    int currentYear = LocalDate.now().getYear();
    List<Integer> years = new ArrayList<>();
    for (int i = currentYear; i >= 2015; i--) {
      years.add(i);
    }
    model.addAttribute("years", years);
    model.addAttribute("currentYear", currentYear);
    model.addAttribute("currentMonth", LocalDate.now().getMonthValue());

    return "stock/htmx/daily-summary";
  }

  @BlueskyPreAuthorize
  @GetMapping("/daily-summary/data")
  public String dailySummaryData(
      @RequestParam(defaultValue = "PROFIT") String type, // PROFIT | DIVIDEND
      @RequestParam(defaultValue = "TOTAL") String timeScale, // TOTAL | MONTHLY | YEARLY
      @RequestParam(defaultValue = "SUMMARY") String groupBy, // STOCK | ACCOUNT | SUMMARY
      @RequestParam(required = false) Integer year,
      @RequestParam(defaultValue = "1") int month,
      @RequestParam(required = false) UUID accountId,
      Model model) {

    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;

    if (year == null) {
      year = LocalDate.now().getYear();
    }

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
    String value5Label = null;
    String value6Label = null;
    String totalLabel = "합계";

    String chartType = "bar";
    boolean isStacked = false;

    List<String> palette =
        List.of(
            "#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f", "#edc948", "#b07aa1", "#ff9da7",
            "#9c755f", "#bab0ac");

    // --- 1. PROFIT LOGIC ---
    if ("PROFIT".equals(type)) {
      value1Label = null;
      value2Label = null;
      value3Label = "TOTAL".equals(timeScale) ? "평가 금액" : null;
      value4Label = "TOTAL".equals(timeScale) ? "보유 손익" : null;
      value5Label = "매도 금액";
      value6Label = "실현 손익";
      totalLabel = null; // Hide Total for Profit view

      TradeProfitRequest request = new TradeProfitRequest();
      request.setUserId(userId);
      if (accountId != null) request.setAccountIdList(List.of(accountId));

      // 1. Get Current Snapshot (for Holding Quantity and Unrealized Profit)
      List<TradeProfit> currentProfits = getEnrichedTradeProfits(request);

      // 2. Get Period Realized Profit
      if ("YEARLY".equals(timeScale)) {
        request.setStartDate(
            LocalDate.of(year, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        request.setEndDate(
            LocalDate.of(year, 12, 31)
                .plusDays(1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant());
        keyLabel = year + "년";
      } else if ("MONTHLY".equals(timeScale)) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        request.setStartDate(start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        request.setEndDate(end.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        keyLabel = year + "년 " + month + "월";
      } else {
        keyLabel = "TOTAL";
      }

      List<TradeProfit> periodProfits =
          "TOTAL".equals(timeScale) ? currentProfits : getEnrichedTradeProfits(request);

      String groupLabel = "종목별";
      if ("ACCOUNT".equals(groupBy)) groupLabel = "계좌별";
      else if ("SUMMARY".equals(groupBy)) groupLabel = "합계";
      chartTitle = "매매/보유 손익 (" + groupLabel + ")";
      if (!"TOTAL".equals(timeScale)) {
        chartTitle = keyLabel + " " + chartTitle;
      }

      if ("SUMMARY".equals(groupBy)) {
        subKeyLabel = null;
      } else {
        subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌별" : "종목별";
      }

      Map<String, BigDecimal> realizedMap = new HashMap<>();
      Map<String, BigDecimal> unrealizedMap = new HashMap<>(); // EvaluationProfit
      Map<String, BigDecimal> evaluationAmountMap = new HashMap<>();
      Map<String, BigDecimal> sellAmountMap = new HashMap<>();
      Map<String, BigDecimal> buyAmountMap = new HashMap<>();
      Map<String, BigDecimal> holdingQuantityMap = new HashMap<>();
      Map<String, BigDecimal> costBasisMap = new HashMap<>();

      // Process Current Snapshot for Holdings
      // Even for YEARLY/MONTHLY, we might want to show currently held quantity if the
      // user traded it in that period?
      // But the requirement for period view is usually "How much did I make in
      // 2025?".
      // However, if the user bought a stock in 2025 and still holds it, they might
      // expect to see it?
      // But previous logic for TOTAL was clear.
      // The issue might be that for YEARLY view, we skip this block, so
      // 'holdingQuantityMap' is empty.
      // Then in the row mapper:
      // BigDecimal hQty = holdingQuantityMap.getOrDefault(name, BigDecimal.ZERO);
      // So hQty is 0.

      // If we want to show 'Current Holding' info even in YEARLY view (for context),
      // we should remove the 'if' check or modify it.
      // But typically 'Period Profit' focuses on Realized.
      // Let's assume the user just wants to see the realized profit rows.

      // The problem is likely here:
      if ("TOTAL".equals(timeScale)) {
        currentProfits.forEach(
            p -> {
              String name;
              if ("SUMMARY".equals(groupBy)) {
                name = "합계";
              } else {
                name =
                    "ACCOUNT".equals(groupBy)
                        ? (p.accountName() != null ? p.accountName() : "Unknown")
                        : (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
              }

              unrealizedMap.merge(
                  name,
                  p.evaluationProfitNet() != null ? p.evaluationProfitNet() : BigDecimal.ZERO,
                  BigDecimal::add);

              BigDecimal evalAmt = BigDecimal.ZERO;
              if (p.currentPrice() != null) {
                if (p.evaluationAmount() != null) {
                  evalAmt = p.evaluationAmount();
                }
              }
              evaluationAmountMap.merge(name, evalAmt, BigDecimal::add);

              // Holding Quantity & Cost Basis
              BigDecimal hQty = BigDecimal.valueOf(p.holdingQuantity());
              holdingQuantityMap.merge(name, hQty, BigDecimal::add);

              if (p.holdingQuantity() > 0 && p.averageBuyPrice() != null) {
                BigDecimal cost = p.averageBuyPrice().multiply(hQty);
                costBasisMap.merge(name, cost, BigDecimal::add);
              }
            });
      }

      // Process Period Profits for Realized
      periodProfits.forEach(
          p -> {
            String name;
            if ("SUMMARY".equals(groupBy)) {
              name = "합계";
            } else {
              name =
                  "ACCOUNT".equals(groupBy)
                      ? (p.accountName() != null ? p.accountName() : "Unknown")
                      : (p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL);
            }
            realizedMap.merge(
                name,
                p.realizedProfitNet() != null ? p.realizedProfitNet() : BigDecimal.ZERO,
                BigDecimal::add);
            sellAmountMap.merge(
                name,
                p.totalSellAmount() != null ? p.totalSellAmount() : BigDecimal.ZERO,
                BigDecimal::add);
            buyAmountMap.merge(
                name,
                p.totalBuyAmount() != null ? p.totalBuyAmount() : BigDecimal.ZERO,
                BigDecimal::add);

            // Ensure that even if realized/sell amount is 0, we track the key if it
            // exists
            // in periodProfits
            // Only if we want to show items that have 0 profit/sell amount but were
            // part of
            // the period result?
            // But period result usually implies some activity.
            // However, if we filter below based on value != 0, we might lose it.
            // If the user wants to see "Zero Profit" trades, we need to relax the
            // filter.
            // Or add a dummy value to a map to ensure key exists?
            // Actually, keys are added to maps above.
          });

      // Collect all keys to ensure we don't miss any negative profit items
      java.util.Set<String> allKeys = new java.util.HashSet<>();
      allKeys.addAll(realizedMap.keySet());
      allKeys.addAll(unrealizedMap.keySet());
      allKeys.addAll(sellAmountMap.keySet());
      allKeys.addAll(buyAmountMap.keySet());
      allKeys.addAll(holdingQuantityMap.keySet());

      rows.addAll(
          allKeys.stream()
              .map(
                  name -> {
                    BigDecimal r = realizedMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal u = unrealizedMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal e = evaluationAmountMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal s = sellAmountMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal b = buyAmountMap.getOrDefault(name, BigDecimal.ZERO);

                    BigDecimal hQty = holdingQuantityMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal totalCost = costBasisMap.getOrDefault(name, BigDecimal.ZERO);
                    BigDecimal avgPrice =
                        (hQty.compareTo(BigDecimal.ZERO) > 0)
                            ? totalCost.divide(hQty, 0, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Order: Qty(1), Price(2), Eval(3), Unrealized(4), Sell(5),
                    // Realized(6), Buy(7)
                    return new AnalyticsRow("전체", name, hQty, avgPrice, e, u, s, r, b);
                  })
              // Only rows that have activity in the selected period should be shown.
              // Activity = Realized Profit exists OR Sell Amount exists OR Buy Amount
              // exists.
              // If TIME_SCALE is TOTAL, then Holding Quantity also counts as activity
              // (current holding).
              // But for YEARLY/MONTHLY, merely holding the stock (value1) should NOT
              // be
              // enough to show it if there was no trade.
              .filter(
                  row -> {
                    boolean isTotal = "TOTAL".equals(timeScale);
                    boolean hasHolding = row.value1().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasSell = row.value5().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasRealized = row.value6().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasBuy =
                        row.value7() != null && row.value7().compareTo(BigDecimal.ZERO) != 0;

                    if (isTotal) {
                      // For TOTAL context, show if held OR sold OR realized
                      // OR bought
                      return hasHolding || hasSell || hasRealized || hasBuy;
                    } else {
                      // For YEARLY/MONTHLY, show ONLY if traded
                      // (sold/realized/bought)
                      // Holding (value1) is irrelevant for period view if no
                      // trade happened.
                      // We must rely on non-zero sell amount, non-zero
                      // realized profit, or non-zero
                      // buy amount to indicate activity.
                      return hasSell || hasRealized || hasBuy;
                    }
                  })
              .sorted((a, b) -> b.value6().compareTo(a.value6())) // Sort by Realized
              // Profit (value6)
              // .limit(20)
              .toList());

      labels = rows.stream().map(AnalyticsRow::subKey).toList();
      List<BigDecimal> rData = rows.stream().map(AnalyticsRow::value6).toList();
      List<BigDecimal> uData = rows.stream().map(AnalyticsRow::value4).toList();

      datasets.add(new ChartDataset("실현 손익", rData, "#4e79a7", "#4e79a7", 1, List.of()));
      if ("TOTAL".equals(timeScale)) {
        datasets.add(new ChartDataset("보유 손익", uData, "#f28e2b", "#f28e2b", 1, List.of()));
      }
    } else if ("DIVIDEND".equals(type)) {
      value1Label = "배당(세전)";
      value2Label = "지급액";
      value3Label = null; // 세금 컬럼 숨김 (후처리 계좌는 0으로 표시되어 의미 없음)
      value4Label = "과세금액"; // value4 (quantity * taxPerShare)

      totalLabel = null;

      DividendRequest request = new DividendRequest();
      request.setUserId(userId);
      if (accountId != null) request.setAccountIdList(List.of(accountId));

      if ("SUMMARY".equals(groupBy)) {
        subKeyLabel = null;
      } else {
        subKeyLabel = "ACCOUNT".equals(groupBy) ? "계좌별" : "종목별";
      }

      if ("YEARLY".equals(timeScale)) {
        // YEARLY -> Show Annual Stats for ALL Years (2015 ~ Current)
        int startYear = 2015;
        int endYear = LocalDate.now().getYear();

        request.setStartDate(
            LocalDate.of(startYear, 1, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant());
        request.setEndDate(
            LocalDate.of(endYear, 12, 31)
                .plusDays(1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant());

        chartTitle = "연도별 배당 집계 (" + startYear + " ~ " + endYear + ")";
        keyLabel = "연도";
        chartType = "bar";
        isStacked = true;

        // Generate Labels (Years)
        for (int i = startYear; i <= endYear; i++) labels.add(i + "년");

      } else if ("MONTHLY".equals(timeScale)) {
        // MONTHLY -> Show Monthly Stats for the specific YEAR (1??~ 12??
        // (Previously this was YEARLY logic)
        request.setStartDate(
            LocalDate.of(year, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        request.setEndDate(
            LocalDate.of(year, 12, 31)
                .plusDays(1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant());

        chartTitle = year + "년 월별 배당 집계";
        keyLabel = "년";
        chartType = "bar";
        isStacked = true;

        // Generate Labels (1??to 12??
        for (int i = 1; i <= 12; i++) labels.add(i + "월");

      } else {
        // TOTAL
        keyLabel = "전체";
        chartTitle = "누적 배당 총합 (전체)";
      }

      List<DividendResponse> dividends =
          Optional.ofNullable(dividendClient.findDividends(request.toParams()))
              .orElse(new ArrayList<>());

      // Resolve Names
      final Map<UUID, String> stockNames = new HashMap<>();

      List<Account> accounts = accountClient.getAccountsByUserId(userId);
      final Map<UUID, String> accountNames =
          accounts.stream()
              .collect(
                  Collectors.toMap(
                      Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));
      final Map<UUID, Boolean> taxDeferredMap =
          accounts.stream()
              .collect(
                  Collectors.toMap(
                      Account::id,
                      (Account account) -> {
                        if (account.jsonConfig() == null) return false;
                        Object val = account.jsonConfig().get("isTaxDeferred");
                        return Boolean.TRUE.equals(val)
                            || "true".equalsIgnoreCase(String.valueOf(val));
                      },
                      (l, r) -> l));
      if (dividends.stream().anyMatch(d -> d.stockItemName() == null)) {
        dividends.stream()
            .map(DividendResponse::stockItemId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(
                id ->
                    stockNames.put(
                        id,
                        stockItemClient
                            .getStockItemById(id)
                            .map(StockItem::name)
                            .orElse(UNKNOWN_LABEL)));
      }

      java.util.function.Function<DividendResponse, String> getSeriesName =
          d -> {
            if ("SUMMARY".equals(groupBy)) return "합계";
            if ("ACCOUNT".equals(groupBy))
              return accountNames.getOrDefault(d.accountId(), "Unknown");
            return d.stockItemName() != null
                ? d.stockItemName()
                : stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
          };

      // Build Chart Data
      if (!"TOTAL".equals(timeScale)) {
        Map<String, List<DividendResponse>> bySeries =
            dividends.stream()
                .filter(d -> d.payDate() != null)
                .collect(Collectors.groupingBy(getSeriesName));

        Map<String, BigDecimal> seriesTotals = new HashMap<>();
        bySeries.forEach(
            (name, list) -> {
              BigDecimal sum =
                  list.stream()
                      .map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              seriesTotals.put(name, sum);
            });

        // REMOVED LIMIT lines
        List<String> topSeries =
            seriesTotals.entrySet().stream()
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
              // YEARLY -> Group by Year (label is "YYYY년")
              int y = Integer.parseInt(label.replace("년", ""));
              pointSum =
                  seriesData.stream()
                      .filter(
                          d -> d.payDate().atZone(java.time.ZoneId.systemDefault()).getYear() == y)
                      .map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if ("MONTHLY".equals(timeScale)) {
              // MONTHLY -> Group by Month (label is "M월")
              int m = Integer.parseInt(label.replace("월", ""));
              pointSum =
                  seriesData.stream()
                      .filter(
                          d ->
                              d.payDate().atZone(java.time.ZoneId.systemDefault()).getMonthValue()
                                  == m)
                      .map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            dataPoints.add(pointSum);
          }
          String color = palette.get(colorIdx++ % palette.size());
          datasets.add(new ChartDataset(series, dataPoints, color, color, 2, List.of()));
        }

        // Table Rows
        List<BigDecimal> taxableTotals = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
          String timeLabel = labels.get(i);
          Map<String, BigDecimal> periodGrossMap = new HashMap<>();
          Map<String, BigDecimal> periodTaxMap = new HashMap<>();
          Map<String, BigDecimal> periodTaxableMap = new HashMap<>();

          for (DividendResponse d : dividends) {
            if (d.payDate() == null) continue;
            boolean match = false;

            if ("YEARLY".equals(timeScale)) {
              // YEARLY -> timeLabel is "YYYY??
              int y = Integer.parseInt(timeLabel.replace("년", ""));
              if (d.payDate().atZone(java.time.ZoneId.systemDefault()).getYear() == y) match = true;
            } else if ("MONTHLY".equals(timeScale)) {
              // MONTHLY -> timeLabel is "M??
              int m = Integer.parseInt(timeLabel.replace("월", ""));
              if (d.payDate().atZone(java.time.ZoneId.systemDefault()).getMonthValue() == m)
                match = true;
            }

            if (match) {
              String sName = getSeriesName.apply(d);
              periodGrossMap.merge(
                  sName,
                  d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO,
                  BigDecimal::add);

              boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
              BigDecimal tax = calculateDividendTax(d, isDeferred);
              periodTaxMap.merge(sName, tax, BigDecimal::add);

              BigDecimal taxable = calculateDividendTaxable(d, isDeferred);
              periodTaxableMap.merge(sName, taxable, BigDecimal::add);
            }
          }

          taxableTotals.add(
              periodTaxableMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

          java.util.Set<String> allSeries = new java.util.HashSet<>();
          allSeries.addAll(periodGrossMap.keySet());

          for (String name : allSeries) {
            BigDecimal g = periodGrossMap.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal t = periodTaxMap.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal taxable = periodTaxableMap.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal net = g.subtract(t);
            if (g.compareTo(BigDecimal.ZERO) > 0)
              rows.add(new AnalyticsRow(timeLabel, name, g, net, t, taxable, null, null, null));
          }
        }
      } else {
        // TOTAL
        rows.addAll(
            dividends.stream()
                .collect(
                    Collectors.groupingBy(
                        getSeriesName,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                              BigDecimal g =
                                  list.stream()
                                      .map(
                                          d ->
                                              d.grossAmount() != null
                                                  ? d.grossAmount()
                                                  : BigDecimal.ZERO)
                                      .reduce(BigDecimal.ZERO, BigDecimal::add);
                              BigDecimal t =
                                  list.stream()
                                      .map(
                                          d -> {
                                            boolean isDeferred =
                                                taxDeferredMap.getOrDefault(d.accountId(), false);
                                            return calculateDividendTax(d, isDeferred);
                                          })
                                      .reduce(BigDecimal.ZERO, BigDecimal::add);
                              BigDecimal taxable =
                                  list.stream()
                                      .map(
                                          d -> {
                                            boolean isDeferred =
                                                taxDeferredMap.getOrDefault(d.accountId(), false);
                                            return calculateDividendTaxable(d, isDeferred);
                                          })
                                      .reduce(BigDecimal.ZERO, BigDecimal::add);
                              return new java.math.BigDecimal[] {g, t, taxable};
                            })))
                .entrySet()
                .stream()
                .map(
                    e -> {
                      BigDecimal g = e.getValue()[0];
                      BigDecimal t = e.getValue()[1];
                      BigDecimal taxable = e.getValue()[2];
                      BigDecimal net = g.subtract(t);
                      return new AnalyticsRow(
                          "전체", e.getKey(), g, net, t, taxable, null, null, null);
                    })
                .sorted((a, b) -> b.value1().compareTo(a.value1())) // Sort by Gross
                // REMOVED LIMIT
                // .limit(20)
                .toList());

        labels = rows.stream().map(AnalyticsRow::subKey).toList();
        List<BigDecimal> data = rows.stream().map(AnalyticsRow::value1).toList();
        datasets.add(new ChartDataset("배당금(세전)", data, null, null, null, List.of()));
      }
    }

    BigDecimal totalValue = BigDecimal.ZERO;
    if (totalLabel != null) {
      totalValue =
          rows.stream()
              .map(AnalyticsRow::value4) // value4 is used for totals in Dividend (Net)
              // or just use logic
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
    model.addAttribute("value5Label", value5Label);
    model.addAttribute("value6Label", value6Label);
    model.addAttribute("totalLabel", totalLabel);
    model.addAttribute("tableData", rows);
    model.addAttribute("totalValue", totalValue);
    if ("DIVIDEND".equals(type) && "TOTAL".equals(timeScale)) {
      // TOTAL view: calculate taxable totals for tooltip
      // 'rows' contains all data. Sort order matches 'labels' derived from rows.
      // rows.value4() is Taxable Amount.
      List<BigDecimal> taxableTotalList =
          rows.stream().map(r -> r.value4() != null ? r.value4() : BigDecimal.ZERO).toList();
      model.addAttribute("taxableTotals", taxableTotalList);
    } else if ("DIVIDEND".equals(type) && !"TOTAL".equals(timeScale)) {
      // Calculate Taxable Totals again for Chart or refactor.
      // Simpler to just re-accumulate from 'rows' which have 'value4' (taxable) and
      // 'key' (timeLabel).
      // 'rows' contains all data.
      // Group 'rows' by 'key' (TimeLabel) and sum 'value4'.
      // Ensure order matches 'labels'.
      Map<String, BigDecimal> sumMap =
          rows.stream()
              .collect(
                  Collectors.groupingBy(
                      AnalyticsRow::key,
                      Collectors.reducing(
                          BigDecimal.ZERO,
                          r -> r.value4() != null ? r.value4() : BigDecimal.ZERO,
                          BigDecimal::add)));

      List<BigDecimal> taxableTotalList = new ArrayList<>();
      for (String lbl : labels) {
        taxableTotalList.add(sumMap.getOrDefault(lbl, BigDecimal.ZERO));
      }
      model.addAttribute("taxableTotals", taxableTotalList);
    }

    // Decide Chart Type
    // String chartType = "bar"; // Default -> chartType variable is already
    // defined.
    /*
     * if ("PROFIT".equals(type) && !"MONTHLY".equals(timeScale)) {
     * // Trend lines for Yearly/Total profit
     * chartType = "line";
     * // If we want stacked bars for profit breakdown, we can switch.
     * }
     */
    model.addAttribute("chartType", chartType);

    model.addAttribute("chartLabels", labels);
    // model.addAttribute("chartLabels", labels);
    model.addAttribute("chartDatasets", datasets);

    // Unique Canvas ID to prevent Chart.js reuse issues
    model.addAttribute("canvasId", "chart-" + UUID.randomUUID());

    return "stock/htmx/daily-summary-data";
  }

  @BlueskyPreAuthorize
  @GetMapping("/analytics/view")
  public String analyticsView(Model model) {
    UUID userId = UserUtil.getUserId();
    // Years list
    int currentYear = LocalDate.now().getYear();
    List<Integer> years = new ArrayList<>();
    for (int i = currentYear; i >= 2015; i--) {
      years.add(i);
    }
    model.addAttribute("years", years);
    model.addAttribute("currentYear", currentYear);
    model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
    if (userId != null) {
      model.addAttribute("accounts", accountClient.getAccountsByUserId(userId));
    }
    return "stock/htmx/analytics";
  }

  @BlueskyPreAuthorize
  @GetMapping("/analytics/data")
  public String analyticsData(
      @RequestParam(defaultValue = "PROFIT") String type, // PROFIT | DIVIDEND
      @RequestParam(defaultValue = "YEARLY") String timeScale, // TOTAL | MONTHLY | YEARLY
      @RequestParam(defaultValue = "STOCK") String groupBy, // STOCK | ACCOUNT
      @RequestParam(defaultValue = "2025") int year,
      @RequestParam(defaultValue = "1") int month,
      @RequestParam(required = false) UUID accountId,
      Model model) {

    // For now, reuse the dailySummaryData logic but point to daily-summary ::
    // data-content
    // Because the logic for data processing is robust there.
    // However, we need to handle the case where "timeScale=YEARLY" for PROFIT
    // behaves as "Yearly Trend" if possible.
    // Current dailySummaryData only handles "MONTHLY" for trend (per day) or
    // "TOTAL" (per year if passed?).
    // Let's modify dailySummaryData to handle YEARLY chart (Month per Month) if
    // needed, or create a new method if it diverges.

    // Reuse logic but return distinct view
    String viewName = dailySummaryData(type, timeScale, groupBy, year, month, accountId, model);
    if (ERROR_VIEW.equals(viewName)) {
      return ERROR_VIEW;
    }

    // Unique Canvas ID to prevent Chart.js reuse issues
    model.addAttribute("canvasId", "chart-" + UUID.randomUUID());
    model.addAttribute("type", type);

    return "stock/htmx/analytics-data";
  }

  @BlueskyPreAuthorize
  @GetMapping("/summary")
  public String summary(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    // 1. 자산/손익 데이터(Enriched to get names for Top Gainers)
    List<TradeProfit> profitList = getEnrichedTradeProfits(request);

    // Allocation Map
    Map<String, BigDecimal> allocation =
        profitList.stream()
            .filter(
                p ->
                    p.evaluationAmount() != null
                        && p.evaluationAmount().compareTo(BigDecimal.ZERO) > 0)
            .collect(
                Collectors.groupingBy(
                    p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
                    Collectors.reducing(
                        BigDecimal.ZERO, TradeProfit::evaluationAmount, BigDecimal::add)))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    // Account Allocation Map
    Map<String, BigDecimal> accountAllocation =
        profitList.stream()
            .filter(
                p ->
                    p.evaluationAmount() != null
                        && p.evaluationAmount().compareTo(BigDecimal.ZERO) > 0)
            .collect(
                Collectors.groupingBy(
                    p -> p.accountName() != null ? p.accountName() : "Unknown",
                    Collectors.reducing(
                        BigDecimal.ZERO, TradeProfit::evaluationAmount, BigDecimal::add)))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    BigDecimal totalAsset =
        profitList.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Use NET Profit (after fees/tax)
    BigDecimal totalRealizedVal =
        profitList.stream()
            .map(TradeProfit::realizedProfitNet)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalUnrealizedVal =
        profitList.stream()
            .map(TradeProfit::evaluationProfitNet)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Win Rate (Percentage of items with positive total profit)
    long winCount =
        profitList.stream()
            .filter(
                p ->
                    p.totalProfitNet() != null && p.totalProfitNet().compareTo(BigDecimal.ZERO) > 0)
            .count();
    double winRate = profitList.isEmpty() ? 0.0 : (double) winCount / profitList.size() * 100;

    // 2. 배당 데이터(전체 기간)
    DividendRequest dividendRequest = new DividendRequest();
    dividendRequest.setUserId(userId);
    List<DividendResponse> dividendList = dividendClient.findDividends(dividendRequest.toParams());

    // Fill names for dividends
    final Map<UUID, String> stockNames = new HashMap<>();
    final Map<UUID, String> accountNames = new HashMap<>();
    if (dividendList.stream().anyMatch(d -> d.stockItemName() == null)) {
      dividendList.stream()
          .map(DividendResponse::stockItemId)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(
              id ->
                  stockNames.put(
                      id,
                      stockItemClient
                          .getStockItemById(id)
                          .map(StockItem::name)
                          .orElse(UNKNOWN_LABEL)));
    }
    // DividendResponse does not have accountName, and we group by Stock for Top
    // List mostly.
    // If we needed account names, we would just fetch them all for the user.
    // accountClient.getAccountsByUserId(userId).forEach(a ->
    // accountNames.put(a.id(), a.name()));

    BigDecimal totalDividendVal =
        dividendList.stream()
            .map(DividendResponse::grossAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 3. Top Lists
    // Top Buying (Purchase Amount - Net Cost)
    List<TradeProfit> topBuying =
        profitList.stream()
            .filter(p -> p.totalBuyCost() != null)
            .sorted((p1, p2) -> p2.totalBuyCost().compareTo(p1.totalBuyCost()))
            .limit(4)
            .toList();

    // Bottom Buying
    List<TradeProfit> bottomBuying =
        profitList.stream()
            .filter(p -> p.totalBuyCost() != null)
            .sorted((p1, p2) -> p1.totalBuyCost().compareTo(p2.totalBuyCost()))
            .limit(4)
            .toList();

    // Top Realized Profit (Net) - Aggregated by Stock Item
    Map<String, BigDecimal> realizedByStock =
        profitList.stream()
            .filter(p -> p.realizedProfitNet() != null)
            .collect(
                Collectors.groupingBy(
                    p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
                    Collectors.reducing(
                        BigDecimal.ZERO, TradeProfit::realizedProfitNet, BigDecimal::add)));

    List<TradeProfit> topRealized =
        realizedByStock.entrySet().stream()
            // .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomRealized =
        realizedByStock.entrySet().stream()
            // .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
            .toList();

    // Top Unrealized Profit (Evaluation Profit)
    Map<String, BigDecimal> unrealizedByStock =
        profitList.stream()
            .filter(p -> p.evaluationProfitNet() != null)
            .collect(
                Collectors.groupingBy(
                    p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
                    Collectors.reducing(
                        BigDecimal.ZERO, TradeProfit::evaluationProfitNet, BigDecimal::add)));

    List<TradeProfit> topUnrealized =
        unrealizedByStock.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit2(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomUnrealized =
        unrealizedByStock.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit2(e.getKey(), e.getValue()))
            .toList();

    // Top Dividend (Gross) - Need to aggregate by Stock/Account first?
    // Usually "Top Dividend Stocks". Group by Series Name.
    Map<String, BigDecimal> dividendBySeries = new HashMap<>();
    dividendList.forEach(
        d -> {
          String name = d.stockItemName(); // Prefer stock name if available
          if (name == null) name = stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
          dividendBySeries.merge(
              name, d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO, BigDecimal::add);
        });

    List<TradeProfit> topDividend =
        dividendBySeries.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomDividend =
        dividendBySeries.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> createSummaryTradeProfit(e.getKey(), e.getValue()))
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
    model.addAttribute("accountAllocation", accountAllocation);

    return "stock/htmx/fragments/summary";
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

  private TradeProfit createSummaryTradeProfit2(
      String stockItemName, BigDecimal evaluationProfitNet) {
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

  @BlueskyPreAuthorize
  @GetMapping("/charts/allocation")
  public String allocationChart(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<TradeProfit> profitList = getEnrichedTradeProfits(request);

    // 종목별 비중 (Top 5 + Others)
    Map<String, BigDecimal> allocation =
        profitList.stream()
            .filter(p -> p.evaluationAmount() != null)
            .collect(
                Collectors.toMap(
                    p -> p.stockItemName() != null ? p.stockItemName() : UNKNOWN_LABEL,
                    TradeProfit::evaluationAmount,
                    BigDecimal::add));

    model.addAttribute("allocation", allocation);

    return "stock/htmx/fragments/chartsAllocation";
  }

  @BlueskyPreAuthorize
  @GetMapping("/charts/dividend")
  public String dividendChart(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;

    DividendRequest request = new DividendRequest();
    request.setUserId(userId);
    List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

    // 월별 그룹화(최근 12개월 or 전체) - 전체 월별 집계
    Map<String, BigDecimal> monthly =
        dividends.stream()
            .filter(d -> d.payDate() != null)
            .collect(
                Collectors.groupingBy(
                    d ->
                        d.payDate()
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO,
                        BigDecimal::add)));

    // Map Key(월) 기준으로 정렬
    Map<String, BigDecimal> sortedMonthly = new java.util.TreeMap<>(monthly);

    model.addAttribute("monthlyDividends", sortedMonthly);
    return "stock/htmx/fragments/chartsDividend";
  }

  @BlueskyPreAuthorize
  @GetMapping("/portfolio")
  public String portfolio(
      TradeProfitRequest request,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "ACCOUNT") String viewGroupBy,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
      return ERROR_VIEW;
    }
    request.setUserId(userId);
    List<TradeProfit> enrichedList = new ArrayList<>(getEnrichedTradeProfits(request));

    // Filter out zero holding quantity
    enrichedList.removeIf(tp -> tp.holdingQuantity() == 0);

    if ("STOCK".equals(viewGroupBy)) {
      // Aggregate by Stock Item
      Map<UUID, List<TradeProfit>> byStock =
          enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::stockItemId));

      List<TradeProfit> aggregatedList = new ArrayList<>();
      byStock.forEach(
          (stockId, list) -> {
            if (list.isEmpty()) return;

            TradeProfit first = list.get(0);
            String stockName = first.stockItemName();

            // Sums
            BigDecimal totalBuyAmount =
                list.stream()
                    .map(TradeProfit::totalBuyAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int holdingQty = list.stream().mapToInt(TradeProfit::holdingQuantity).sum();
            int totalSellQty = list.stream().mapToInt(TradeProfit::totalSellQuantity).sum();
            BigDecimal totalSellAmount =
                list.stream()
                    .map(TradeProfit::totalSellAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal realizedProfit =
                list.stream()
                    .map(TradeProfit::realizedProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationAmount =
                list.stream()
                    .map(TradeProfit::evaluationAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationProfit =
                list.stream()
                    .map(TradeProfit::evaluationProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit =
                list.stream()
                    .map(TradeProfit::totalProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBuyFee =
                list.stream()
                    .map(TradeProfit::totalBuyFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellFee =
                list.stream()
                    .map(TradeProfit::totalSellFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellTax =
                list.stream()
                    .map(TradeProfit::totalSellTax)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBuyCost =
                list.stream()
                    .map(TradeProfit::totalBuyCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellProceeds =
                list.stream()
                    .map(TradeProfit::totalSellProceeds)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal realizedProfitNet =
                list.stream()
                    .map(TradeProfit::realizedProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationProfitNet =
                list.stream()
                    .map(TradeProfit::evaluationProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfitNet =
                list.stream()
                    .map(TradeProfit::totalProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Averages
            BigDecimal currentPrice = first.currentPrice(); // Assumed same for same stock
            BigDecimal avgBuyPrice =
                (holdingQty > 0)
                    ? totalBuyAmount.divide(BigDecimal.valueOf(holdingQty), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal avgSellPrice =
                (totalSellQty > 0)
                    ? totalSellAmount.divide(
                        BigDecimal.valueOf(totalSellQty), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal avgBuyPriceNet =
                (holdingQty > 0)
                    ? totalBuyCost.divide(BigDecimal.valueOf(holdingQty), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal avgSellPriceNet =
                (totalSellQty > 0)
                    ? totalSellProceeds.divide(
                        BigDecimal.valueOf(totalSellQty), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            aggregatedList.add(
                new TradeProfit(
                    stockId,
                    stockName,
                    null,
                    "전체",
                    totalBuyAmount,
                    avgBuyPrice,
                    totalSellQty,
                    avgSellPrice,
                    totalSellAmount,
                    realizedProfit,
                    holdingQty,
                    currentPrice,
                    evaluationAmount,
                    evaluationProfit,
                    totalProfit,
                    totalBuyFee,
                    totalSellFee,
                    totalSellTax,
                    totalBuyCost,
                    totalSellProceeds,
                    avgBuyPriceNet,
                    avgSellPriceNet,
                    realizedProfitNet,
                    evaluationProfitNet,
                    totalProfitNet));
          });
      enrichedList = aggregatedList;
    }

    Comparator<TradeProfit> comparator = null;

    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";

      comparator =
          switch (field) {
            case "accountName" ->
                Comparator.comparing(
                    TradeProfit::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    TradeProfit::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "holdingQuantity" ->
                Comparator.comparing(
                    TradeProfit::holdingQuantity, Comparator.nullsLast(Comparator.naturalOrder()));
            case "averageBuyPrice" ->
                Comparator.comparing(
                    TradeProfit::averageBuyPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "currentPrice" ->
                Comparator.comparing(
                    TradeProfit::currentPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "evaluationAmount" ->
                Comparator.comparing(
                    TradeProfit::evaluationAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "evaluationProfit" ->
                Comparator.comparing(
                    TradeProfit::evaluationProfit, Comparator.nullsLast(Comparator.naturalOrder()));
            case "realizedProfit" ->
                Comparator.comparing(
                    TradeProfit::realizedProfit, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };

      if (comparator != null) {
        if ("desc".equalsIgnoreCase(direction)) {
          comparator = comparator.reversed();
        }
      }
    }

    // If grouping by ACCOUNT, ensure we sort by Account Name first
    Map<String, TradeProfit> accountTotalMap = new HashMap<>();
    if ("ACCOUNT".equals(viewGroupBy)) {
      Comparator<TradeProfit> accountComparator =
          Comparator.comparing(
              TradeProfit::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
      if (comparator == null) {
        comparator = accountComparator;
      } else {
        // Secondary sort
        comparator = accountComparator.thenComparing(comparator);
      }

      // Calculate Account Subtotals
      Map<String, List<TradeProfit>> byAccount =
          enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::accountName));

      byAccount.forEach(
          (accountName, list) -> {
            if (list.isEmpty()) return;

            BigDecimal totalBuyAmount =
                list.stream()
                    .map(TradeProfit::totalBuyAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int holdingQty = list.stream().mapToInt(TradeProfit::holdingQuantity).sum();
            int totalSellQty = list.stream().mapToInt(TradeProfit::totalSellQuantity).sum();
            BigDecimal totalSellAmount =
                list.stream()
                    .map(TradeProfit::totalSellAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal realizedProfit =
                list.stream()
                    .map(TradeProfit::realizedProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationAmount =
                list.stream()
                    .map(TradeProfit::evaluationAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationProfit =
                list.stream()
                    .map(TradeProfit::evaluationProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit =
                list.stream()
                    .map(TradeProfit::totalProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBuyFee =
                list.stream()
                    .map(TradeProfit::totalBuyFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellFee =
                list.stream()
                    .map(TradeProfit::totalSellFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellTax =
                list.stream()
                    .map(TradeProfit::totalSellTax)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBuyCost =
                list.stream()
                    .map(TradeProfit::totalBuyCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSellProceeds =
                list.stream()
                    .map(TradeProfit::totalSellProceeds)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal realizedProfitNet =
                list.stream()
                    .map(TradeProfit::realizedProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal evaluationProfitNet =
                list.stream()
                    .map(TradeProfit::evaluationProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfitNet =
                list.stream()
                    .map(TradeProfit::totalProfitNet)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            accountTotalMap.put(
                accountName,
                new TradeProfit(
                    null,
                    null,
                    null,
                    accountName,
                    totalBuyAmount,
                    BigDecimal.ZERO,
                    totalSellQty,
                    BigDecimal.ZERO,
                    totalSellAmount,
                    realizedProfit,
                    holdingQty,
                    BigDecimal.ZERO,
                    evaluationAmount,
                    evaluationProfit,
                    totalProfit,
                    totalBuyFee,
                    totalSellFee,
                    totalSellTax,
                    totalBuyCost,
                    totalSellProceeds,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    realizedProfitNet,
                    evaluationProfitNet,
                    totalProfitNet));
          });
    }

    if (comparator != null) {
      enrichedList.sort(comparator);
    }

    // Calculate Totals
    BigDecimal totalEvaluationAmount =
        enrichedList.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalEvaluationProfit =
        enrichedList.stream()
            .map(TradeProfit::evaluationProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalRealizedProfit =
        enrichedList.stream()
            .map(TradeProfit::realizedProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("tradeProfitList", enrichedList);
    model.addAttribute("sort", sort);
    model.addAttribute("viewGroupBy", viewGroupBy);
    model.addAttribute("totalEvaluationAmount", totalEvaluationAmount);
    model.addAttribute("totalEvaluationProfit", totalEvaluationProfit);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);
    model.addAttribute("accountTotalMap", accountTotalMap);
    return "stock/htmx/fragments/tabsPortfolio";
  }

  // Helper to get enriched data
  private List<TradeProfit> getEnrichedTradeProfits(TradeProfitRequest request) {
    List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request.toParams());

    Map<UUID, String> accountNames =
        tradeProfitList.stream()
            .map(TradeProfit::accountId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(
                Collectors.toMap(
                    id -> id,
                    id -> accountClient.getAccountById(id).map(Account::name).orElse(UNKNOWN_LABEL),
                    (a, b) -> a)); // duplicate handling

    // Optimize: Bulk fetch all stock names instead of N+1 calls
    Map<UUID, String> stockItemNames =
        stockItemClient.getStockItems().stream()
            .collect(Collectors.toMap(StockItem::id, StockItem::name, (a, b) -> a));
    return tradeProfitList.stream()
        .map(
            profit ->
                new TradeProfit(
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

  @BlueskyPreAuthorize
  @GetMapping("/dividend/list")
  public String dividendList(
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(required = false) String sort,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      model.addAttribute(ERROR_ATTRIBUTE, LOGIN_REQUIRED_MESSAGE);
      return ERROR_VIEW;
    }

    // Date Range: Optional
    Instant startInstant =
        (startDate == null)
            ? null
            : startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

    Instant endInstant =
        (endDate == null)
            ? null
            : endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

    var request = new DividendRequest();
    request.setUserId(userId);
    // Don't filter by account/stock at API level to get full list for Dropdowns
    request.setStartDate(startInstant);
    request.setEndDate(endInstant);

    List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

    // 1. Extract Unique IDs from the full result set (for Dropdowns)
    var dividendAccountIds =
        dividends.stream().map(DividendResponse::accountId).collect(Collectors.toSet());
    var dividendStockIds =
        dividends.stream().map(DividendResponse::stockItemId).collect(Collectors.toSet());

    // 2. Filter Account & Stock List for Dropdowns
    List<Account> accounts = accountClient.getAccountsByUserId(userId);
    List<Account> filteredAccountList =
        accounts.stream().filter(a -> dividendAccountIds.contains(a.id())).toList();

    Map<UUID, String> accountNames =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));
    Map<UUID, Boolean> taxDeferredMap =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id,
                    a ->
                        a.jsonConfig() != null
                            && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
                    (l, r) -> l));

    // Optimize: Bulk fetch and then filter
    List<StockItem> stockItemList = stockItemClient.getStockItems();
    List<StockItem> filteredStockItemList =
        stockItemList.stream().filter(s -> dividendStockIds.contains(s.id())).toList();

    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    // 3. Filter Dividends for View List based on User Selection
    List<DividendView> viewList =
        dividends.stream()
            .filter(
                d ->
                    (accountIdList == null
                        || accountIdList.isEmpty()
                        || accountIdList.contains(d.accountId())))
            .filter(
                d ->
                    (stockItemIdList == null
                        || stockItemIdList.isEmpty()
                        || stockItemIdList.contains(d.stockItemId())))
            .map(
                dividend -> {
                  String accountName =
                      accountNames.getOrDefault(dividend.accountId(), UNKNOWN_LABEL);
                  String stockItemName =
                      Optional.ofNullable(dividend.stockItemName())
                          .orElse(
                              Optional.ofNullable(dividend.stockItemId())
                                  .map(id -> stockItemNames.getOrDefault(id, UNKNOWN_LABEL))
                                  .orElse(UNKNOWN_LABEL));

                  boolean isDeferred = taxDeferredMap.getOrDefault(dividend.accountId(), false);

                  BigDecimal grossAmount =
                      Optional.ofNullable(dividend.grossAmount()).orElse(BigDecimal.ZERO);
                  BigDecimal tax =
                      isDeferred
                          ? BigDecimal.ZERO
                          : Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
                  BigDecimal netAmount =
                      isDeferred
                          ? grossAmount
                          : Optional.ofNullable(dividend.netAmount())
                              .orElse(grossAmount.subtract(tax));

                  BigDecimal taxableAmount = BigDecimal.ZERO;
                  if (!isDeferred
                      && dividend.taxPerShare() != null
                      && dividend.quantity() != null) {
                    taxableAmount =
                        dividend.taxPerShare().multiply(BigDecimal.valueOf(dividend.quantity()));
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

    // Sort
    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";

      Comparator<DividendView> comparator =
          switch (field) {
            case "payDate" ->
                Comparator.comparing(
                    DividendView::payDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "accountName" ->
                Comparator.comparing(
                    DividendView::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    DividendView::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "grossAmount" ->
                Comparator.comparing(
                    DividendView::grossAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "netAmount" ->
                Comparator.comparing(
                    DividendView::netAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "tax" ->
                Comparator.comparing(
                    DividendView::tax, Comparator.nullsLast(Comparator.naturalOrder()));
            case "taxableAmount" ->
                Comparator.comparing(
                    DividendView::taxableAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };

      if (comparator != null) {
        if ("desc".equalsIgnoreCase(direction)) {
          comparator = comparator.reversed();
        }
        viewList.sort(comparator);
      }
    } else {
      // Default Sort: PayDate Desc
      viewList.sort(
          Comparator.comparing(
              DividendView::payDate, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    // Pagination & Show All Logic
    if (size <= 0) size = 15;

    boolean isSearch =
        (accountIdList != null && !accountIdList.isEmpty())
            || (stockItemIdList != null && !stockItemIdList.isEmpty())
            || startDate != null
            || endDate != null;

    if (isSearch) {
      size = Math.max(viewList.size(), 1);
    }

    int totalItems = viewList.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int currentPage = Math.max(1, Math.min(page, totalPages));
    if (totalPages == 0) currentPage = 1;

    int fromIndex = (currentPage - 1) * size;
    int toIndex = Math.min(fromIndex + size, totalItems);

    List<DividendView> pagedList =
        (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex) : Collections.emptyList();

    // Calculate Totals
    BigDecimal totalGrossAmount =
        pagedList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalNetAmount =
        pagedList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTax =
        pagedList.stream().map(DividendView::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTaxableAmount =
        pagedList.stream()
            .map(DividendView::taxableAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
    var pagination = new Pagination(pageImpl);

    model.addAttribute("dividendList", pagedList);
    model.addAttribute("pagination", pagination);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("size", size);

    model.addAttribute("accountList", filteredAccountList);
    model.addAttribute("stockItemList", filteredStockItemList);

    // Preserved Values
    model.addAttribute(
        "selectedAccountId",
        (accountIdList != null && !accountIdList.isEmpty()) ? accountIdList.get(0) : null);
    model.addAttribute(
        "selectedStockItemId",
        (stockItemIdList != null && !stockItemIdList.isEmpty()) ? stockItemIdList.get(0) : null);
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);

    model.addAttribute("sort", sort);

    model.addAttribute("totalGrossAmount", totalGrossAmount);
    model.addAttribute("totalNetAmount", totalNetAmount);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("totalTaxableAmount", totalTaxableAmount);

    return "stock/htmx/fragments/tabsDividendHistory";
  }

  @BlueskyPreAuthorize
  @GetMapping("/trade/list")
  public String tradeList(
      @RequestHeader(value = "userId", required = false) String userIdStr,
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
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

    java.time.Instant startInst =
        (startDate == null)
            ? null
            : startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

    java.time.Instant endInst =
        (endDate == null)
            ? null
            : endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

    TradeSearchRequest request =
        new TradeSearchRequest(userId, accountIdList, stockItemIdList, startInst, endInst);
    List<TradeResponse> fullTradeList = tradeClient.findTrades(request.toParams());

    // Basic Data Fetching (Accounts & StockItems) for UI Labels
    List<Account> accountList = accountClient.getAccountsByUserId(userId);
    Map<UUID, String> accountNames =
        accountList.stream()
            .collect(
                Collectors.toMap(
                    Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));

    List<StockItem> stockItemList = stockItemClient.getStockItems();
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    // Enrich Trade List with Names (if needed for sorting or just logic)
    fullTradeList =
        fullTradeList.stream()
            .map(
                t ->
                    new TradeResponse(
                        t.id(),
                        t.accountId(),
                        t.stockItemId(),
                        stockItemNames.getOrDefault(t.stockItemId(), UNKNOWN_LABEL),
                        t.type(),
                        t.quantity(),
                        t.price(),
                        t.fee(),
                        t.tax(),
                        t.amount(),
                        t.realizedProfit(),
                        t.tradeDate()))
            .collect(Collectors.toCollection(ArrayList::new));

    // Sort by Date Descending
    fullTradeList.sort(
        Comparator.comparing(
            TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder())));

    if (size <= 0) size = 15;

    boolean isSearch =
        (accountIdList != null && !accountIdList.isEmpty())
            || (stockItemIdList != null && !stockItemIdList.isEmpty())
            || startDate != null
            || endDate != null;

    if (isSearch) {
      size = Math.max(fullTradeList.size(), 1);
    }

    // Pagination
    int totalItems = fullTradeList.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int currentPage = Math.max(1, Math.min(page, totalPages));
    if (totalPages == 0) currentPage = 1;

    int fromIndex = (currentPage - 1) * size;
    int toIndex = Math.min(fromIndex + size, totalItems);

    List<TradeResponse> pagedList =
        (fromIndex < totalItems)
            ? fullTradeList.subList(fromIndex, toIndex)
            : Collections.emptyList();

    // Calculate Sums for the visible list (pagedList)
    BigDecimal totalFee =
        pagedList.stream()
            .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalTax =
        pagedList.stream()
            .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalRealizedProfit =
        pagedList.stream()
            .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("totalFee", totalFee);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);

    var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
    var pagination = new Pagination(pageImpl);

    model.addAttribute("tradeList", pagedList);
    model.addAttribute("pagination", pagination);
    model.addAttribute("accountNames", accountNames);

    // Search Form Data
    model.addAttribute("accountList", accountList);
    model.addAttribute("stockItemList", stockItemList);

    // Preserved Values
    model.addAttribute(
        "selectedAccountId",
        (accountIdList != null && !accountIdList.isEmpty()) ? accountIdList.get(0) : null);
    model.addAttribute(
        "selectedStockItemId",
        (stockItemIdList != null && !stockItemIdList.isEmpty()) ? stockItemIdList.get(0) : null);
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);

    return "stock/htmx/tradeList";
  }

  public record Activity(
      String type,
      String stockItemName,
      String tradeType,
      Integer quantity,
      String description,
      BigDecimal amount,
      Instant date,
      List<String> accountNames) {}

  private List<Activity> getAllActivities(UUID userId) {
    // Fetch Trades
    TradeSearchRequest tradeReq = new TradeSearchRequest(userId, null, null, null, null);
    List<TradeResponse> trades = tradeClient.findTrades(tradeReq.toParams());

    // Fetch Dividends
    DividendRequest divReq = new DividendRequest();
    divReq.setUserId(userId);
    List<DividendResponse> dividends = dividendClient.findDividends(divReq.toParams());

    // Fetch Stock Names
    List<StockItem> stockItemList = stockItemClient.getStockItems();
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    // Fetch Account Names
    List<Account> accountList = accountClient.getAccountsByUserId(userId);
    Map<UUID, String> accountNamesMap =
        accountList.stream().collect(Collectors.toMap(Account::id, Account::name));

    List<Activity> rawActivities = new ArrayList<>();

    for (TradeResponse t : trades) {
      String stockName = stockItemNames.getOrDefault(t.stockItemId(), UNKNOWN_LABEL);
      String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "TRADE",
              stockName,
              t.type().name(),
              t.quantity(),
              null,
              t.amount(),
              t.tradeDate(),
              List.of(accountName)));
    }

    for (DividendResponse d : dividends) {
      String stockName =
          d.stockItemName() != null
              ? d.stockItemName()
              : stockItemNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
      String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "DIVIDEND",
              stockName,
              null,
              null,
              "배당금지급",
              d.netAmount(),
              d.payDate() != null ? d.payDate() : d.recordDate(),
              List.of(accountName)));
    }

    // Grouping Logic
    // Group by: Date (yyyy-MM-dd), Type (TRADE/DIVIDEND), StockName, TradeType
    // (BUY/SELL/null)
    Map<String, Activity> groupedMap = new HashMap<>();

    for (Activity a : rawActivities) {
      if (a.date() == null) continue;

      String dateStr = a.date().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
      String key =
          String.format("%s|%s|%s|%s", dateStr, a.type(), a.stockItemName(), a.tradeType());

      if (groupedMap.containsKey(key)) {
        Activity existing = groupedMap.get(key);

        Integer newQty = null;
        if (existing.quantity() != null || a.quantity() != null) {
          newQty =
              (existing.quantity() != null ? existing.quantity() : 0)
                  + (a.quantity() != null ? a.quantity() : 0);
        }

        BigDecimal newAmount = null;
        if (existing.amount() != null || a.amount() != null) {
          newAmount =
              (existing.amount() != null ? existing.amount() : BigDecimal.ZERO)
                  .add(a.amount() != null ? a.amount() : BigDecimal.ZERO);
        }

        List<String> newAccountNames = new ArrayList<>(existing.accountNames());
        if (!newAccountNames.contains(a.accountNames().get(0))) {
          newAccountNames.add(a.accountNames().get(0));
        }

        groupedMap.put(
            key,
            new Activity(
                existing.type(),
                existing.stockItemName(),
                existing.tradeType(),
                newQty,
                existing.description(),
                newAmount,
                existing.date(),
                newAccountNames));
      } else {
        groupedMap.put(key, a);
      }
    }

    List<Activity> activities = new ArrayList<>(groupedMap.values());
    activities.sort(
        Comparator.comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder())));
    return activities;
  }

  @BlueskyPreAuthorize
  @GetMapping("/recent-activities")
  public String recentActivities(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    List<Activity> activities = getAllActivities(userId);
    model.addAttribute("activities", activities.stream().limit(5).toList());

    return "stock/htmx/fragments/recentActivities";
  }

  @BlueskyPreAuthorize
  @GetMapping("/activity-list")
  public String activityList(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    List<Activity> activities = getAllActivities(userId);
    model.addAttribute("activities", activities);

    return "stock/htmx/fragments/activityList";
  }

  @GetMapping("/asset-growth/view")
  public String assetGrowthView(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    request.setUserId(userId);

    // 타임시리즈 데이터 위치
    var params = request.toParams();
    params.add("granularity", "AUTO");
    List<net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint> timeSeries =
        tradeProfitClient.timeSeries(params);

    model.addAttribute("timeSeries", timeSeries);
    return "stock/htmx/asset-growth";
  }

  @BlueskyPreAuthorize
  @GetMapping("/holdings-snapshot")
  public String holdingsSnapshot(@RequestParam(required = false) String date, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }
    if (date == null || date.isBlank()) {
      model.addAttribute("holdings", java.util.List.of());
      model.addAttribute("date", "");
      return "stock/htmx/holdings-snapshot";
    }
    var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
    params.add("userId", userId.toString());
    params.add("date", date);
    List<net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem> holdings =
        tradeProfitClient.holdingsSnapshot(params);
    model.addAttribute("holdings", holdings);
    model.addAttribute("date", date);
    return "stock/htmx/holdings-snapshot";
  }
}
