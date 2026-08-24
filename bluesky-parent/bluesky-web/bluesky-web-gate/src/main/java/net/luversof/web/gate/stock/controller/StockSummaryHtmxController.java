package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
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
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.domain.TradeProfitAggregator;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup;
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
  private final ExecutorService stockRemoteCallExecutor;

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
      ExecutorService stockRemoteCallExecutor,
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
    this.stockRemoteCallExecutor = stockRemoteCallExecutor;
  }

  /** 서로 의존이 없는 원격 호출을 동시에 던진다. */
  private <T> CompletableFuture<T> supplyRemote(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, stockRemoteCallExecutor);
  }

  /**
   * join 이 감싸는 CompletionException 을 벗겨 순차 호출과 같은 예외가 밖으로 나가게 한다.
   *
   * <p>벗기는 규칙은 {@link StockAsyncSupport#join} 한 곳에만 둔다. 예전에는 같은 코드가 여기와 자산성장 컨트롤러에도 복사돼 있었다 &mdash;
   * 이 저장소에서 같은 공식이 여러 곳에 있으면 한쪽만 고쳐져 갈라진 사례가 반복됐다(활동 묶기 3벌, 창 계산 3벌, 매도원가 2곳).
   */
  private static <T> T joinRemote(CompletableFuture<T> future) {
    return net.luversof.web.gate.stock.support.StockAsyncSupport.join(future);
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
    if (userId == null) return loginRequiredView(model);
    request.setUserId(userId);
    // 이 화면의 요약 카드는 '지금 보유'다. 기간이 실려 오면 api-stock 은 평가(현재가/평가금액/평가손익)를
    // 아예 계산하지 않아 평가액이 0 으로 나간다(실측: 같은 사용자의 보유 18행이 기간 없이는 평가액
    // 1,493,281,835 인데 기간을 주면 0). 이 요청은 URL 에서 바인딩되므로 화면이 안 보내더라도
    // 엔드포인트를 직접 부르면 그대로 드러난다. 자산현황/포트폴리오와 같은 이유로 여기서 떨어낸다.
    request.setStartDate(null);
    request.setEndDate(null);

    // 이 화면이 쓰는 api-stock 호출 5개(계좌/종목/손익/배당/추이)는 서로 의존이 없다.
    // 순차로 던지면 응답시간이 그대로 합산된다(실측: 프래그먼트 201ms). 한꺼번에 던지고 결과만 모은다.
    var accountsFuture = supplyRemote(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));

    // 계좌 필터가 있으면 보내기 전에 이 사용자 계좌로 좁힌다. 없는 id 가 하나라도 섞이면 api-stock 이
    // 요청을 거절해 이 화면이 통째로 "불러오지 못했습니다"가 됐다(저장된 선택이 지워진 계좌를 가리킬 때).
    // 목록/활동 화면이 이미 쓰는 교집합 방식과 같다. 필터가 없으면 기다리지 않고 그대로 던진다.
    boolean emptyAccountSelection = narrowToOwnedAccounts(request, accountsFuture);

    // 파라미터는 스레드에 넘기기 전에 만들어 둔다(요청 객체를 공유하지 않기 위해).
    var profitParams = request.toParams();

    // 최근 6개월 자산 추이 (히어로 미니 차트) — assetGrowthView 와 동일한 timeSeries 조회 패턴
    // 들어온 필터(accountIdList/stockItemIdList)를 유지한 채 기간만 6개월로 덮어쓴다
    java.time.ZoneId trendZone = java.time.ZoneId.systemDefault();
    LocalDate trendToday = LocalDate.now(trendZone);
    TradeProfitRequest trendRequest = copyTradeProfitRequest(request);
    trendRequest.setStartDate(trendToday.minusMonths(6).atStartOfDay(trendZone).toInstant());
    trendRequest.setEndDate(trendToday.plusDays(1).atStartOfDay(trendZone).toInstant());
    var trendParams = trendRequest.toParams();
    trendParams.add("granularity", "MONTHLY");

    var stockItemsFuture = supplyRemote(() -> emptyIfNull(stockItemClient.getStockItems()));
    var rawProfitFuture =
        emptyAccountSelection
            ? null
            : supplyRemote(() -> emptyIfNull(tradeProfitClient.calculateProfit(profitParams)));
    // 세후 배당 합계. 예전에는 전 기간 이력을 통째로 받아 더했고(193건 79,919 바이트), 그 다음에는
    // 사용자 전체 합계만 주는 meta 를 썼다. 후자는 계좌/종목 필터를 걸어도 배당만 전체 값이라
    // '누적 확정 수익'이 실현손익(필터 적용)과 어긋났다(실측: KB증권 위탁 10,113,820 이어야 할 값이
    // 61,646,257). 손익 조회와 같은 파라미터로 집계해 두 항의 조건을 맞춘다.
    // 계좌 선택이 빈 목록이면 이 조회도 건너뛴다. 빈 목록은 toParams() 에서 파라미터가 통째로 빠져
    // '필터 없음'(= 전체)이 되므로, 손익·추이는 0 인데 이 항만 전 기간 합계(실측 61,645,687)가 찍힌다.
    // 위에 적힌 사고(10,113,820 자리에 61,646,257)와 같은 어긋남이 다른 문으로 되돌아오는 자리다.
    // 종목별로 나눠 받는다. 합계는 값들의 합이라 /total 을 따로 부르지 않는다(호출 수 그대로).
    // 종목별이 필요한 이유는 아래 '수익권 종목 비율'에 있다.
    var dividendByStockFuture =
        emptyAccountSelection
            ? null
            : supplyRemote(() -> dividendClient.findDividendTotalByStockItem(profitParams));
    var trendFuture =
        emptyAccountSelection
            ? null
            : supplyRemote(() -> emptyIfNull(tradeProfitClient.timeSeries(trendParams)));

    // 아래에서 계좌 목록을 원금 보정에 다시 쓰므로 한 번만 읽어 이름 맵까지 같이 만든다.
    List<Account> userAccountList = joinRemote(accountsFuture);
    List<TradeProfit> profitList =
        rawProfitFuture == null
            ? List.<TradeProfit>of()
            : enrichTradeProfits(
                joinRemote(rawProfitFuture),
                userId,
                toTradeProfitNames(userAccountList, joinRemote(stockItemsFuture)));

    BigDecimal totalAsset =
        profitList.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 매매 화면 헤드라인과 같은 기준(매도 거래에 기록된 값)을 쓴다. realizedProfitNet 은 앱이 평균단가로
    // 다시 계산한 값이라 253,553 원 낮게 나왔다(실측 2026-08-23). 배당 합계도 세후이므로 세금이 이미
    // 빠진 기록값과 더하는 편이 '누적 확정 수익'의 뜻에 맞는다.
    BigDecimal totalRealizedVal =
        profitList.stream()
            .map(TradeProfit::realizedProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    // 실현손익과 같은 기준(기본값). 섞으면 '실현+평가=총' 항등식이 깨진다(실측: 61행 중 18행).
    BigDecimal totalUnrealizedVal =
        profitList.stream()
            .map(TradeProfit::evaluationProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<UUID, BigDecimal> accountPrincipalOverrideMap = new LinkedHashMap<>();
    for (Account account : userAccountList) {
      if (account.id() == null) {
        continue;
      }
      BigDecimal principal = resolveAccountManualPrincipal(account);
      if (principal != null) {
        // 같은 계좌가 중복이면 먼저 값을 유지한다(기존 toMap 병합 규칙과 동일).
        accountPrincipalOverrideMap.putIfAbsent(account.id(), principal);
      }
    }

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
                  // 보유 원가는 포트폴리오 화면과 같은 헬퍼를 쓴다. 예전에는 폴백이 totalBuyCost 였는데
                  // 그것은 기간 누적 매수액이라 성격이 다르다(실측: 735,958,622 vs 실제 632,223,825).
                  BigDecimal defaultPrincipal =
                      resolveCurrentHoldingCost(
                          sums.evaluationAmount(),
                          sums.evaluationProfit(),
                          sums.avgBuyPrice(),
                          sums.holdingQuantity());
                  return accountPrincipal(
                      accountPrincipalOverrideMap.get(entry.getKey()), defaultPrincipal);
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(
                profitList.stream()
                    .filter(profit -> profit.accountId() == null)
                    .map(this::resolveCurrentHoldingCost)
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
                  return accountEvaluationProfit(
                      accountPrincipalOverrideMap.get(entry.getKey()),
                      evaluationAmount,
                      defaultEvaluationProfit);
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

    // 이 카드는 "종목 승률"인데 profitList 는 계좌x종목 단위다. 그대로 세면 5개 계좌에 나눠
    // 담은 종목이 5번 세어지고, 같은 종목이 어떤 계좌에선 수익 어떤 계좌에선 손실로 양쪽에 잡힌다
    // (실측: 계좌x종목 61건 기준 63.9% vs 종목 42개 기준 73.8%).
    // 종목별로 손익을 합쳐서 센다 — 합계는 API 의 종목 그룹 결과와 일치한다(실측 확인).
    Map<UUID, BigDecimal> profitByStockItem = new LinkedHashMap<>();
    for (TradeProfit profit : profitList) {
      if (profit == null || profit.stockItemId() == null) {
        continue;
      }
      BigDecimal value =
          profit.totalProfitNet() != null ? profit.totalProfitNet() : BigDecimal.ZERO;
      profitByStockItem.merge(profit.stockItemId(), value, BigDecimal::add);
    }
    Map<UUID, BigDecimal> dividendByStockItem =
        dividendByStockFuture == null ? Map.of() : joinRemote(dividendByStockFuture);
    BigDecimal dividendTotal =
        dividendByStockFuture == null
            ? null
            : dividendByStockItem.values().stream()
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 배당을 얹어서 센다. totalProfitNet 은 실현+평가일 뿐 배당이 없다. 이 카드 바로 위에 누적 배당이
    // 있고 같은 카드의 '합산 손익'은 배당을 더하는데, 승패만 배당을 빼고 세면 배당이 큰 종목이 실제로는
    // 이익인데 패로 잡힌다(실측 2026-08-24: TIGER 리츠부동산인프라 실현+평가 -2,929,544 · 배당
    // 5,132,889 -> 실제 +2,203,345. 42종목 중 이 1종목이 뒤집혀 76.19% 대신 78.57%).
    long winCount = countProfitableStocks(profitByStockItem, dividendByStockItem);
    double winRate =
        profitByStockItem.isEmpty() ? 0.0 : (double) winCount / profitByStockItem.size() * 100;
    int winDenominator = profitByStockItem.size();
    BigDecimal totalDividendVal = dividendTotal != null ? dividendTotal : BigDecimal.ZERO;

    List<TradeProfitTimeSeriesPoint> trendSeries =
        trendFuture == null ? List.<TradeProfitTimeSeriesPoint>of() : joinRemote(trendFuture);
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

    // 총자산은 마지막으로 수집된 종가로 계산된 값이다. 시세 수집이 자동이 아니라서 며칠 전 값일 수 있는데,
    // 요약 화면만 그 사실을 밝히지 않고 있었다(자산 현황·포트폴리오·종목/계좌 상세에는 이미 있다).
    model.addAttribute(
        "priceBasisDate",
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.latestPriceBasisDate(profitList));
    model.addAttribute("totalAsset", totalAsset);
    model.addAttribute("totalRealizedProfit", totalRealizedVal);
    model.addAttribute("totalUnrealizedProfit", totalUnrealizedVal);
    model.addAttribute("displayPrincipal", displayPrincipal);
    model.addAttribute("displayCurrentEvaluationProfit", displayCurrentEvaluationProfit);
    model.addAttribute("combinedAdjustmentAmount", combinedAdjustmentAmount);
    model.addAttribute("holdingFeeAdjustment", holdingFeeAdjustment);
    // 표시되는 평가손익은 매수 수수료를 원가로 보지 않는 기준이다. 그 금액을 밝히지 않으면
    // 수수료가 반영된 값으로 오해한다(실측 2026-08-23: 24,986 원).
    model.addAttribute(
        "excludedHoldingBuyFee",
        net.luversof.web.gate.stock.util.StockProfitBasisUtil.excludedHoldingBuyFee(profitList));
    model.addAttribute("manualPrincipalAdjustment", manualPrincipalAdjustment);
    model.addAttribute("totalDividend", totalDividendVal);
    model.addAttribute("winRate", winRate);
    model.addAttribute("winCount", winCount);
    model.addAttribute("winDenominator", winDenominator);

    return "stock/htmx/fragments/summary";
  }

  /** 대시보드 "보유 비중" 카드 행 (stockItemId == null 이면 기타 묶음 행) */
  public record AllocationBarRow(
      UUID stockItemId, String stockItemName, double weightPercent, long amount) {}

  @BlueskyPreAuthorize
  @GetMapping("/summary/allocation")
  public String allocation(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return loginRequiredView(model);

    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);

    // 손익 조회와 이름 재료(계좌/종목) 조회는 서로 의존이 없다. 예전에는 이름 붙이기 안에서
    // 계좌·종목을 다시 읽어 한 프래그먼트가 세 호출을 순차로 던졌다.
    var stockGroupedRequest = copyTradeProfitRequest(request);
    stockGroupedRequest.setGroupBy(
        net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM);
    var stockGroupedParams = stockGroupedRequest.toParams();
    var rawStockGroupedFuture =
        supplyRemote(() -> emptyIfNull(tradeProfitClient.calculateProfit(stockGroupedParams)));
    var accountsFuture = supplyRemote(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));
    var stockItemsFuture = supplyRemote(() -> emptyIfNull(stockItemClient.getStockItems()));

    List<TradeProfit> stockGroupedList =
        new ArrayList<>(
            enrichTradeProfits(
                joinRemote(rawStockGroupedFuture),
                userId,
                toTradeProfitNames(joinRemote(accountsFuture), joinRemote(stockItemsFuture))));
    // getStockGroupedTradeProfits(request, false) 와 같은 순서로 보유량 0 을 먼저 걸러낸다.
    stockGroupedList.removeIf(tp -> tp.holdingQuantity() == 0);

    // 종목 단위 그룹핑(계좌 무시) 후 보유 중 + 평가금액 있는 종목만 평가금액 내림차순
    List<TradeProfit> holdings =
        stockGroupedList.stream()
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

  /**
   * 수익권 종목 수. 종목 손익에 그 종목의 세후 배당을 얹어 부호를 본다.
   *
   * <p>{@code totalProfitNet} 은 실현+평가일 뿐 배당이 없다. 배당이 큰 종목은 실제로 이익인데 패로 잡힌다(실측 2026-08-24: TIGER
   * 리츠부동산인프라 실현+평가 -2,929,544 · 배당 5,132,889 -> 실제 +2,203,345).
   *
   * <p>분모는 손익 쪽 종목 수 그대로다. 배당만 있고 손익 행이 없는 종목이 분모를 늘리지 않는다.
   */
  static long countProfitableStocks(
      Map<UUID, BigDecimal> profitByStockItem, Map<UUID, BigDecimal> dividendByStockItem) {
    return profitByStockItem.entrySet().stream()
        .filter(
            entry -> {
              BigDecimal profit = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
              BigDecimal dividend =
                  dividendByStockItem == null
                      ? BigDecimal.ZERO
                      : dividendByStockItem.getOrDefault(entry.getKey(), BigDecimal.ZERO);
              return profit
                      .add(dividend != null ? dividend : BigDecimal.ZERO)
                      .compareTo(BigDecimal.ZERO)
                  > 0;
            })
        .count();
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
    if (userId == null) return loginRequiredView(model);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    // 스냅샷·프로필·지급이력 세 조회는 서로 의존이 없다. 순차로 던지면 왕복이 그대로 합산된다
    // (실측: 백엔드 3회 20.0ms 인데 프래그먼트는 49.8ms).
    var snapshotsFuture =
        supplyRemote(() -> emptyIfNull(monthlyDividendSnapshotClient.findSnapshots(params)));
    var profilesFuture =
        supplyRemote(
            () ->
                emptyIfNull(
                    monthlyDividendProfileClient.findProfiles(new LinkedMultiValueMap<>())));
    var payoutsFuture =
        supplyRemote(
            () ->
                emptyIfNull(monthlyDividendPayoutClient.findPayouts(new LinkedMultiValueMap<>())));
    // 스냅샷의 보유 수량은 사람이 갱신한 시점의 값이다. 원장의 현재 수량은 항상 정확하므로 함께 읽어
    // 두 값이 어긋난 종목을 화면에서 짚어 준다(실측 2026-08-23: 8종목 중 7종목이 어긋났고 그 몫이
    // 표시 합계의 80.2%였다).
    MultiValueMap<String, String> holdingParams = new LinkedMultiValueMap<>();
    holdingParams.add("userId", userId.toString());
    holdingParams.add("groupBy", TradeProfitRequestGroup.STOCKITEM.name());
    var holdingsFuture =
        supplyRemote(() -> emptyIfNull(tradeProfitClient.calculateProfit(holdingParams)));

    List<MonthlyDividendSnapshotResponse> snapshots = joinRemote(snapshotsFuture);
    model.addAttribute("hasSnapshots", !snapshots.isEmpty());

    // 심볼 -> 지급 시기(MID_MONTH/MONTH_END)
    Map<String, String> windowBySymbol = new HashMap<>();
    for (MonthlyDividendProfileResponse profile : joinRemote(profilesFuture)) {
      String sym = normalizeSymbol(profile.stockItemSymbol());
      if (sym != null && profile.payoutWindow() != null) {
        windowBySymbol.putIfAbsent(sym, profile.payoutWindow());
      }
    }

    // 실제 지급이력(payDate)에서 시기별 대표 지급일(day-of-month)을 산출한다.
    List<MonthlyDividendPayoutResponse> payouts = joinRemote(payoutsFuture);
    int midRepDay = representativePayDay(payouts, windowBySymbol, "MID_MONTH", 15);
    int endRepDay = representativePayDay(payouts, windowBySymbol, "MONTH_END", 31);

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
    String nextPayDateLabel = nextPayDate != null ? monthDayLabel(nextPayDate) : "";

    model.addAttribute("nextWindowLabel", nextWindowLabel);
    model.addAttribute("nextWindowIsMid", "MID_MONTH".equals(nextWindow));
    model.addAttribute("nextPayDateLabel", nextPayDateLabel);
    model.addAttribute("windowTotal", windowTotal);
    model.addAttribute("topDividendSnapshots", windowRows.stream().limit(5).toList());

    // 스냅샷 수량 vs 원장의 현재 수량. 1주당 배당(평균 1년)은 스냅샷 그대로 두고 수량만 현재 값으로
    // 바꿔 합계를 다시 낸다 — 스냅샷의 expectedMonthlyDividend 가 정확히
    // averageMonthlyDividendPerShare1y x heldQuantity 임을 실측으로 확인했다(8건 전부 일치).
    Map<UUID, Integer> currentQuantityByStockItem = new HashMap<>();
    for (TradeProfit profit : joinRemote(holdingsFuture)) {
      if (profit.stockItemId() != null) {
        currentQuantityByStockItem.merge(
            profit.stockItemId(), profit.holdingQuantity(), Integer::sum);
      }
    }
    var currentQuantitySummary =
        net.luversof.web.gate.stock.service.MonthlyDividendCalculator.currentQuantitySummary(
            windowRows, currentQuantityByStockItem);
    model.addAttribute("staleQuantityCount", currentQuantitySummary.staleCount());
    model.addAttribute("currentQuantityTotal", currentQuantitySummary.totalAtCurrentQuantity());
    // 이 카드의 예상 배당은 저장된 기준 데이터(스냅샷)로 계산된다. 스냅샷은 사람이 갱신해야 하고
    // 종목마다 시점이 다르다(실측 2026-08-22: 2026-07-20 ~ 2026-08-04 로 최대 33 일 지연, 그만큼
    // 보유 수량이 옛날 값이라 예상 월배당이 1.66% 낮게 잡혔다). 어느 시점 데이터인지 밝히지 않으면
    // 사용자는 이 숫자를 현재값으로 읽는다.
    // 화면에 넘기는 목록은 상위 5 개뿐이라 합계를 만든 전체 행에서 구한다.
    model.addAttribute(
        "dividendAsOfOldest",
        windowRows.stream()
            .map(MonthlyDividendSnapshotResponse::asOfDate)
            .filter(java.util.Objects::nonNull)
            .min(java.time.LocalDate::compareTo)
            .orElse(null));
    model.addAttribute(
        "dividendAsOfNewest",
        windowRows.stream()
            .map(MonthlyDividendSnapshotResponse::asOfDate)
            .filter(java.util.Objects::nonNull)
            .max(java.time.LocalDate::compareTo)
            .orElse(null));

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
  /**
   * 그 시기의 대표 지급일(day-of-month). 지급이력 전체에서 가장 자주 나온 날을 쓴다.
   *
   * <p>예전에는 심볼별 <b>최신 지급 1건</b>의 일자를 평균했다. 그런데 실제 지급일은 영업일 보정 때문에 달마다 흔들린다(실측: 중순 지급은 17/19/20 일,
   * 월말 지급은 2~7 일 사이를 오간다). 최신 한 달만 보면 그 달이 어쩌다 늦게 지급된 달이면 예상일이 통째로 밀린다.
   *
   * <p>어느 추정이 나은지 실제 지급이력으로 되짚어 봤다(그 달 이전 자료만 써서 그 달을 맞히는 방식, 중순 22 개월 + 월말 51 개월):
   *
   * <pre>
   *                        중순: 평균오차 / 정확일치      월말: 평균오차 / 정확일치
   *   최신 1개월(예전)        1.45 일 /  5 of 22          1.57 일 /  7 of 51
   *   전체 최빈값(현재)        0.86 일 / 12 of 22          1.33 일 / 14 of 51
   * </pre>
   *
   * <p>중순은 평균오차가 41%, 월말은 15% 줄고, 정확히 맞히는 달이 5→12 · 7→14 로 늘었다. 이 수치는 지금 구현과 같은 규칙(지급 행 단위로 세고 동률이면
   * 최근 등장 우선)으로 되짚은 값이다. 참고로 최근 3/6 개월 중앙값도 예전보다는 나았지만 최빈값보다 정확일치가 적었다.
   *
   * <p>이 사용자 데이터 기준 대표일은 중순 20 → 17 일, 월말 4 → 2 일로 바뀐다.
   *
   * <p>같은 횟수인 날이 여럿이면 최근에 나온 쪽을 쓴다(일정이 옮겨간 경우를 따라가기 위해서다).
   *
   * <p><b>되짚을 때는 반드시 "달 단위" 로 한다.</b> 지급 행 하나하나를 두고 "그 직전 행까지의 자료로 이 행을 맞히기" 로 재면 <b>같은 창의 다른 종목이
   * 이미 그 달에 지급한 행</b>을 쓰게 된다. 예측 시점에는 알 수 없는 정보다. 실측 2026-08-23: 그렇게 재면 "직전 지급일" 이 평균오차 0.62 일 /
   * 정확일치 130 of 200 으로 최빈값(1.16 일 / 80 of 200)을 크게 이기는 것처럼 나온다. 달 단위로 다시 재면 정반대다(아래).
   *
   * <p>자료가 늘어 2026-08-23 에 같은 방식(달 단위)으로 다시 쟀다. 중순 27 달 · 월말 56 달:
   *
   * <pre>
   *                        중순: 평균오차 / 정확일치      월말: 평균오차 / 정확일치
   *   직전 달 지급일           1.37 일 /  6 of 27          1.57 일 /  7 of 56
   *   전체 중앙값             1.11 일 /  9 of 27          1.04 일 / 11 of 56
   *   최근 3 달 중앙값         1.11 일 /  9 of 27          1.27 일 /  7 of 56
   *   전체 최빈값(현재)        0.93 일 / 14 of 27          1.34 일 / 15 of 56
   *   최근 24 건 최빈값        0.93 일 / 14 of 27          1.29 일 / 16 of 56
   * </pre>
   *
   * <p>결론은 그대로다 &mdash; 정확일치 기준으로 최빈값이 가장 낫다. 창을 최근 24 건으로 좁히면 월말이 아주 조금 나아지지만(1.34 → 1.29 일, 15 →
   * 16 달) 중순은 같아서 바꿀 만한 차이가 아니다.
   *
   * <p>종목별 대표일도 재 봤는데 <b>더 나빴다</b>(평균오차 1.28 vs 1.16 일, 정확일치 68 vs 80 of 200). 같은 창의 ETF 들이 같은 배분
   * 일정을 따르므로 표본을 합치는 편이 안정적이다.
   */
  static int representativePayDay(
      List<MonthlyDividendPayoutResponse> payouts,
      Map<String, String> windowBySymbol,
      String window,
      int fallback) {
    Map<Integer, Integer> countByDay = new HashMap<>();
    Map<Integer, LocalDate> latestByDay = new HashMap<>();
    for (MonthlyDividendPayoutResponse payout : payouts) {
      if (payout == null || payout.payDate() == null) {
        continue;
      }
      String symbol = normalizeSymbol(payout.stockItemSymbol());
      if (symbol == null || !window.equals(windowBySymbol.get(symbol))) {
        continue;
      }
      int day = payout.payDate().getDayOfMonth();
      countByDay.merge(day, 1, Integer::sum);
      latestByDay.merge(day, payout.payDate(), (a, b) -> a.isAfter(b) ? a : b);
    }
    return countByDay.entrySet().stream()
        .max(
            Comparator.comparingInt(Map.Entry<Integer, Integer>::getValue)
                .thenComparing(entry -> latestByDay.get(entry.getKey())))
        .map(Map.Entry::getKey)
        .orElse(fallback);
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

  /**
   * 계좌 원금. 직접 입력한 원금이 있으면 그것을, 없으면 보유원가를 쓴다.
   *
   * <p>그래서 계좌마다 <b>기준이 다를 수 있다</b> &mdash; 화면은 그 차이를 "수동 원금 반영" 줄로 밝힌다. 실측 2026-08-23: 이 사용자의 6 개
   * 계좌는 현재 전부 폴백이고(직접 입력값이 비어 있다) 합계가 632,223,826 원이다. ISA·연금저축1·연금저축2 에 각각 60,000,000 / 12,000,000
   * / 18,000,000 을 넣으면 621,595,903 원이 되어 10,627,923 원 줄어든다.
   */
  static BigDecimal accountPrincipal(BigDecimal manualPrincipal, BigDecimal holdingCost) {
    if (manualPrincipal != null) {
      return manualPrincipal;
    }
    return holdingCost != null ? holdingCost : BigDecimal.ZERO;
  }

  /**
   * 계좌 평가손익. 직접 입력한 원금이 있으면 그 원금 대비로 다시 낸다.
   *
   * <p>원금을 바꿔 놓고 손익은 그대로 두면 "평가액 - 원금 = 손익" 이 깨진다.
   */
  static BigDecimal accountEvaluationProfit(
      BigDecimal manualPrincipal, BigDecimal evaluationAmount, BigDecimal defaultEvaluationProfit) {
    if (manualPrincipal == null) {
      return defaultEvaluationProfit != null ? defaultEvaluationProfit : BigDecimal.ZERO;
    }
    BigDecimal amount = evaluationAmount != null ? evaluationAmount : BigDecimal.ZERO;
    return amount.subtract(manualPrincipal);
  }
}
