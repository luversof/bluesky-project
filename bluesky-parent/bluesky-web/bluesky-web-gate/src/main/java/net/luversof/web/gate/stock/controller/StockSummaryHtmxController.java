package net.luversof.web.gate.stock.controller;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockSummaryHtmxController extends StockBaseHtmxController {

  public StockSummaryHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient) {
    super(tradeProfitClient, tradeClient, accountClient, stockItemClient, dividendClient);
  }

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
      Map<String, BigDecimal> unrealizedMap = new HashMap<>();
      Map<String, BigDecimal> evaluationAmountMap = new HashMap<>();
      Map<String, BigDecimal> sellAmountMap = new HashMap<>();
      Map<String, BigDecimal> buyAmountMap = new HashMap<>();
      Map<String, BigDecimal> holdingQuantityMap = new HashMap<>();
      Map<String, BigDecimal> costBasisMap = new HashMap<>();

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

              BigDecimal hQty = BigDecimal.valueOf(p.holdingQuantity());
              holdingQuantityMap.merge(name, hQty, BigDecimal::add);

              if (p.holdingQuantity() > 0 && p.averageBuyPrice() != null) {
                BigDecimal cost = p.averageBuyPrice().multiply(hQty);
                costBasisMap.merge(name, cost, BigDecimal::add);
              }
            });
      }

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
          });

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

                    return new AnalyticsRow("전체", name, hQty, avgPrice, e, u, s, r, b);
                  })
              .filter(
                  row -> {
                    boolean isTotal = "TOTAL".equals(timeScale);
                    boolean hasHolding = row.value1().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasSell = row.value5().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasRealized = row.value6().compareTo(BigDecimal.ZERO) != 0;
                    boolean hasBuy =
                        row.value7() != null && row.value7().compareTo(BigDecimal.ZERO) != 0;

                    if (isTotal) {
                      return hasHolding || hasSell || hasRealized || hasBuy;
                    } else {
                      return hasSell || hasRealized || hasBuy;
                    }
                  })
              .sorted((a, b) -> b.value6().compareTo(a.value6()))
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
      value3Label = null;
      value4Label = "과세금액";

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
        int startYear = DIVIDEND_CHART_START_YEAR;
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

        for (int i = startYear; i <= endYear; i++) labels.add(i + "년");

      } else if ("MONTHLY".equals(timeScale)) {
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

        for (int i = 1; i <= 12; i++) labels.add(i + "월");

      } else {
        keyLabel = "전체";
        chartTitle = "누적 배당 총합 (전체)";
      }

      List<DividendResponse> dividends =
          Optional.ofNullable(dividendClient.findDividends(request.toParams()))
              .orElse(new ArrayList<>());

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

        List<String> topSeries =
            seriesTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        int colorIdx = 0;
        for (String series : topSeries) {
          List<BigDecimal> dataPoints = new ArrayList<>();
          List<DividendResponse> seriesData = bySeries.get(series);

          for (String label : labels) {
            BigDecimal pointSum = BigDecimal.ZERO;
            if ("YEARLY".equals(timeScale)) {
              int y = Integer.parseInt(label.replace("년", ""));
              pointSum =
                  seriesData.stream()
                      .filter(
                          d -> d.payDate().atZone(java.time.ZoneId.systemDefault()).getYear() == y)
                      .map(d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if ("MONTHLY".equals(timeScale)) {
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
              int y = Integer.parseInt(timeLabel.replace("년", ""));
              if (d.payDate().atZone(java.time.ZoneId.systemDefault()).getYear() == y) match = true;
            } else if ("MONTHLY".equals(timeScale)) {
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
                .sorted((a, b) -> b.value1().compareTo(a.value1()))
                .toList());

        labels = rows.stream().map(AnalyticsRow::subKey).toList();
        List<BigDecimal> data = rows.stream().map(AnalyticsRow::value1).toList();
        datasets.add(new ChartDataset("배당금 (세전)", data, null, null, null, List.of()));
      }
    }

    BigDecimal totalValue = BigDecimal.ZERO;
    if (totalLabel != null) {
      totalValue =
          rows.stream()
              .map(AnalyticsRow::value4)
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
      List<BigDecimal> taxableTotalList =
          rows.stream().map(r -> r.value4() != null ? r.value4() : BigDecimal.ZERO).toList();
      model.addAttribute("taxableTotals", taxableTotalList);
    } else if ("DIVIDEND".equals(type) && !"TOTAL".equals(timeScale)) {
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

    model.addAttribute("chartType", chartType);
    model.addAttribute("chartLabels", labels);
    model.addAttribute("chartDatasets", datasets);
    model.addAttribute("canvasId", "chart-" + UUID.randomUUID());

    return "stock/htmx/daily-summary-data";
  }

  @BlueskyPreAuthorize
  @GetMapping("/analytics/view")
  public String analyticsView(Model model) {
    UUID userId = UserUtil.getUserId();
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

    String viewName = dailySummaryData(type, timeScale, groupBy, year, month, accountId, model);
    if (ERROR_VIEW.equals(viewName)) {
      return ERROR_VIEW;
    }

    model.addAttribute("canvasId", "chart-" + UUID.randomUUID());
    model.addAttribute("type", type);

    return "stock/htmx/analytics-data";
  }

  @BlueskyPreAuthorize
  @GetMapping("/summary")
  public String summary(
      TradeProfitRequest request,
      @RequestParam(defaultValue = "true") boolean showCharts,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<TradeProfit> profitList = getEnrichedTradeProfits(request);

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

    long winCount =
        profitList.stream()
            .filter(
                p ->
                    p.totalProfitNet() != null && p.totalProfitNet().compareTo(BigDecimal.ZERO) > 0)
            .count();
    double winRate = profitList.isEmpty() ? 0.0 : (double) winCount / profitList.size() * 100;

    DividendRequest dividendRequest = new DividendRequest();
    dividendRequest.setUserId(userId);
    List<DividendResponse> dividendList = dividendClient.findDividends(dividendRequest.toParams());

    final Map<UUID, String> stockNames = new HashMap<>();
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

    BigDecimal totalDividendVal =
        dividendList.stream()
            .map(DividendResponse::netAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<TradeProfit> topBuying =
        profitList.stream()
            .filter(p -> p.totalBuyCost() != null)
            .sorted((p1, p2) -> p2.totalBuyCost().compareTo(p1.totalBuyCost()))
            .limit(4)
            .toList();

    List<TradeProfit> bottomBuying =
        profitList.stream()
            .filter(p -> p.totalBuyCost() != null)
            .sorted((p1, p2) -> p1.totalBuyCost().compareTo(p2.totalBuyCost()))
            .limit(4)
            .toList();

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
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> TradeProfit.ofRealizedSummary(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomRealized =
        realizedByStock.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> TradeProfit.ofRealizedSummary(e.getKey(), e.getValue()))
            .toList();

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
            .map(e -> TradeProfit.ofEvaluationSummary(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomUnrealized =
        unrealizedByStock.entrySet().stream()
            .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> TradeProfit.ofEvaluationSummary(e.getKey(), e.getValue()))
            .toList();

    Map<String, BigDecimal> dividendBySeries = new HashMap<>();
    dividendList.forEach(
        d -> {
          String name = d.stockItemName();
          if (name == null) name = stockNames.getOrDefault(d.stockItemId(), UNKNOWN_LABEL);
          dividendBySeries.merge(
              name, d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO, BigDecimal::add);
        });

    List<TradeProfit> topDividend =
        dividendBySeries.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> TradeProfit.ofRealizedSummary(e.getKey(), e.getValue()))
            .toList();

    List<TradeProfit> bottomDividend =
        dividendBySeries.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .limit(4)
            .map(e -> TradeProfit.ofRealizedSummary(e.getKey(), e.getValue()))
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
    model.addAttribute("showCharts", showCharts);

    return "stock/htmx/fragments/summary";
  }
}
