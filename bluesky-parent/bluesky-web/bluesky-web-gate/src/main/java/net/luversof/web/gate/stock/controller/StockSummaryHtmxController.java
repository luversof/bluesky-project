package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.domain.TradeProfitAggregator;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendSnapshotClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockSummaryHtmxController extends StockBaseHtmxController {

  private final MonthlyDividendSnapshotClient monthlyDividendSnapshotClient;
  private final MonthlyDividendProfileClient monthlyDividendProfileClient;
  private final MonthlyDividendPayoutClient monthlyDividendPayoutClient;
  private final MonthlyDividendCalculator monthlyDividendCalculator;

  public StockSummaryHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      MonthlyDividendSnapshotClient monthlyDividendSnapshotClient,
      MonthlyDividendProfileClient monthlyDividendProfileClient,
      MonthlyDividendPayoutClient monthlyDividendPayoutClient,
      MonthlyDividendCalculator monthlyDividendCalculator,
      MessageSource messageSource) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
    this.monthlyDividendSnapshotClient = monthlyDividendSnapshotClient;
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
    this.monthlyDividendCalculator = monthlyDividendCalculator;
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
  @GetMapping("/summary")
  public String summary(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<TradeProfit> profitList = getEnrichedTradeProfits(request);

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

    Map<UUID, BigDecimal> accountPrincipalOverrideMap =
        emptyIfNull(accountClient.getAccountsByUserId(userId)).stream()
            .filter(account -> account.id() != null)
            .flatMap(
                account -> {
                  BigDecimal principal = resolveAccountManualPrincipal(account);
                  return principal != null
                      ? java.util.stream.Stream.of(Map.entry(account.id(), principal))
                      : java.util.stream.Stream.empty();
                })
            .collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

    BigDecimal displayPrincipal =
        profitList.stream()
            .filter(profit -> profit.accountId() != null)
            .collect(Collectors.groupingBy(TradeProfit::accountId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  var sums = TradeProfitAggregator.aggregate(entry.getValue());
                  BigDecimal evaluationAmount =
                      Optional.ofNullable(sums.evaluationAmount()).orElse(BigDecimal.ZERO);
                  BigDecimal defaultEvaluationProfit =
                      Optional.ofNullable(sums.evaluationProfit()).orElse(BigDecimal.ZERO);
                  BigDecimal defaultPrincipal =
                      sums.evaluationAmount() != null && sums.evaluationProfit() != null
                          ? evaluationAmount.subtract(defaultEvaluationProfit)
                          : Optional.ofNullable(sums.totalBuyCost()).orElse(BigDecimal.ZERO);
                  return Optional.ofNullable(accountPrincipalOverrideMap.get(entry.getKey()))
                      .orElse(defaultPrincipal);
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(
                profitList.stream()
                    .filter(profit -> profit.accountId() == null)
                    .map(
                        profit ->
                            profit.evaluationAmount() != null && profit.evaluationProfit() != null
                                ? profit.evaluationAmount().subtract(profit.evaluationProfit())
                                : profit.totalBuyCost())
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

    BigDecimal grossCurrentEvaluationProfit =
        profitList.stream()
            .filter(profit -> profit.accountId() != null)
            .collect(Collectors.groupingBy(TradeProfit::accountId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  var sums = TradeProfitAggregator.aggregate(entry.getValue());
                  return Optional.ofNullable(sums.evaluationProfit()).orElse(BigDecimal.ZERO);
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(
                profitList.stream()
                    .filter(profit -> profit.accountId() == null)
                    .map(TradeProfit::evaluationProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

    BigDecimal displayCurrentEvaluationProfit =
        profitList.stream()
            .filter(profit -> profit.accountId() != null)
            .collect(Collectors.groupingBy(TradeProfit::accountId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  var sums = TradeProfitAggregator.aggregate(entry.getValue());
                  BigDecimal evaluationAmount =
                      Optional.ofNullable(sums.evaluationAmount()).orElse(BigDecimal.ZERO);
                  BigDecimal defaultEvaluationProfit =
                      Optional.ofNullable(sums.evaluationProfit()).orElse(BigDecimal.ZERO);
                  BigDecimal manualPrincipal = accountPrincipalOverrideMap.get(entry.getKey());
                  return manualPrincipal != null
                      ? evaluationAmount.subtract(manualPrincipal)
                      : defaultEvaluationProfit;
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(
                profitList.stream()
                    .filter(profit -> profit.accountId() == null)
                    .map(TradeProfit::evaluationProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

    BigDecimal holdingFeeAdjustment = grossCurrentEvaluationProfit.subtract(totalUnrealizedVal);
    BigDecimal manualPrincipalAdjustment =
        displayCurrentEvaluationProfit.subtract(grossCurrentEvaluationProfit);
    BigDecimal combinedAdjustmentAmount =
        displayCurrentEvaluationProfit.subtract(totalUnrealizedVal);

    long winCount =
        profitList.stream()
            .filter(
                p ->
                    p.totalProfitNet() != null && p.totalProfitNet().compareTo(BigDecimal.ZERO) > 0)
            .count();
    double winRate = profitList.isEmpty() ? 0.0 : (double) winCount / profitList.size() * 100;

    DividendRequest dividendRequest = new DividendRequest();
    dividendRequest.setUserId(userId);
    List<DividendResponse> dividendList =
        emptyIfNull(dividendClient.findDividends(dividendRequest.toParams()));

    BigDecimal totalDividendVal =
        dividendList.stream()
            .map(DividendResponse::netAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 최근 6개월 자산 추이 (히어로 미니 차트) — assetGrowthView 와 동일한 timeSeries 조회 패턴
    // 들어온 필터(accountIdList/stockItemIdList)를 유지한 채 기간만 6개월로 덮어쓴다
    java.time.ZoneId trendZone = java.time.ZoneId.systemDefault();
    LocalDate trendToday = LocalDate.now(trendZone);
    TradeProfitRequest trendRequest = copyTradeProfitRequest(request);
    trendRequest.setStartDate(trendToday.minusMonths(6).atStartOfDay(trendZone).toInstant());
    trendRequest.setEndDate(trendToday.plusDays(1).atStartOfDay(trendZone).toInstant());
    var trendParams = trendRequest.toParams();
    trendParams.add("granularity", "MONTHLY");
    List<TradeProfitTimeSeriesPoint> trendSeries =
        emptyIfNull(tradeProfitClient.timeSeries(trendParams));
    StringBuilder trendLabelSb = new StringBuilder();
    StringBuilder trendValueSb = new StringBuilder();
    int trendPointCount = 0;
    for (TradeProfitTimeSeriesPoint point : trendSeries) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      if (trendLabelSb.length() > 0) {
        trendLabelSb.append(",");
        trendValueSb.append(",");
      }
      // 라벨은 ISO 날짜만 — 사용자 유래 문자열 추가 금지(이스케이프 없는 $unsafe 경로)
      trendLabelSb
          .append("\"")
          .append(point.timestamp().atZone(trendZone).toLocalDate())
          .append("\"");
      BigDecimal holdingsValue =
          point.totalHoldingsValue() != null ? point.totalHoldingsValue() : BigDecimal.ZERO;
      trendValueSb.append(holdingsValue.toPlainString());
      trendPointCount++;
    }
    model.addAttribute("trendChartLabelsJs", "[" + trendLabelSb + "]");
    model.addAttribute("trendChartValuesJs", "[" + trendValueSb + "]");
    model.addAttribute("trendPointCount", trendPointCount);

    model.addAttribute("totalAsset", totalAsset);
    model.addAttribute("totalRealizedProfit", totalRealizedVal);
    model.addAttribute("totalUnrealizedProfit", totalUnrealizedVal);
    model.addAttribute("displayPrincipal", displayPrincipal);
    model.addAttribute("displayCurrentEvaluationProfit", displayCurrentEvaluationProfit);
    model.addAttribute("combinedAdjustmentAmount", combinedAdjustmentAmount);
    model.addAttribute("holdingFeeAdjustment", holdingFeeAdjustment);
    model.addAttribute("manualPrincipalAdjustment", manualPrincipalAdjustment);
    model.addAttribute("totalDividend", totalDividendVal);
    model.addAttribute("winRate", winRate);

    return "stock/htmx/fragments/summary";
  }

  /** 대시보드 "보유 비중" 카드 행 (stockItemId == null 이면 기타 묶음 행) */
  public record AllocationBarRow(
      UUID stockItemId, String stockItemName, double weightPercent, long amount) {}

  @BlueskyPreAuthorize
  @GetMapping("/summary/allocation")
  public String allocation(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;

    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);

    // 종목 단위 그룹핑(계좌 무시) 후 보유 중 + 평가금액 있는 종목만 평가금액 내림차순
    List<TradeProfit> holdings =
        getStockGroupedTradeProfits(request, false).stream()
            .filter(profit -> profit.holdingQuantity() > 0)
            .filter(
                profit ->
                    profit.evaluationAmount() != null
                        && profit.evaluationAmount().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(TradeProfit::evaluationAmount).reversed())
            .toList();

    BigDecimal totalEvaluationAmount =
        holdings.stream()
            .map(TradeProfit::evaluationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<AllocationBarRow> allocationRows =
        holdings.stream()
            .limit(5)
            .map(
                profit ->
                    new AllocationBarRow(
                        profit.stockItemId(),
                        profit.stockItemName() != null
                            ? profit.stockItemName()
                            : msg("stock.label.unknown"),
                        weightPercent(profit.evaluationAmount(), totalEvaluationAmount),
                        profit.evaluationAmount().longValue()))
            .toList();

    AllocationBarRow othersRow = null;
    if (holdings.size() > 5) {
      List<TradeProfit> others = holdings.subList(5, holdings.size());
      BigDecimal othersAmount =
          others.stream()
              .map(TradeProfit::evaluationAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      othersRow =
          new AllocationBarRow(
              null,
              msg("stock.dashboard.allocation.others", String.valueOf(others.size())),
              weightPercent(othersAmount, totalEvaluationAmount),
              othersAmount.longValue());
    }

    model.addAttribute("allocationRows", allocationRows);
    model.addAttribute("othersRow", othersRow);

    return "stock/htmx/fragments/allocationBars";
  }

  private static double weightPercent(BigDecimal amount, BigDecimal total) {
    return total.compareTo(BigDecimal.ZERO) > 0
        ? amount.doubleValue() / total.doubleValue() * 100
        : 0.0;
  }

  @BlueskyPreAuthorize
  @GetMapping("/summary/upcoming-dividends")
  public String upcomingDividends(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    List<MonthlyDividendSnapshotResponse> snapshots =
        emptyIfNull(monthlyDividendSnapshotClient.findSnapshots(params));
    model.addAttribute("hasSnapshots", !snapshots.isEmpty());

    // 심볼 -> 지급 시기(MID_MONTH/MONTH_END)
    Map<String, String> windowBySymbol = new HashMap<>();
    for (MonthlyDividendProfileResponse profile :
        emptyIfNull(monthlyDividendProfileClient.findProfiles(new LinkedMultiValueMap<>()))) {
      String sym = normalizeSymbol(profile.stockItemSymbol());
      if (sym != null && profile.payoutWindow() != null) {
        windowBySymbol.putIfAbsent(sym, profile.payoutWindow());
      }
    }

    // 실제 지급이력(payDate)에서 시기별 대표 지급일(day-of-month) 산출: 심볼별 최신 지급 1건만 사용
    Map<String, MonthlyDividendPayoutResponse> latestPayoutBySymbol = new HashMap<>();
    for (MonthlyDividendPayoutResponse p :
        emptyIfNull(monthlyDividendPayoutClient.findPayouts(new LinkedMultiValueMap<>()))) {
      if (p.payDate() == null) continue;
      String sym = normalizeSymbol(p.stockItemSymbol());
      if (sym == null) continue;
      latestPayoutBySymbol.merge(sym, p, (a, b) -> a.payDate().isAfter(b.payDate()) ? a : b);
    }
    int midRepDay = representativePayDay(latestPayoutBySymbol, windowBySymbol, "MID_MONTH", 15);
    int endRepDay = representativePayDay(latestPayoutBySymbol, windowBySymbol, "MONTH_END", 31);

    // 사용자가 실제 보유(스냅샷 존재)한 시기 중, 오늘 기준 지급일이 가장 가까운 시기를 "다가올" 시기로 선택.
    LocalDate today = LocalDate.now();
    boolean holdsMid = holdsWindow(snapshots, windowBySymbol, "MID_MONTH");
    boolean holdsEnd = holdsWindow(snapshots, windowBySymbol, "MONTH_END");
    String nextWindow = null;
    LocalDate nextPayDate = null;
    if (holdsMid) {
      nextWindow = "MID_MONTH";
      nextPayDate = projectedPayDate(today, midRepDay);
    }
    if (holdsEnd) {
      LocalDate endDate = projectedPayDate(today, endRepDay);
      if (nextPayDate == null || endDate.isBefore(nextPayDate)) {
        nextWindow = "MONTH_END";
        nextPayDate = endDate;
      }
    }

    // 다음 시기 종목: 시기 필터 → 예상 월배당 내림차순. (시기 미분류 종목만 있으면 전체 상위로 폴백)
    final String window = nextWindow;
    List<MonthlyDividendSnapshotResponse> windowRows =
        snapshots.stream()
            .filter(s -> s.expectedMonthlyDividend() != null)
            .filter(
                s ->
                    window == null
                        || window.equals(windowBySymbol.get(normalizeSymbol(s.stockItemSymbol()))))
            .sorted(
                (left, right) ->
                    right.expectedMonthlyDividend().compareTo(left.expectedMonthlyDividend()))
            .toList();
    BigDecimal windowTotal =
        windowRows.stream()
            .map(MonthlyDividendSnapshotResponse::expectedMonthlyDividend)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String nextWindowLabel =
        "MID_MONTH".equals(nextWindow)
            ? msg("stock.page.dividend.monthly.reference.profile.payout.window.mid.month")
            : "MONTH_END".equals(nextWindow)
                ? msg("stock.page.dividend.monthly.reference.profile.payout.window.month.end")
                : "";
    String nextPayDateLabel =
        nextPayDate != null
            ? nextPayDate.getMonthValue() + "월 " + nextPayDate.getDayOfMonth() + "일"
            : "";

    model.addAttribute("nextWindowLabel", nextWindowLabel);
    model.addAttribute("nextWindowIsMid", "MID_MONTH".equals(nextWindow));
    model.addAttribute("nextPayDateLabel", nextPayDateLabel);
    model.addAttribute("windowTotal", windowTotal);
    model.addAttribute("topDividendSnapshots", windowRows.stream().limit(5).toList());

    return "stock/htmx/fragments/upcomingDividends";
  }

  private static String normalizeSymbol(String symbol) {
    return symbol != null && !symbol.isBlank() ? symbol.trim().toUpperCase(Locale.ROOT) : null;
  }

  private static boolean holdsWindow(
      List<MonthlyDividendSnapshotResponse> snapshots,
      Map<String, String> windowBySymbol,
      String window) {
    return snapshots.stream()
        .anyMatch(s -> window.equals(windowBySymbol.get(normalizeSymbol(s.stockItemSymbol()))));
  }

  /** 해당 시기 종목들의 최신 지급일 day-of-month 평균(반올림). 이력이 없으면 fallback. */
  private static int representativePayDay(
      Map<String, MonthlyDividendPayoutResponse> latestPayoutBySymbol,
      Map<String, String> windowBySymbol,
      String window,
      int fallback) {
    List<Integer> days =
        latestPayoutBySymbol.entrySet().stream()
            .filter(e -> window.equals(windowBySymbol.get(e.getKey())))
            .map(e -> e.getValue().payDate().getDayOfMonth())
            .toList();
    if (days.isEmpty()) {
      return fallback;
    }
    return (int) Math.round(days.stream().mapToInt(Integer::intValue).average().orElse(fallback));
  }

  /** 오늘 이후 가장 가까운 대표 지급일. 이번 달 지급일이 오늘 이후면 이번 달, 지났으면 다음 달. */
  private static LocalDate projectedPayDate(LocalDate today, int repDay) {
    LocalDate thisMonth = today.withDayOfMonth(Math.min(repDay, today.lengthOfMonth()));
    if (!today.isAfter(thisMonth)) {
      return thisMonth;
    }
    LocalDate nm = today.plusMonths(1);
    return nm.withDayOfMonth(Math.min(repDay, nm.lengthOfMonth()));
  }
}
