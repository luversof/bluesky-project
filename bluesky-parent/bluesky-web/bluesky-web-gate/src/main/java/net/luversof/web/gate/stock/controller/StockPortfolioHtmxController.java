package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.domain.TradeProfitAggregator;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.AssetStatusAccountHoldingView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DataFirstDateClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
// /stock/htmx/portfolio(tabsPortfolio.jte) 는 2026-09-09 에 지웠다 - 어떤 화면도 부르지 않는 서버 왕복 정렬 시절의 잔재였다
// (UnreachableEndpointTest 가 2026-08-23 부터 '알려진 죽은 경로' 로 들고 있던 것). 지금 보유 표는 assetStatus 다.
public class StockPortfolioHtmxController extends StockBaseHtmxController {

  private final DataFirstDateClient dataFirstDateClient;

  private final net.luversof.web.gate.stock.support.StockAsyncSupport async;

  public StockPortfolioHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      DataFirstDateClient dataFirstDateClient,
      MessageSource messageSource,
      net.luversof.web.gate.stock.support.StockAsyncSupport async) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
    this.dataFirstDateClient = dataFirstDateClient;
    this.async = async;
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-status")
  public String assetStatus(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return loginRequiredView(model);
    request.setUserId(userId);
    // 이 화면은 '지금 보유'다. 기간이 실려 오면 api-stock 은 평가(현재가/평가금액/평가손익)를 아예
    // 계산하지 않으므로 수량과 평단만 있고 평가가 0 인 모순된 표가 된다(실측: 수량 5,043 · 평단
    // 71,887 인데 현재가 0). 지금 화면에서는 기간을 보내지 않지만, 엔드포인트를 직접 부르면 그대로
    // 드러나므로 여기서 떨어낸다. 기간별 손익은 거래/자산추이 화면이 담당한다.
    request.setStartDate(null);
    request.setEndDate(null);

    // 이 프래그먼트는 원격 호출 8개를 순차로 던지고 있었다(계좌/종목 조회가 이름 붙이기 안에서
    // 두 번씩 더 나갔다). 이름 재료는 여기서 한 번만 읽어 두 손익 조회에 함께 넘기고,
    // 서로 의존이 없는 네 호출은 동시에 던진다.
    var stockItemsFuture =
        async.supply(
            () ->
                emptyIfNull(
                    (List<net.luversof.web.gate.stock.domain.StockItem>)
                        stockItemClient.getStockItems()));
    var accountsFuture = async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));

    // 위 portfolio 와 같은 이유로, 계좌 필터가 있으면 보내기 전에 이 사용자 계좌로 좁힌다.
    boolean emptyAccountSelection = narrowToOwnedAccounts(request, accountsFuture);

    var profitParams = request.toParams();
    var stockGroupedRequest = copyTradeProfitRequest(request);
    stockGroupedRequest.setGroupBy(
        net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM);
    var stockGroupedParams = stockGroupedRequest.toParams();
    var rawProfitFuture =
        emptyAccountSelection
            ? null
            : async.supply(() -> emptyIfNull(tradeProfitClient.calculateProfit(profitParams)));
    // 종목별 누적 배당(세후). 종목 상세가 쓰는 것과 같은 잣대다.
    // 목록 전체(실측 202행 약 78KB)를 받지 않고 종목별 합계만 받는다(이 사용자 18행).
    var dividendByStockItemFuture =
        emptyAccountSelection
            ? null
            : async.supply(() -> dividendClient.findDividendTotalByStockItem(profitParams));
    var rawStockGroupedFuture =
        emptyAccountSelection
            ? null
            : async.supply(
                () -> emptyIfNull(tradeProfitClient.calculateProfit(stockGroupedParams)));
    // 종목별 최초 매수일. 보유 기간·연평균 수익률을 적으려면 처음 산 날이 필요한데, 원장 목록
    // (실측 2026-09-14: 258 행)을 다시 받는 대신 종목별 집계만 받는다 - 배당 합계와 같은 방식이다.
    var firstBuyDateFuture =
        emptyAccountSelection
            ? null
            : async.supply(() -> tradeClient.findFirstBuyDateByStockItem(profitParams));

    List<net.luversof.web.gate.stock.domain.StockItem> stockItemList =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(stockItemsFuture);
    var accountList = net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountsFuture);
    var tradeProfitNames = toTradeProfitNames(accountList, stockItemList);

    List<TradeProfit> enrichedList =
        new ArrayList<>(
            rawProfitFuture == null
                ? List.<TradeProfit>of()
                : enrichTradeProfits(
                    net.luversof.web.gate.stock.support.StockAsyncSupport.join(rawProfitFuture),
                    userId,
                    tradeProfitNames));
    enrichedList.removeIf(tp -> tp.holdingQuantity() == 0);
    BigDecimal totalEvaluationAmount =
        enrichedList.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<UUID, BigDecimal> accountPrincipalOverrideMap =
        accountList.stream()
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

    // 계좌별 집계
    // 표시용 맵의 키는 계좌 id 다. 예전에는 계좌명을 키로 써서, 같은 이름의 계좌가 둘이면
    // 뒤에 담긴 쪽이 앞의 것을 덮어 한 행이 화면에서 사라졌다(집계 자체는 id 기준이라 정확했다).
    Map<UUID, TradeProfit> accountTotalMap = new LinkedHashMap<>();
    Map<UUID, BigDecimal> accountProfitBasisMap = new LinkedHashMap<>();
    // 원금이 계좌 설정의 수동 입력값인 계좌. 화면이 계산 원가와 구분해 표시한다.
    java.util.Set<UUID> manualPrincipalAccountIds = new java.util.LinkedHashSet<>();
    Map<UUID, List<AssetStatusAccountHoldingView>> accountHoldingMap = new LinkedHashMap<>();
    enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::accountId)).entrySet().stream()
        .sorted(
            Comparator.comparing(
                entry -> {
                  List<TradeProfit> list = entry.getValue();
                  if (list == null || list.isEmpty()) {
                    return null;
                  }
                  return list.get(0).accountName();
                },
                Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(
            entry -> {
              UUID accountId = entry.getKey();
              List<TradeProfit> list = entry.getValue();
              TradeProfit first = list.get(0);
              String accountName = first.accountName();
              List<TradeProfit> sortedHoldings =
                  list.stream()
                      .sorted(
                          Comparator.comparing(
                                  TradeProfit::evaluationAmount,
                                  Comparator.nullsLast(Comparator.reverseOrder()))
                              .thenComparing(
                                  TradeProfit::stockItemName,
                                  Comparator.nullsLast(Comparator.naturalOrder())))
                      .toList();
              var s = TradeProfitAggregator.aggregate(list);
              BigDecimal evaluationAmount =
                  s.evaluationAmount() != null ? s.evaluationAmount() : BigDecimal.ZERO;
              BigDecimal holdingCost =
                  resolveCurrentHoldingCost(
                      evaluationAmount, s.evaluationProfit(), s.avgBuyPrice(), s.holdingQuantity());
              BigDecimal defaultEvaluationProfit =
                  s.evaluationProfit() != null ? s.evaluationProfit() : BigDecimal.ZERO;
              BigDecimal defaultPrincipal = holdingCost;
              BigDecimal manualPrincipal =
                  accountId != null ? accountPrincipalOverrideMap.get(accountId) : null;
              BigDecimal profitBasis = manualPrincipal != null ? manualPrincipal : defaultPrincipal;
              accountProfitBasisMap.put(accountId, profitBasis);
              if (manualPrincipal != null && accountId != null) {
                manualPrincipalAccountIds.add(accountId);
              }
              accountHoldingMap.put(
                  accountId,
                  buildAccountHoldingViews(
                      sortedHoldings, evaluationAmount, totalEvaluationAmount));
              accountTotalMap.put(
                  accountId,
                  TradeProfit.ofAccountStatus(
                      accountName,
                      evaluationAmount,
                      defaultEvaluationProfit,
                      s.realizedProfit(),
                      holdingCost,
                      s.totalSellProceeds()));
            });

    // 종목별 집계 (계좌 무시)
    List<TradeProfit> stockGroupedList =
        new ArrayList<>(
            rawStockGroupedFuture == null
                ? List.<TradeProfit>of()
                : enrichTradeProfits(
                    net.luversof.web.gate.stock.support.StockAsyncSupport.join(
                        rawStockGroupedFuture),
                    userId,
                    tradeProfitNames));
    // 걸러내기 전에 '이미 다 판 종목' 몫을 따로 센다. 이 표의 합계는 보유 종목만이라 전체 누적보다 작은데,
    // 얼마나 작은지는 화면 어디에도 없었다 - 실측 2026-09-11: 실현손익 합계가 자산 현황 140,350,295 인데
    // 대시보드·매매·자산 성장은 225,630,135 를 적는다(차이 85,279,840, 37.8%). 그 차이를 숫자로 밝힌다.
    List<TradeProfit> soldOutList =
        stockGroupedList.stream().filter(tp -> tp.holdingQuantity() == 0).toList();
    java.util.Set<UUID> soldOutStockItemIds =
        soldOutList.stream()
            .map(TradeProfit::stockItemId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    BigDecimal soldOutRealizedProfit =
        soldOutList.stream()
            .map(TradeProfit::realizedProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    // getStockGroupedTradeProfits(request, false) 와 같은 순서로 보유량 0 을 먼저 걸러낸다.
    stockGroupedList.removeIf(tp -> tp.holdingQuantity() == 0);
    List<TradeProfit> stockAggregated =
        stockGroupedList.stream()
            .map(this::toAssetStatusStock)
            .sorted(
                Comparator.comparing(
                        TradeProfit::evaluationAmount,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        TradeProfit::stockItemName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    BigDecimal totalEvaluationProfit =
        stockAggregated.stream()
            .map(TradeProfit::evaluationProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("accountTotalMap", accountTotalMap);
    model.addAttribute("accountProfitBasisMap", accountProfitBasisMap);
    // 수동 원금이 적용된 계좌를 화면이 표시할 수 있게 넘긴다. 표시가 없으면 그 값이
    // 사라져도(계좌 설정에는 갱신 UI 가 없다) 수익률만 조용히 달라진다.
    model.addAttribute("manualPrincipalAccountIds", manualPrincipalAccountIds);
    model.addAttribute("accountHoldingMap", accountHoldingMap);
    model.addAttribute("stockItemList", stockItemList);
    model.addAttribute("stockAggregated", stockAggregated);
    // 종목별 실현손익·누적배당·합산손익. 세 값은 종목 상세에만 있어서 종목끼리 견줄 수가 없었다.
    // 이 화면은 기간이 없는 '지금 보유' 스냅샷이라 전 기간이 대상이고, 전 기간의 평가 변동은 곧
    // 현재 평가손익이므로(기초가 0) 종목 상세의 '전체' 와 어긋나지 않는다.
    java.util.Map<UUID, BigDecimal> dividendByStockItem =
        dividendByStockItemFuture == null
            ? java.util.Map.<UUID, BigDecimal>of()
            : emptyIfNullMap(
                net.luversof.web.gate.stock.support.StockAsyncSupport.join(
                    dividendByStockItemFuture));
    var stockProfitBreakdown =
        net.luversof.web.gate.stock.util.StockCombinedProfitUtil.byStockItem(
            stockAggregated, dividendByStockItem);
    // 이 표에 없는 종목 = 다 판 종목 + '거래는 없는데 배당만 있는 종목'. 후자를 빼면 합이 맞지 않는다 -
    // 실측 2026-09-11: 하나금융지주가 거래 행 없이 배당 2,100 만 있어, 다 판 34 종목만 세면
    // 59,067,537 + 6,582,497 = 65,650,034 로 전체 배당 65,652,134 에 2,100 모자랐다.
    java.util.Set<UUID> heldStockItemIds =
        stockGroupedList.stream()
            .map(TradeProfit::stockItemId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    java.util.Set<UUID> notListedStockItemIds = new java.util.LinkedHashSet<>(soldOutStockItemIds);
    dividendByStockItem.forEach(
        (stockItemId, amount) -> {
          if (stockItemId != null && !heldStockItemIds.contains(stockItemId)) {
            notListedStockItemIds.add(stockItemId);
          }
        });
    BigDecimal soldOutDividend =
        notListedStockItemIds.stream()
            .map(dividendByStockItem::get)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    model.addAttribute("soldOutStockCount", notListedStockItemIds.size());
    model.addAttribute("soldOutRealizedProfit", soldOutRealizedProfit);
    model.addAttribute("soldOutDividend", soldOutDividend);
    model.addAttribute("stockProfitBreakdown", stockProfitBreakdown);
    model.addAttribute(
        "stockProfitBreakdownTotal",
        net.luversof.web.gate.stock.util.StockCombinedProfitUtil.total(stockProfitBreakdown));
    model.addAttribute("totalEvaluationAmount", totalEvaluationAmount);
    model.addAttribute("totalEvaluationProfit", totalEvaluationProfit);

    // 화면의 "현재가"는 마지막으로 수집된 종가다. 오늘 시세가 아직 없으면 며칠 전 값일 수 있어
    // 어느 날 기준인지 함께 보여준다(보유 종목 중 가장 최근 일자).
    java.time.LocalDate priceBasisDate =
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.latestPriceBasisDate(enrichedList);
    model.addAttribute("priceBasisDate", priceBasisDate);

    // 보유 기간의 끝은 '오늘'이다. 요청 존으로 잡는다 - 서버 존(UTC)으로 잡으면 KST 오전에 하루가 덜 센다.
    java.time.ZoneId holdingZone = resolveZoneIdOrDefault(request.getTimeZone());
    model.addAttribute("holdingBasisDate", java.time.LocalDate.now(holdingZone));
    model.addAttribute(
        "firstBuyDateByStockItem",
        firstBuyDateFuture == null
            ? java.util.Map.<UUID, java.time.LocalDate>of()
            : emptyIfNullMap(
                net.luversof.web.gate.stock.support.StockAsyncSupport.join(firstBuyDateFuture)));
    return "stock/htmx/fragments/assetStatus";
  }

  private List<AssetStatusAccountHoldingView> buildAccountHoldingViews(
      List<TradeProfit> holdings,
      BigDecimal accountEvaluationAmount,
      BigDecimal totalEvaluationAmount) {
    if (holdings == null || holdings.isEmpty()) {
      return List.of();
    }

    List<AssetStatusAccountHoldingView> views = new ArrayList<>(holdings.size());
    for (TradeProfit holding : holdings) {
      BigDecimal evaluationAmount =
          holding.evaluationAmount() != null ? holding.evaluationAmount() : BigDecimal.ZERO;
      BigDecimal buyAmount = resolveCurrentHoldingCost(holding);
      BigDecimal averageBuyPrice = resolveHoldingAverageBuyPrice(holding, buyAmount);
      BigDecimal evaluationProfit =
          holding.evaluationProfit() != null ? holding.evaluationProfit() : BigDecimal.ZERO;

      views.add(
          new AssetStatusAccountHoldingView(
              holding.stockItemId(),
              holding.stockItemName(),
              holding.holdingQuantity(),
              averageBuyPrice,
              holding.currentPrice(),
              evaluationAmount,
              buyAmount,
              evaluationProfit,
              percentage(evaluationProfit, buyAmount),
              percentage(evaluationAmount, accountEvaluationAmount),
              percentage(evaluationAmount, totalEvaluationAmount)));
    }

    return views;
  }

  private BigDecimal resolveHoldingAverageBuyPrice(TradeProfit holding, BigDecimal buyAmount) {
    if (holding == null) {
      return BigDecimal.ZERO;
    }

    if (holding.averageBuyPrice() != null) {
      return holding.averageBuyPrice();
    }

    if (holding.averageBuyPriceNet() != null) {
      return holding.averageBuyPriceNet();
    }

    if (holding.holdingQuantity() > 0) {
      return buyAmount.divide(
          BigDecimal.valueOf(holding.holdingQuantity()), 4, java.math.RoundingMode.HALF_UP);
    }

    return BigDecimal.ZERO;
  }

  private static <K, V> java.util.Map<K, V> emptyIfNullMap(java.util.Map<K, V> value) {
    return value != null ? value : java.util.Map.of();
  }

  private TradeProfit toAssetStatusStock(TradeProfit profit) {
    BigDecimal holdingCost =
        resolveCurrentHoldingCost(
            profit.evaluationAmount(),
            profit.evaluationProfit(),
            profit.averageBuyPrice(),
            profit.holdingQuantity());
    return TradeProfit.ofStockStatus(
        profit.stockItemId(),
        profit.stockItemName(),
        profit.averageBuyPrice(),
        profit.holdingQuantity(),
        profit.currentPrice(),
        profit.evaluationAmount(),
        profit.evaluationProfit(),
        profit.realizedProfit(),
        holdingCost);
  }

  /**
   * 백분율. <b>100 을 먼저 곱한 뒤</b> 나눈다 - 순서를 바꾸면 표시 자릿수보다 굵게 미리 반올림된다.
   *
   * <p>2026-09-12 까지는 {@code divide(base, 4).multiply(100)} 이라 <b>백분율이 소수 2 자리로 잘린 뒤</b> 화면이 다시 1
   * 자리로 반올림했다. 실측 2026-09-12(자산 현황, 계좌별 보유 종목 상세): 연금저축2 의 TIGER 리츠부동산인프라 가 8,847,600 /
   * 1,622,109,770 = 0.5454% 인데 0.0055 x 100 = 0.55% 를 거쳐 <b>0.6%</b> 로 나갔다(옳게 하면 0.5%). 같은 화면 37 행
   * 중 이 한 줄이 걸렸다 - 나머지는 잔차가 0.x5 경계에 닿지 않았을 뿐이다.
   *
   * <p>{@code StockDividendHtmxController.percentage} 는 처음부터 이 순서였다. 같은 규칙으로 맞춘다.
   */
  private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
    if (amount == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    return amount.multiply(BigDecimal.valueOf(100)).divide(base, 4, java.math.RoundingMode.HALF_UP);
  }
}
