package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;

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
  @GetMapping("/portfolio")
  public String portfolio(
      TradeProfitRequest request,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "ACCOUNT") String viewGroupBy,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return loginRequiredView(model);
    }
    request.setUserId(userId);
    // 이 화면은 '지금 보유'다. 기간이 실려 오면 api-stock 은 평가(현재가/평가금액/평가손익)를 아예
    // 계산하지 않으므로 수량과 평단만 있고 평가가 0 인 모순된 표가 된다(실측: 수량 5,043 · 평단
    // 71,887 인데 현재가 0). 지금 화면에서는 기간을 보내지 않지만, 엔드포인트를 직접 부르면 그대로
    // 드러나므로 여기서 떨어낸다. 기간별 손익은 거래/자산추이 화면이 담당한다.
    request.setStartDate(null);
    request.setEndDate(null);
    // 날짜 범위 네비게이션의 하한(minDate)용 최초 거래일.
    // 전체 거래를 내려받아 min() 하던 것을 DB 집계 엔드포인트 1회 호출로 대체했다.
    ZoneId dataZone = resolveZoneIdOrDefault(request.getTimeZone());

    // 이 프래그먼트의 원격 호출 4개는 서로 의존이 없는데 완전히 줄을 서 있었다
    // (실측: 최초거래일 0ms -> 손익 5ms -> 종목 13ms -> 계좌 18ms). 손익 조회 결과에 이름을 입힐
    // 재료(계좌/종목)는 손익과 무관하게 미리 읽을 수 있으므로 넷을 함께 던진다.
    // 같은 파일의 asset-status 가 이미 쓰는 방식이다.
    boolean stockView = "STOCK".equals(viewGroupBy);
    var dataFirstDateFuture = async.supply(() -> dataFirstDateClient.findDataFirstDate(userId));
    var namesAccountsFuture =
        async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));
    var namesStockItemsFuture = async.supply(() -> emptyIfNull(stockItemClient.getStockItems()));

    // 계좌 필터가 있으면 보내기 전에 이 사용자 계좌로 좁힌다(없는 id 하나에 화면이 통째로 죽는 것을 막는다).
    // 필터가 없는 요청은 좁힐 것이 없으므로 계좌 응답을 기다리지 않고 그대로 던진다.
    boolean emptyAccountSelection = narrowToOwnedAccounts(request, namesAccountsFuture);

    // getEnrichedTradeProfits(request, STOCKITEM, names) 와 같은 분기 조건을 그대로 쓴다
    // (이미 STOCKITEM 이면 원본을 그대로 써야 파라미터가 완전히 동일하다).
    TradeProfitRequest profitRequest = request;
    if (stockView
        && request.getGroupBy()
            != net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM) {
      profitRequest = copyTradeProfitRequest(request);
      profitRequest.setGroupBy(
          net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM);
    }
    var profitParams = profitRequest.toParams();
    var rawPortfolioProfitFuture =
        emptyAccountSelection
            ? null
            : async.supply(() -> emptyIfNull(tradeProfitClient.calculateProfit(profitParams)));

    Instant tradeFirstInstant =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dataFirstDateFuture)
            .tradeFirstDate();
    LocalDate dataFirstDate =
        tradeFirstInstant != null ? tradeFirstInstant.atZone(dataZone).toLocalDate() : null;

    // 이름 붙이기는 메시지 조회를 타므로 반드시 요청 스레드에서 한다.
    var portfolioNames =
        toTradeProfitNames(
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(namesAccountsFuture),
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(namesStockItemsFuture));
    List<TradeProfit> enrichedProfits =
        rawPortfolioProfitFuture == null
            ? List.of()
            : enrichTradeProfits(
                net.luversof.web.gate.stock.support.StockAsyncSupport.join(
                    rawPortfolioProfitFuture),
                userId,
                portfolioNames);

    // STOCK 뷰에서는 아래 종목 그룹 조회 결과만 쓰므로, 계좌 기준 조회를 미리 하지 않는다.
    // (기존에는 조회 후 통째로 덮어써서 calculateProfit/종목/계좌 조회가 낭비됐다.)
    // 이 표의 "현재가"도 asset-status 와 같이 마지막으로 수집된 종가다. 어느 날 기준인지 밝히지 않으면
    // 실시간 시세로 오해할 수 있어 같은 표기를 붙인다(계산·문구 모두 asset-status 와 동일).
    // 종목 뷰 변환(toPortfolioStock) 뒤에는 이 값이 남지 않으므로 변환 전에 구한다.
    model.addAttribute(
        "priceBasisDate",
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.latestPriceBasisDate(enrichedProfits));

    List<TradeProfit> enrichedList = new ArrayList<>(enrichedProfits);
    enrichedList.removeIf(tp -> tp.holdingQuantity() == 0);
    if (stockView) {
      enrichedList =
          enrichedList.stream()
              .map(this::toPortfolioStock)
              .collect(Collectors.toCollection(ArrayList::new));
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

      if (comparator != null && "desc".equalsIgnoreCase(direction)) {
        comparator = comparator.reversed();
      }
    }

    Map<String, TradeProfit> accountTotalMap = new HashMap<>();
    if ("ACCOUNT".equals(viewGroupBy)) {
      Comparator<TradeProfit> accountComparator =
          Comparator.comparing(
              TradeProfit::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
      comparator =
          (comparator == null) ? accountComparator : accountComparator.thenComparing(comparator);

      // 계좌는 id 로 묶는다. 이름으로 묶으면 같은 이름의 계좌가 둘일 때 한 행으로 합쳐진다.
      Map<UUID, List<TradeProfit>> byAccount =
          enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::accountId));

      byAccount.forEach(
          (groupAccountId, list) -> {
            if (list.isEmpty()) return;

            String accountName = list.get(0).accountName();
            var s = TradeProfitAggregator.aggregate(list);
            accountTotalMap.put(
                accountName,
                TradeProfit.ofPortfolioAccount(
                    accountName,
                    s.totalBuyAmount(),
                    s.totalSellQuantity(),
                    s.totalSellAmount(),
                    s.realizedProfit(),
                    s.holdingQuantity(),
                    s.evaluationAmount(),
                    s.evaluationProfit(),
                    s.totalProfit(),
                    s.totalBuyFee(),
                    s.totalSellFee(),
                    s.totalSellTax(),
                    s.totalBuyCost(),
                    s.totalSellProceeds(),
                    s.realizedProfitNet(),
                    s.evaluationProfitNet(),
                    s.totalProfitNet()));
          });
    }

    if (comparator != null) {
      enrichedList.sort(comparator);
    }

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
    var rawStockGroupedFuture =
        emptyAccountSelection
            ? null
            : async.supply(
                () -> emptyIfNull(tradeProfitClient.calculateProfit(stockGroupedParams)));

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
    model.addAttribute("totalEvaluationAmount", totalEvaluationAmount);
    model.addAttribute("totalEvaluationProfit", totalEvaluationProfit);

    // 화면의 "현재가"는 마지막으로 수집된 종가다. 오늘 시세가 아직 없으면 며칠 전 값일 수 있어
    // 어느 날 기준인지 함께 보여준다(보유 종목 중 가장 최근 일자).
    java.time.LocalDate priceBasisDate =
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.latestPriceBasisDate(enrichedList);
    model.addAttribute("priceBasisDate", priceBasisDate);
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

  private TradeProfit toPortfolioStock(TradeProfit profit) {
    return TradeProfit.ofPortfolioStock(
        profit.stockItemId(),
        profit.stockItemName(),
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
        profit.totalProfitNet());
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

  private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
    if (amount == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    return amount.divide(base, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }
}
