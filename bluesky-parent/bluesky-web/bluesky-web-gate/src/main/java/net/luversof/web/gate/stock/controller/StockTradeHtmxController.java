package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.common.menu.domain.Pagination;
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.domain.TradeProfitAggregator;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.ActivityFilterIdsClient;
import net.luversof.web.gate.stock.httpexchange.DataFirstDateClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.support.StockAsyncSupport;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockTradeHtmxController extends StockBaseHtmxController {

  private final DataFirstDateClient dataFirstDateClient;

  private final ActivityFilterIdsClient activityFilterIdsClient;

  private final net.luversof.web.gate.stock.support.StockAsyncSupport async;

  public StockTradeHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      DataFirstDateClient dataFirstDateClient,
      ActivityFilterIdsClient activityFilterIdsClient,
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
    this.activityFilterIdsClient = activityFilterIdsClient;
    this.async = async;
  }

  /** 거래/배당 중 이른 일자 (날짜 선택기 하한). 전체 이력 대신 집계 엔드포인트 1회로 구한다. */
  private LocalDate resolveDataFirstDate(UUID userId, ZoneId zone, boolean includeDividend) {
    var response = dataFirstDateClient.findDataFirstDate(userId);
    Instant first = response.tradeFirstDate();
    if (includeDividend
        && response.dividendFirstDate() != null
        && (first == null || response.dividendFirstDate().isBefore(first))) {
      first = response.dividendFirstDate();
    }
    return first != null ? first.atZone(zone).toLocalDate() : null;
  }

  @BlueskyPreAuthorize
  /** 필터 드롭다운에 쓰는 '해당 기간에 등장한' 계좌/종목 id 집합. */
  private record AvailableIds(Set<UUID> accountIds, Set<UUID> stockItemIds) {}

  /**
   * 같은 값끼리 묶였을 때의 표시 순서.
   *
   * <p>{@code /api/trade} 는 <b>ORDER BY 가 없다</b>(TradeQuery 에 그렇게 적혀 있다). 그래서 행 순서는 저장 순서일 뿐이고, 한
   * 건만 고쳐도 그 행이 뒤로 밀린다. 목록은 {@code List.sort}(안정 정렬)로 한 열만 보고 정렬하므로 동점 행의 순서가 그대로 그 저장 순서를 따라간다
   * &mdash; 즉 편집 한 번에 화면 순서가 바뀐다.
   *
   * <p>실측 2026-08-24: 거래 250 건 중 <b>155 건(62.0%)</b> 이 같은 날짜에 다른 거래와 묶여 있다(한 날 최대 7 건). 컬럼 정렬은 더
   * 심해서 수수료 동점 147 건, 실현손익 동점 196 건이다.
   *
   * <p>종목명 &rarr; 매수/매도 &rarr; id 로 끊는다. id 까지 가면 어떤 두 행도 같지 않으므로 순서가 완전히 정해진다.
   */
  static final Comparator<TradeResponse> TRADE_TIE_BREAKER =
      Comparator.comparing(
              TradeResponse::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(TradeResponse::type, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(
              TradeResponse::id,
              Comparator.nullsLast(Comparator.comparing(java.util.UUID::toString)));

  @GetMapping("/trade/list")
  public String tradeList(
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) List<String> stockTagList,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String rangeMode,
      Model model) {

    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return loginRequiredView(model);
    }

    // If no date range provided by client, default to this year (ytd)
    Instant startInst = startDate;
    Instant endInst = endDate;
    // rangeMode 는 어떤 프리셋 버튼이 눌렸는지 알리는 화면 상태값이지 기간 그 자체가 아니다
    // (기간은 startDate/endDate 로 온다). 그런데 이 가드가 rangeMode 의 '존재'를 보는 바람에,
    // 날짜 없이 rangeMode 만 실려 오면 기본 기간이 적용되지 않고 전 기간이 조회됐다
    // (실측: rangeMode=ytd 인데 올해 106행이 아니라 전체 300행). 같은 가드를 asset-growth 는
    // 이미 '날짜가 없고 all 도 아니면 기본 적용'으로 쓰고 있어, 그쪽에 맞춘다.
    if (startInst == null && endInst == null && !"all".equalsIgnoreCase(rangeMode)) {
      ZoneId zone = resolveZoneIdOrDefault(timeZone);
      var preset = resolvePresetRange(rangeMode, zone);
      startInst = preset.start();
      endInst = preset.end();
      rangeMode = preset.mode();
    }

    TradeSearchRequest request = new TradeSearchRequest(userId, null, null, startInst, endInst);
    ZoneId zone = resolveZoneIdOrDefault(timeZone);

    // 이 화면 앞부분의 원격 호출 4개(거래목록/최초거래일/계좌/종목)와 아래 '기간 내 등장 계좌·종목'
    // 계산용 손익 조회는 서로 의존이 없다. 순차로 던지면 응답시간이 그대로 합산되므로 한꺼번에 던진다.
    // clientProvidedRange 는 원래 기본값 적용 뒤 값을 쓰므로 계산 시점을 그대로 유지한다.
    boolean clientProvidedRange =
        !((startDate == null || startDate.toEpochMilli() == 0)
            && (endDate == null || endDate.toEpochMilli() == 0)
            && (rangeMode == null || rangeMode.isBlank()));

    var tradeSearchParams = request.toParams();
    var tradesFuture = async.supply(() -> emptyIfNull(tradeClient.findTrades(tradeSearchParams)));
    var dataFirstDateFuture = async.supply(() -> resolveDataFirstDate(userId, zone, false));
    var accountsFuture = async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));
    var stockItemsFuture = async.supply(() -> emptyIfNull(stockItemClient.getStockItems()));

    // 아래 실현손익 조회는 지금까지 위 4개를 다 받은 뒤에야 출발했다. 그런데 계좌/종목 필터를 아무것도
    // 고르지 않았다면 retainAvailableIds 가 null 을 돌려주므로, 실현손익 요청 파라미터는 위 응답과
    // 무관하게 이미 확정되어 있다(기간도 원격 호출 전에 정해진다). 그 경우에는 기다릴 이유가 없어
    // 같이 던진다. 필터를 고른 요청은 예전처럼 교집합이 나온 뒤에 던진다.
    // 태그도 종목 필터다. 이 조건에서 빠뜨리면 태그만 고른 요청이 '필터 없음' 으로 취급돼 전체 손익을
    // 미리 던지고 그대로 재사용한다 — 매매 내역은 걸러지는데 계좌별/종목별 실현손익 요약만 전체가
    // 남는다(실측: stockTagList=ETF 일 때 목록은 9 종목인데 종목별 요약은 36 종목 그대로).
    // 같은 파일의 activity-list 조건은 이미 태그를 포함하고 있다.
    boolean noFilterSelected =
        (accountIdList == null || accountIdList.isEmpty())
            && (stockItemIdList == null || stockItemIdList.isEmpty())
            && (stockTagList == null || stockTagList.isEmpty());
    TradeProfitRequest earlyProfitRequest = null;
    java.util.concurrent.CompletableFuture<List<TradeProfit>> earlyRealizedFuture = null;
    java.util.concurrent.CompletableFuture<List<TradeProfit>> earlyStockGroupedFuture = null;
    if (noFilterSelected) {
      earlyProfitRequest = new TradeProfitRequest();
      earlyProfitRequest.setUserId(userId);
      earlyProfitRequest.setStartDate(startInst);
      earlyProfitRequest.setEndDate(endInst);
      earlyProfitRequest.setTimeZone(timeZone);
      var earlyParams = earlyProfitRequest.toParams();
      var earlyStockGrouped = copyTradeProfitRequest(earlyProfitRequest);
      earlyStockGrouped.setGroupBy(
          net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM);
      var earlyStockGroupedParams = earlyStockGrouped.toParams();
      earlyRealizedFuture =
          async.supply(() -> emptyIfNull(tradeProfitClient.calculateProfit(earlyParams)));
      earlyStockGroupedFuture =
          async.supply(
              () -> emptyIfNull(tradeProfitClient.calculateProfit(earlyStockGroupedParams)));
    }

    // 필터 목록에 쓰는 '해당 기간에 등장한 계좌/종목' 집합. id 만 필요하므로 이름은 붙이지 않는다.
    java.util.concurrent.CompletableFuture<AvailableIds> availableIdsFuture = null;
    if (clientProvidedRange) {
      // 예전에는 이 집합을 얻으려고 전체 거래 목록(80KB) 또는 손익 전체 계산을 다시 받아 id 만 뽑고 버렸다.
      // 집계 엔드포인트가 같은 집합을 2.8KB 로 준다(실측: 전체·YTD·최근 1개월·2024년 네 구간 id 집합 완전 일치).
      final Instant availStart = startDate;
      final Instant availEnd = endDate;
      availableIdsFuture =
          async.supply(
              () -> {
                var ids = activityFilterIdsClient.findFilterIds(userId, availStart, availEnd);
                return new AvailableIds(
                    new java.util.HashSet<>(emptyIfNull(ids.tradeAccountIds())),
                    new java.util.HashSet<>(emptyIfNull(ids.tradeStockItemIds())));
              });
    }

    List<TradeResponse> allFromApi = StockAsyncSupport.join(tradesFuture);
    LocalDate dataFirstDate = StockAsyncSupport.join(dataFirstDateFuture);

    List<Account> accountList = StockAsyncSupport.join(accountsFuture);
    Map<UUID, String> accountNames =
        accountList.stream()
            .collect(Collectors.toMap(Account::id, Account::name, (l, r) -> l, LinkedHashMap::new));

    List<StockItem> stockItemList = StockAsyncSupport.join(stockItemsFuture);
    // 이 요청에서 손익을 여러 번 계산하는데, 이름 맵의 재료는 이미 위에서 읽은 두 목록이다.
    TradeProfitNames tradeProfitNames = toTradeProfitNames(accountList, stockItemList);
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, stockItemIdList, stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    List<TradeResponse> enrichedAll =
        allFromApi.stream()
            .map(
                t ->
                    new TradeResponse(
                        t.id(),
                        t.accountId(),
                        t.stockItemId(),
                        stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown")),
                        t.type(),
                        t.quantity(),
                        t.price(),
                        t.fee(),
                        t.tax(),
                        t.amount(),
                        t.realizedProfit(),
                        t.tradeDate()))
            .collect(Collectors.toCollection(ArrayList::new));

    // Prepare filtered lists based on date-only availability (do NOT include
    // user's account/stock filters when computing availability)
    List<Account> filteredAccountList;
    List<StockItem> filteredStockItemList;
    Set<UUID> tradeAccountIds;
    Set<UUID> tradeStockIds;
    if (clientProvidedRange) {
      AvailableIds availableIds = StockAsyncSupport.join(availableIdsFuture);
      tradeAccountIds = availableIds.accountIds();
      tradeStockIds = availableIds.stockItemIds();

      filteredAccountList =
          accountList.stream().filter(a -> tradeAccountIds.contains(a.id())).toList();
      filteredStockItemList =
          stockItemList.stream().filter(s -> tradeStockIds.contains(s.id())).toList();
    } else {
      tradeAccountIds = Collections.emptySet();
      tradeStockIds = Collections.emptySet();
      filteredAccountList = accountList;
      filteredStockItemList = stockItemList;
    }

    // Validate requested filters against the full available lists (not the
    // filtered-by-date lists) so we can preserve previously selected items
    Set<UUID> availableAccountIds =
        accountList.stream().map(Account::id).collect(Collectors.toSet());
    List<UUID> requestedAccountIds = accountIdList;
    List<UUID> effectiveAccountIdList =
        retainAvailableIds(requestedAccountIds, availableAccountIds);

    Set<UUID> availableStockIds =
        stockItemList.stream().map(StockItem::id).collect(Collectors.toSet());
    List<UUID> requestedStockItemIds = stockTagSelection.requestedStockItemIds();
    List<UUID> effectiveStockItemIdList =
        retainAvailableStockItemIds(stockTagSelection, availableStockIds);

    // Build final lists for UI selects: when client provided a range, show
    // only date-available items but prepend any previously selected items
    // that don't appear in the date-available set so the selection doesn't
    // disappear.
    List<Account> finalAccountList;
    if (clientProvidedRange) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null) continue;
          if (!tradeAccountIds.contains(sel)) {
            accountList.stream()
                .filter(a -> a.id().equals(sel))
                .findFirst()
                .ifPresent(
                    a -> {
                      if (finalAccountList.stream().noneMatch(x -> x.id().equals(a.id())))
                        finalAccountList.add(0, a);
                    });
          }
        }
      }
    } else {
      finalAccountList = accountList;
    }

    List<StockItem> finalStockItemList;
    if (clientProvidedRange) {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!tradeStockIds.contains(sel)) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(
                    s -> {
                      if (finalStockItemList.stream().noneMatch(x -> x.id().equals(s.id())))
                        finalStockItemList.add(0, s);
                    });
          }
        }
      }
    } else {
      finalStockItemList = stockItemList;
    }

    List<TradeResponse> viewList =
        enrichedAll.stream()
            .filter(
                t ->
                    effectiveAccountIdList == null
                        || effectiveAccountIdList.contains(t.accountId()))
            .filter(
                t ->
                    effectiveStockItemIdList == null
                        || effectiveStockItemIdList.contains(t.stockItemId()))
            .collect(Collectors.toCollection(ArrayList::new));

    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";
      Comparator<TradeResponse> comparator =
          switch (field) {
            case "tradeDate" ->
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    TradeResponse::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "amount" ->
                Comparator.comparing(
                    TradeResponse::amount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "fee" ->
                Comparator.comparing(
                    TradeResponse::fee, Comparator.nullsLast(Comparator.naturalOrder()));
            case "realizedProfit" ->
                Comparator.comparing(
                    TradeResponse::realizedProfit, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };
      if (comparator != null) {
        if ("desc".equalsIgnoreCase(direction)) comparator = comparator.reversed();
        viewList.sort(comparator.thenComparing(TRADE_TIE_BREAKER));
      }
    } else {
      viewList.sort(
          Comparator.comparing(
                  TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(TRADE_TIE_BREAKER));
    }

    BigDecimal totalAllBuyAmount =
        viewList.stream()
            .filter(t -> t.type() == TradeType.BUY)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllSellAmount =
        viewList.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllFee =
        viewList.stream()
            .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllTax =
        viewList.stream()
            .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllRealizedProfit =
        viewList.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 실현손익 집계 (구 "실현 손익" 메뉴 통합) — 현재 필터(계좌/종목/태그/기간)와 동일한 조건으로 계산
    // 목록이 비어 있다는 것은 "필터를 걸었는데 유효한 대상이 하나도 없다"는 뜻이다.
    // 그대로 API 로 보내면 파라미터가 하나도 안 실려 '필터 없음'(= 전체)이 되므로 호출을 건너뛴다.
    boolean emptyStockSelection =
        (effectiveStockItemIdList != null && effectiveStockItemIdList.isEmpty())
            || (effectiveAccountIdList != null && effectiveAccountIdList.isEmpty());
    TradeProfitRequest profitRequest = new TradeProfitRequest();
    profitRequest.setUserId(userId);
    profitRequest.setAccountIdList(effectiveAccountIdList);
    profitRequest.setStockItemIdList(effectiveStockItemIdList);
    profitRequest.setStartDate(startInst);
    profitRequest.setEndDate(endInst);
    profitRequest.setTimeZone(timeZone);

    // 계좌별/종목별 실현손익은 같은 조건을 groupBy 만 달리해 부르는 서로 독립인 두 조회다.
    // 순차로 부르면 각 40ms 가 그대로 더해지므로 함께 던지고 이름만 요청 스레드에서 입힌다.
    var profitParams = profitRequest.toParams();
    var stockGroupedRequest = copyTradeProfitRequest(profitRequest);
    stockGroupedRequest.setGroupBy(
        net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup.STOCKITEM);
    var stockGroupedParams = stockGroupedRequest.toParams();

    // 먼저 던져 둔 것이 있으면 그대로 쓴다. 위 조건에서 두 목록이 모두 null 이므로 파라미터가 같다.
    java.util.concurrent.CompletableFuture<List<TradeProfit>> realizedRawFuture =
        emptyStockSelection
            ? null
            : earlyRealizedFuture != null
                ? earlyRealizedFuture
                : async.supply(() -> emptyIfNull(tradeProfitClient.calculateProfit(profitParams)));
    java.util.concurrent.CompletableFuture<List<TradeProfit>> stockGroupedRawFuture =
        emptyStockSelection
            ? null
            : earlyStockGroupedFuture != null
                ? earlyStockGroupedFuture
                : async.supply(
                    () -> emptyIfNull(tradeProfitClient.calculateProfit(stockGroupedParams)));

    // 계좌별 실현손익 (보유량 0 포함: 전량 매도 종목의 실현손익도 반영)
    List<TradeProfit> realizedEnrichedList =
        realizedRawFuture == null
            ? List.of()
            : enrichTradeProfits(
                StockAsyncSupport.join(realizedRawFuture), userId, tradeProfitNames);
    List<AccountRealizedRow> accountRealizedList = new ArrayList<>();
    // 계좌는 id 로 묶는다. 이름으로 묶으면 같은 이름의 계좌가 둘일 때 한 행으로 합쳐져
    // 실현손익이 뭉뚱그려진다(표시 순서는 예전과 같이 계좌명 오름차순).
    realizedEnrichedList.stream()
        .collect(Collectors.groupingBy(TradeProfit::accountId))
        .entrySet()
        .stream()
        .sorted(
            Comparator.comparing(
                e -> e.getValue().isEmpty() ? null : e.getValue().get(0).accountName(),
                Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(
            entry -> {
              var s = TradeProfitAggregator.aggregate(entry.getValue());
              String rowAccountName =
                  entry.getValue().isEmpty() ? null : entry.getValue().get(0).accountName();
              // 실현손익은 매도 거래에 기록된 값(증권사 기준)을 쓴다. 앱이 평균단가로 다시 계산한
              // realizedProfitNet 을 쓰면 같은 화면의 헤드라인/거래목록(기록값 합계)과 어긋난다
              // (실측: 헤드라인과 계좌별/종목별 합이 0.11% 어긋났다).
              // 화면의 다른 곳(자산현황 등)도 기록값을 쓰므로 여기만 예외였다.
              BigDecimal realized =
                  s.realizedProfit() != null ? s.realizedProfit() : BigDecimal.ZERO;
              BigDecimal sellAmount =
                  s.totalSellAmount() != null ? s.totalSellAmount() : BigDecimal.ZERO;
              // 매도 원가 = 매도금액 - 증권거래세 - 기록된 실현손익.
              // 기록된 실현손익은 증권사가 세금까지 뺀 뒤의 값이라, 세금을 빼지 않으면 원가가 그만큼
              // 부풀어 오른다. api-stock 의 costOfGoodsSold 와 배당 화면의 원가 계산이 이미 이 식을 쓴다.
              // 실측 2026-08-22: 이 화면만 세금을 빠뜨려 두 계좌(한국투자증권 위탁 · ISA)의 매도 원가가
              // 그 계좌 증권거래세 합만큼 과대 계상됐다.
              BigDecimal sellTax = s.totalSellTax() != null ? s.totalSellTax() : BigDecimal.ZERO;
              // 기록된 실현손익은 <b>계좌를 합친</b> 원가를 따른다(실측 2026-08-23: 매도 54 건 중 종목 단위
              // 원가로 50 건이 재현되고 계좌x종목 단위로는 38 건뿐이다). 그래서 이 행의 실현손익과
              // 매도원가는 이 계좌의 매수와 맞지 않을 수 있다 - 연금저축1 은 그 계좌 매매만으로 계산하면
              // 화면값의 5.0 배가 되고, ISA 는 반대로 화면값이 그 계좌 기준의 104 배다.
              // 두 값이 크게 갈리면 그 사실을 화면이 밝힌다(값 자체는 다른 화면과 맞추기 위해 기록값 유지).
              BigDecimal realizedOwnBasis =
                  s.realizedProfitNet() != null ? s.realizedProfitNet() : BigDecimal.ZERO;
              accountRealizedList.add(
                  new AccountRealizedRow(
                      rowAccountName,
                      sellAmount,
                      realized,
                      sellAmount.subtract(sellTax).subtract(realized),
                      realizedOwnBasis,
                      entry.getKey()));
            });

    // 종목별 실현손익 (계좌 무시, 보유량 0 포함) — 실현손익 발생 종목만, 실현손익 내림차순
    List<TradeProfit> stockRealizedList =
        (stockGroupedRawFuture == null
                ? Collections.<TradeProfit>emptyList()
                : enrichTradeProfits(
                    StockAsyncSupport.join(stockGroupedRawFuture), userId, tradeProfitNames))
            .stream()
                .map(this::toStockRealized)
                // 표시 값과 같은 기준(거래에 기록된 실현손익)으로 걸러 정렬한다. 기준이 다르면
                // 기록값은 0 이 아닌데 목록에서 빠지거나, 정렬 순서가 표시 값과 어긋난다.
                .filter(
                    tp ->
                        tp.realizedProfit() != null
                            && tp.realizedProfit().compareTo(BigDecimal.ZERO) != 0)
                .sorted(
                    Comparator.comparing(
                            TradeProfit::realizedProfit,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                            TradeProfit::stockItemName,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    long realizedWinCount =
        stockRealizedList.stream()
            .filter(
                tp ->
                    tp.realizedProfit() != null
                        && tp.realizedProfit().compareTo(BigDecimal.ZERO) > 0)
            .count();

    if (size <= 0) size = 15;
    // 상세 목록은 페이징 없이 전체를 펼쳐서 표시(헤더 sticky로 스크롤). 한 페이지에 전부 담는다.
    if (!viewList.isEmpty()) size = viewList.size();
    int totalItems = viewList.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int currentPage = Math.max(1, Math.min(page, totalPages));
    if (totalPages == 0) currentPage = 1;

    int fromIndex = (currentPage - 1) * size;
    int toIndex = Math.min(fromIndex + size, totalItems);
    List<TradeResponse> pagedList =
        (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex) : Collections.emptyList();

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
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
    var pagination = new Pagination(pageImpl);

    // 조회/페이징은 그대로(기본=날짜 desc, 1페이지=최신 묶음) 두고, 화면에는 그 페이지를 오름차순(오래된 것 위)으로 표시.
    // 사용자가 컬럼 정렬을 명시한 경우(sort 존재)는 선택한 정렬을 그대로 보여준다.
    List<TradeResponse> displayTradeList = new ArrayList<>(pagedList);
    if (sort == null || sort.isEmpty()) {
      Collections.reverse(displayTradeList);
    }
    model.addAttribute("tradeList", displayTradeList);
    model.addAttribute("allTradeList", viewList);
    model.addAttribute("pagination", pagination);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("size", size);
    model.addAttribute("accountList", finalAccountList);
    model.addAttribute("stockItemList", finalStockItemList);
    model.addAttribute(
        "tagCountStockItemList", clientProvidedRange ? filteredStockItemList : stockItemList);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute("accountNames", accountNames);
    model.addAttribute(
        "selectedAccountIds", effectiveAccountIdList != null ? effectiveAccountIdList : List.of());
    model.addAttribute(
        "selectedStockItemIds",
        effectiveStockItemIdList != null ? effectiveStockItemIdList : List.of());
    model.addAttribute("selectedStockTags", selectedStockTags);
    model.addAttribute(
        "selectedAccountId",
        (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
            ? effectiveAccountIdList.get(0)
            : null);
    model.addAttribute(
        "selectedStockItemId",
        (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
            ? effectiveStockItemIdList.get(0)
            : null);
    // reflect actual instants used (may have been defaulted to YTD above)
    model.addAttribute("startDate", startInst);
    model.addAttribute("endDate", endInst);
    // reflect rangeMode back into model (may have been defaulted above)
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("timeZone", timeZone);
    model.addAttribute("sort", sort);
    model.addAttribute("totalFee", totalFee);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);
    model.addAttribute("totalAllBuyAmount", totalAllBuyAmount);
    model.addAttribute("totalAllSellAmount", totalAllSellAmount);
    model.addAttribute("totalAllFee", totalAllFee);
    model.addAttribute("totalAllTax", totalAllTax);
    model.addAttribute("totalAllRealizedProfit", totalAllRealizedProfit);
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    model.addAttribute("accountRealizedList", accountRealizedList);
    model.addAttribute("stockRealizedList", stockRealizedList);
    model.addAttribute("realizedWinCount", realizedWinCount);
    model.addAttribute("realizedStockCount", stockRealizedList.size());

    return "stock/htmx/tradeList";
  }

  /** 계좌별 실현손익 요약 행 (매매 내역 화면용). */
  /**
   * @param realizedNet 화면에 찍는 값 &mdash; 매도 거래에 <b>기록된</b> 실현손익의 합
   * @param realizedOwnBasis 같은 계좌의 매매만으로 앱이 평균단가로 계산한 실현손익
   */
  /**
   * 계좌별 실현손익 한 줄.
   *
   * <p>매수금액은 담지 않는다 - 이 절의 다른 값은 전부 '판 것' 기준인데 매수금액만 '산 것 전체' 기준이라 종목별 표와 합이 6.79% 달랐다(실측
   * 2026-08-24). 화면은 수익률의 실제 분모인 {@code soldCost} 를 보여준다.
   */
  public record AccountRealizedRow(
      String name,
      BigDecimal sellAmount,
      BigDecimal realizedNet,
      BigDecimal soldCost,
      BigDecimal realizedOwnBasis,
      UUID accountId) {}

  public record Activity(
      String type,
      /** 상세 링크용. 예전에는 화면에서 '이름 -> id' 맵으로 되찾아, 같은 이름이 둘이면 엉뚱한 종목을 가리켰다. */
      UUID stockItemId,
      String stockItemName,
      String tradeType,
      Integer quantity,
      String description,
      BigDecimal amount,
      Instant date,
      /** 계좌 id 목록. 예전에는 계좌명 목록이라 화면에서 '이름 -> id' 로 되찾아야 했다(동명 계좌 오연결 위험). */
      List<UUID> accountIds) {}

  /**
   * 활동을 (날짜 · 유형 · 종목 · 매매구분) 으로 묶는다.
   *
   * <p>날짜 칸은 {@code zone} 기준이다. 예전에는 {@code ZoneId.systemDefault()} 를 썼는데, 화면은 요청 타임존으로 그린다 &mdash;
   * 달력은 요청 존으로 칸을 나누고(activityList.jte 의 activityByDate) 표는 서버 존으로 날짜를 찍어, 브라우저 타임존이 서버와 다르면 같은 활동이
   * 달력과 표에서 서로 다른 날에 나타났다(실측: 저장된 시각이 모두 UTC 자정이라 서버보다 서쪽 존에서는 매매 250건 전부 하루가 어긋난다).
   *
   * <p>종목은 이름이 아니라 id 로 묶는다. 이름으로 묶으면 같은 이름의 종목이 둘일 때 한 행으로 합쳐진다 &mdash; {@link Activity} 가 id 를 들고
   * 있는 이유와 같은 사정이다(현재 데이터에 동명 종목은 없다). id 가 없는 활동만 이름으로 묶는다.
   *
   * <p>정렬은 날짜 내림차순 뒤에 종목명 · 유형 · 매매구분을 더 본다. 예전에는 날짜 하나로만 정렬해 같은 날의 행 순서가 {@link HashMap} 순회
   * 순서였다(실측: 299행 중 171행(57%)이 이에 해당).
   */
  static List<Activity> groupActivitiesByDay(List<Activity> rawActivities, ZoneId zone) {
    Map<String, Activity> groupedMap = new HashMap<>();
    for (Activity a : rawActivities) {
      if (a.date() == null) continue;

      String dateStr = a.date().atZone(zone).toLocalDate().toString();
      String stockKey =
          a.stockItemId() != null ? a.stockItemId().toString() : "name:" + a.stockItemName();
      String key = String.format("%s|%s|%s|%s", dateStr, a.type(), stockKey, a.tradeType());

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

        List<UUID> newAccountIds = new ArrayList<>(existing.accountIds());
        if (!a.accountIds().isEmpty() && !newAccountIds.contains(a.accountIds().get(0))) {
          newAccountIds.add(a.accountIds().get(0));
        }

        groupedMap.put(
            key,
            new Activity(
                existing.type(),
                existing.stockItemId() != null ? existing.stockItemId() : a.stockItemId(),
                existing.stockItemName(),
                existing.tradeType(),
                newQty,
                existing.description(),
                newAmount,
                existing.date(),
                newAccountIds));
      } else {
        groupedMap.put(key, a);
      }
    }

    List<Activity> activities = new ArrayList<>(groupedMap.values());
    activities.sort(
        Comparator.comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Activity::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Activity::type, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Activity::tradeType, Comparator.nullsLast(Comparator.naturalOrder())));
    return activities;
  }

  /** 호출부가 이미 종목 목록을 읽었으면 넘겨서 재조회를 없앤다(측정: 최근활동 한 요청에 종목목록 2회). */
  private List<Activity> getAllActivities(
      UUID userId,
      Instant startInstant,
      Instant endInstant,
      List<StockItem> preloadedStockItems,
      ZoneId zone) {
    // 네 조회(거래·배당·종목목록·계좌)는 서로 의존이 없다. 순차로 던지면 응답시간이 그대로 합산된다
    // (실측: 종목목록 16.7 + 거래 24.1 + 배당 13.6 + 계좌 17.7 = 72ms, 프래그먼트 실측 77.6ms).
    // 호출 하나하나의 시간은 응답 크기와 거의 무관하다(거래 전체 80KB 24.1ms vs 이번달 3.3KB 19.2ms)
    // — 즉 줄일 것은 전송량이 아니라 직렬 왕복 횟수다.
    TradeSearchRequest tradeReq =
        new TradeSearchRequest(userId, null, null, startInstant, endInstant);
    var tradeParams = tradeReq.toParams();

    DividendRequest divReq = new DividendRequest();
    divReq.setUserId(userId);
    divReq.setStartDate(startInstant);
    divReq.setEndDate(endInstant);
    var divParams = divReq.toParams();

    var tradesFuture = async.supply(() -> emptyIfNull(tradeClient.findTrades(tradeParams)));
    var dividendsFuture = async.supply(() -> emptyIfNull(dividendClient.findDividends(divParams)));
    var stockItemsFuture =
        preloadedStockItems != null
            ? null
            : async.supply(() -> emptyIfNull(stockItemClient.getStockItems()));
    var accountsFuture = async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));

    List<TradeResponse> trades = StockAsyncSupport.join(tradesFuture);
    List<DividendResponse> dividends = StockAsyncSupport.join(dividendsFuture);

    List<StockItem> stockItemList =
        preloadedStockItems != null
            ? preloadedStockItems
            : StockAsyncSupport.join(stockItemsFuture);
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    List<Account> accountList = StockAsyncSupport.join(accountsFuture);
    Map<UUID, String> accountNamesMap =
        accountList.stream().collect(Collectors.toMap(Account::id, Account::name));

    List<Activity> rawActivities = new ArrayList<>();

    for (TradeResponse t : trades) {
      // 거래 응답에 이미 종목명이 들어 있다(실측: 250건 전부 존재, 종목 목록과 값 동일).
      // 배당 경로와 같은 규칙으로 응답 값을 먼저 쓰고, 없을 때만 목록에서 찾는다.
      String stockName =
          t.stockItemName() != null
              ? t.stockItemName()
              : stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown"));
      String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "TRADE",
              t.stockItemId(),
              stockName,
              t.type().name(),
              t.quantity(),
              null,
              t.amount(),
              t.tradeDate(),
              t.accountId() != null ? List.of(t.accountId()) : List.of()));
    }

    for (DividendResponse d : dividends) {
      String stockName =
          d.stockItemName() != null
              ? d.stockItemName()
              : stockItemNames.getOrDefault(d.stockItemId(), msg("stock.label.unknown"));
      String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "DIVIDEND",
              d.stockItemId(),
              stockName,
              null,
              null,
              msg("stock.activity.type.dividend.payout"),
              d.netAmount(),
              d.payDate() != null ? d.payDate() : d.recordDate(),
              d.accountId() != null ? List.of(d.accountId()) : List.of()));
    }

    return groupActivitiesByDay(rawActivities, zone);
  }

  @BlueskyPreAuthorize
  @GetMapping("/recent-activities")
  public String recentActivities(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return loginRequiredView(model);

    // 종목 목록도 getAllActivities 안에서 나머지 조회와 동시에 던지게 둔다.
    // 여기서 먼저 동기로 읽으면 그 시간이 통째로 앞에 붙는다(실측 16.7ms).
    // 이 엔드포인트는 timeZone 을 받지 않아 서버 존으로 묶는다(활동목록 화면과 달리 요청 존이 없다).
    List<Activity> activities = getAllActivities(userId, null, null, null, ZoneId.systemDefault());

    // 이번 달 요약
    LocalDate now = LocalDate.now();
    LocalDate monthStart = now.withDayOfMonth(1);
    Instant monthStartInst = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant monthEndInst = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    List<Activity> thisMonth =
        activities.stream()
            .filter(
                a ->
                    a.date() != null
                        && !a.date().isBefore(monthStartInst)
                        && a.date().isBefore(monthEndInst))
            .toList();

    long buyCount =
        thisMonth.stream()
            .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
            .count();
    long sellCount =
        thisMonth.stream()
            .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
            .count();
    BigDecimal buyAmount =
        thisMonth.stream()
            .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal sellAmount =
        thisMonth.stream()
            .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long dividendCount = thisMonth.stream().filter(a -> "DIVIDEND".equals(a.type())).count();
    BigDecimal dividendAmount =
        thisMonth.stream()
            .filter(a -> "DIVIDEND".equals(a.type()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 대시보드 "최근 활동"은 최신순 5건 유지.
    model.addAttribute("activities", activities.stream().limit(5).toList());
    model.addAttribute("thisMonthLabel", shortMonthLabel(now));
    model.addAttribute("buyCount", buyCount);
    model.addAttribute("sellCount", sellCount);
    model.addAttribute("buyAmount", buyAmount);
    model.addAttribute("sellAmount", sellAmount);
    model.addAttribute("dividendCount", dividendCount);
    model.addAttribute("dividendAmount", dividendAmount);
    return "stock/htmx/fragments/recentActivities";
  }

  @BlueskyPreAuthorize
  /** 활동 화면 뷰 모드. 셋 중 하나가 아니면 "all"(전부 렌더)로 본다. */
  /**
   * 활동 내역의 표시 뷰. 화면(JS)은 항상 {@code calendar / timeline / list} 중 하나를 실어 보내고 저장값이 없으면 {@code
   * calendar} 를 쓴다.
   *
   * <p>그런데 값이 없거나 모르는 값이면 서버는 세 뷰를 모두 그려 왔다({@code all}). 그 응답은 같은 조건에서 2,017KB 로, 실제 화면이 받는 {@code
   * calendar} 226KB / {@code timeline} 346KB 의 6~9 배다(실측, 전체 기간 기준). 화면이 절대 요청하지 않는 값이 기본값이라 직접
   * URL·비 JS 접근만 그 부담을 진다. 기본값을 화면과 같은 {@code calendar} 로 맞춘다. 세 뷰를 한 번에 받는 동작이 필요하면 {@code
   * activityView=all} 로 명시한다 (JS 의 즉시 전환 경로가 그 응답 모양을 그대로 처리한다).
   */
  private static String normalizeActivityView(String activityView) {
    if ("calendar".equals(activityView)
        || "timeline".equals(activityView)
        || "list".equals(activityView)
        || "all".equals(activityView)) {
      return activityView;
    }
    return "calendar";
  }

  @GetMapping("/activity-list")
  public String activityList(
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) List<String> stockTagList,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) String rangeMode,
      @RequestParam(required = false) String activityView,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return loginRequiredView(model);

    ZoneId zone = resolveZoneIdOrDefault(timeZone);

    // 화면에 보이는 뷰 하나만 렌더한다. 예전에는 캘린더/타임라인/목록 3종을 모두 그려 놓고
    // 클라이언트가 hidden 으로 감췄다(실측: 전체 기간에서 3048KB 중 1370KB 가 숨은 뷰).
    // 값이 없으면 예전처럼 전부 렌더한다 — 스크립트가 꺼진 환경에서도 탭이 동작해야 하기 때문이다.
    model.addAttribute("activityView", normalizeActivityView(activityView));

    // Convert start/end Instants into Instants for the helper (they already are
    // Instants)
    Instant startInstant = startDate;
    Instant endInstant = endDate;
    // rangeMode 는 어떤 프리셋 버튼이 눌렸는지 알리는 화면 상태값이지 기간 그 자체가 아니다
    // (기간은 startDate/endDate 로 온다). 그런데 이 가드가 rangeMode 의 '존재'를 보는 바람에,
    // 날짜 없이 rangeMode 만 실려 오면 기본 기간이 적용되지 않고 전 기간이 조회됐다
    // (실측: rangeMode=ytd 인데 올해 106행이 아니라 전체 300행). 같은 가드를 asset-growth 는
    // 이미 '날짜가 없고 all 도 아니면 기본 적용'으로 쓰고 있어, 그쪽에 맞춘다.
    if (startInstant == null && endInstant == null && !"all".equalsIgnoreCase(rangeMode)) {
      var preset = resolvePresetRange(rangeMode, zone);
      startInstant = preset.start();
      endInstant = preset.end();
      rangeMode = preset.mode();
    }

    // 최초 활동일 / 계좌 / 종목 / 기간 내 등장 계좌·종목 조회는 서로 의존이 없다. 함께 던진다.
    // 최초 활동일(거래·배당 중 이른 쪽)은 전체 활동 이력 대신 집계 엔드포인트 1회로 구한다.
    var dataFirstDateFuture = async.supply(() -> resolveDataFirstDate(userId, zone, true));
    var activityAccountsFuture =
        async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));
    var activityStockItemsFuture = async.supply(() -> emptyIfNull(stockItemClient.getStockItems()));

    // 필터를 아무것도 고르지 않으면 아래 활동 조회 조건이 계좌/종목 목록과 무관하게 확정된다
    // (retainAvailableIds 와 resolveStockTagSelection 이 둘 다 null 을 준다). 그때는 기다리지 않고
    // 위 조회들과 함께 던진다. 필터를 고른 요청은 예전처럼 교집합이 나온 뒤에 던진다.
    boolean noActivityFilterSelected =
        (accountIdList == null || accountIdList.isEmpty())
            && (stockItemIdList == null || stockItemIdList.isEmpty())
            && (stockTagList == null || stockTagList.isEmpty());
    java.util.concurrent.CompletableFuture<List<TradeResponse>> earlyActivityTrades = null;
    java.util.concurrent.CompletableFuture<List<DividendResponse>> earlyActivityDividends = null;
    if (noActivityFilterSelected) {
      var earlyActivityParams = activitySearchParams(userId, startInstant, endInstant, null, null);
      earlyActivityTrades =
          async.supply(
              () -> emptyIfNull(tradeClient.findTrades(earlyActivityParams.tradeParams())));
      earlyActivityDividends =
          async.supply(
              () -> emptyIfNull(dividendClient.findDividends(earlyActivityParams.divParams())));
    }

    boolean clientProvidedRangeForActivity =
        !((startInstant == null || startInstant.toEpochMilli() == 0)
            && (endInstant == null || endInstant.toEpochMilli() == 0)
            && (rangeMode == null || rangeMode.isBlank()));
    java.util.concurrent.CompletableFuture<AvailableIds> activityAvailableFuture = null;
    if (clientProvidedRangeForActivity) {
      // 활동 필터는 거래·배당 합집합이다. 예전에는 손익 계산 1회 + 배당 목록 1회(44KB)를 더 받았다.
      final Instant availStart = startInstant;
      final Instant availEnd = endInstant;
      activityAvailableFuture =
          async.supply(
              () -> {
                var ids = activityFilterIdsClient.findFilterIds(userId, availStart, availEnd);
                var accounts = new java.util.HashSet<UUID>(emptyIfNull(ids.tradeAccountIds()));
                accounts.addAll(emptyIfNull(ids.dividendAccountIds()));
                var stocks = new java.util.HashSet<UUID>(emptyIfNull(ids.tradeStockItemIds()));
                stocks.addAll(emptyIfNull(ids.dividendStockItemIds()));
                return new AvailableIds(accounts, stocks);
              });
    }

    LocalDate dataFirstDate = StockAsyncSupport.join(dataFirstDateFuture);
    // provide account/stock lists for filter UI
    List<Account> accountList = StockAsyncSupport.join(activityAccountsFuture);
    List<StockItem> stockItemList = StockAsyncSupport.join(activityStockItemsFuture);
    // 아래 손익 계산이 같은 두 목록을 다시 읽지 않도록 이름 맵을 만들어 넘긴다.
    TradeProfitNames activityNames = toTradeProfitNames(accountList, stockItemList);
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, stockItemIdList, stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    List<UUID> requestedStockIdsForActivity = stockTagSelection.requestedStockItemIds();
    // 계좌 필터는 이 사용자에게 실제로 있는 계좌만 남겨서 넘긴다. 없는 id 가 섞여 있으면
    // API 의 계좌 소유 검증에 걸려 거래 조회가 통째로 비고, 배당만 남아 결과가 오히려 이상해진다
    // (실측: 유효 계좌 1개면 64건인데 유효1+가짜1 이면 38건).
    Set<UUID> availableAccountIdsForActivity =
        accountList.stream().map(Account::id).collect(Collectors.toSet());
    List<UUID> effectiveAccountIdListForActivity =
        retainAvailableIds(accountIdList, availableAccountIdsForActivity);
    boolean emptyAccountSelectionForActivity =
        effectiveAccountIdListForActivity != null && effectiveAccountIdListForActivity.isEmpty();
    // 태그를 골랐는데 해당 종목이 하나도 없으면 종목 id 목록이 비고, 그대로 보내면 파라미터가 아예
    // 빠져 '필터 없음'(= 전체)이 된다(실측: 없는 태그로 걸렀는데 전체 106행이 그대로 나왔다).
    // 목록 화면(trade/list, dividend/list)은 이미 이 경우 빈 결과를 내므로 같게 맞춘다.
    boolean emptyStockSelectionForActivity =
        stockTagSelection.hasFilter()
            && (requestedStockIdsForActivity == null || requestedStockIdsForActivity.isEmpty());
    List<Activity> activities =
        emptyAccountSelectionForActivity || emptyStockSelectionForActivity
            ? List.of()
            : getAllActivities(
                userId,
                startInstant,
                endInstant,
                effectiveAccountIdListForActivity,
                requestedStockIdsForActivity,
                stockItemList,
                accountList,
                earlyActivityTrades,
                earlyActivityDividends,
                zone);

    // Determine date-only availability for activities (trades + dividends)
    List<Account> filteredAccountList;
    List<StockItem> filteredStockItemList;
    java.util.Set<UUID> activityAccountIds = new java.util.HashSet<>();
    java.util.Set<UUID> activityStockIds = new java.util.HashSet<>();
    if (clientProvidedRangeForActivity) {
      AvailableIds availableIds = StockAsyncSupport.join(activityAvailableFuture);
      activityAccountIds.addAll(availableIds.accountIds());
      activityStockIds.addAll(availableIds.stockItemIds());

      filteredAccountList =
          accountList.stream().filter(a -> activityAccountIds.contains(a.id())).toList();
      filteredStockItemList =
          stockItemList.stream().filter(s -> activityStockIds.contains(s.id())).toList();
    } else {
      filteredAccountList = accountList;
      filteredStockItemList = stockItemList;
    }

    // Validate requested filters against full lists so selection can be preserved
    List<UUID> requestedAccountIdsForActivity = accountIdList;

    Set<UUID> availableStockIdsForActivity =
        stockItemList.stream().map(StockItem::id).collect(Collectors.toSet());
    List<UUID> effectiveStockItemIdListForActivity =
        retainAvailableStockItemIds(stockTagSelection, availableStockIdsForActivity);

    // Build final lists and preserve selected items
    List<Account> finalAccountListForActivity;
    if (clientProvidedRangeForActivity) {
      finalAccountListForActivity = new ArrayList<>(filteredAccountList);
      if (requestedAccountIdsForActivity != null) {
        for (UUID sel : requestedAccountIdsForActivity) {
          if (sel == null) continue;
          if (!activityAccountIds.contains(sel)) {
            accountList.stream()
                .filter(a -> a.id().equals(sel))
                .findFirst()
                .ifPresent(
                    a -> {
                      if (finalAccountListForActivity.stream()
                          .noneMatch(x -> x.id().equals(a.id())))
                        finalAccountListForActivity.add(0, a);
                    });
          }
        }
      }
    } else {
      finalAccountListForActivity = accountList;
    }

    List<StockItem> finalStockItemListForActivity;
    if (clientProvidedRangeForActivity) {
      finalStockItemListForActivity = new ArrayList<>(filteredStockItemList);
      if (requestedStockIdsForActivity != null) {
        for (UUID sel : requestedStockIdsForActivity) {
          if (sel == null) continue;
          if (!activityStockIds.contains(sel)) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(
                    s -> {
                      if (finalStockItemListForActivity.stream()
                          .noneMatch(x -> x.id().equals(s.id())))
                        finalStockItemListForActivity.add(0, s);
                    });
          }
        }
      }
    } else {
      finalStockItemListForActivity = stockItemList;
    }

    long buyCount =
        activities.stream()
            .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
            .count();
    long sellCount =
        activities.stream()
            .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
            .count();
    BigDecimal buyAmount =
        activities.stream()
            .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal sellAmount =
        activities.stream()
            .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long dividendCount = activities.stream().filter(a -> "DIVIDEND".equals(a.type())).count();
    BigDecimal dividendAmount =
        activities.stream()
            .filter(a -> "DIVIDEND".equals(a.type()))
            .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 화면에는 오름차순(오래된 것 위)으로 표시 — 조회는 그대로(날짜 desc).
    List<Activity> displayActivities = new ArrayList<>(activities);
    Collections.reverse(displayActivities);
    model.addAttribute("activities", displayActivities);
    // 계좌 배지는 id 로 렌더하고 표시 이름은 이 맵에서 찾는다.
    model.addAttribute(
        "accountNames",
        accountList.stream()
            .filter(a -> a != null && a.id() != null)
            .collect(
                Collectors.toMap(
                    Account::id,
                    a -> a.name() != null ? a.name() : msg("stock.label.unknown"),
                    (l, r) -> l)));
    model.addAttribute("accountList", finalAccountListForActivity);
    model.addAttribute("stockItemList", finalStockItemListForActivity);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute(
        "selectedAccountIds",
        effectiveAccountIdListForActivity != null ? effectiveAccountIdListForActivity : List.of());
    model.addAttribute(
        "selectedStockItemIds",
        effectiveStockItemIdListForActivity != null
            ? effectiveStockItemIdListForActivity
            : List.of());
    model.addAttribute("selectedStockTags", selectedStockTags);
    model.addAttribute(
        "selectedAccountId",
        (effectiveAccountIdListForActivity != null && !effectiveAccountIdListForActivity.isEmpty())
            ? effectiveAccountIdListForActivity.get(0)
            : null);
    model.addAttribute(
        "selectedStockItemId",
        (effectiveStockItemIdListForActivity != null
                && !effectiveStockItemIdListForActivity.isEmpty())
            ? effectiveStockItemIdListForActivity.get(0)
            : null);
    model.addAttribute("startDate", startInstant);
    model.addAttribute("endDate", endInstant);
    model.addAttribute("timeZone", timeZone);
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    model.addAttribute("buyCount", buyCount);
    model.addAttribute("sellCount", sellCount);
    model.addAttribute("buyAmount", buyAmount);
    model.addAttribute("sellAmount", sellAmount);
    model.addAttribute("dividendCount", dividendCount);
    model.addAttribute("dividendAmount", dividendAmount);
    return "stock/htmx/fragments/activityList";
  }

  /** 활동 내역(거래+배당) 조회 파라미터. 조기 발사와 일반 경로가 같은 값을 쓰도록 한 곳에서 만든다. */
  private record ActivitySearchParams(
      org.springframework.util.MultiValueMap<String, String> tradeParams,
      org.springframework.util.MultiValueMap<String, String> divParams) {}

  private ActivitySearchParams activitySearchParams(
      UUID userId,
      Instant startInstant,
      Instant endInstant,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList) {
    TradeSearchRequest tradeReq =
        new TradeSearchRequest(userId, accountIdList, stockItemIdList, startInstant, endInstant);
    DividendRequest divReq = new DividendRequest();
    divReq.setUserId(userId);
    divReq.setStartDate(startInstant);
    divReq.setEndDate(endInstant);
    divReq.setAccountIdList(accountIdList);
    divReq.setStockItemIdList(stockItemIdList);
    return new ActivitySearchParams(tradeReq.toParams(), divReq.toParams());
  }

  /**
   * 거래·배당 조회를 호출부가 이미 던져 뒀으면 그 future 를 그대로 쓴다.
   *
   * <p>필터를 아무것도 고르지 않은 요청은 이 두 조회의 조건이 계좌/종목 목록과 무관하게 확정되어 있는데도, 앞선 조회들이 다 끝난 뒤에야 출발하고 있었다 (실측:
   * activity-list 의 trade/dividend 가 +8ms 에 시작).
   */
  private List<Activity> getAllActivities(
      UUID userId,
      Instant startInstant,
      Instant endInstant,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList,
      List<StockItem> preloadedStockItems,
      List<Account> preloadedAccounts,
      java.util.concurrent.CompletableFuture<List<TradeResponse>> preTradesFuture,
      java.util.concurrent.CompletableFuture<List<DividendResponse>> preDividendsFuture,
      ZoneId zone) {
    // 거래와 배당 조회는 서로 의존이 없다(실측 22.7ms + 29.0ms). 함께 던진다.
    var tradesFuture = preTradesFuture;
    var dividendsFuture = preDividendsFuture;
    if (tradesFuture == null || dividendsFuture == null) {
      var params =
          activitySearchParams(userId, startInstant, endInstant, accountIdList, stockItemIdList);
      tradesFuture = async.supply(() -> emptyIfNull(tradeClient.findTrades(params.tradeParams())));
      dividendsFuture =
          async.supply(() -> emptyIfNull(dividendClient.findDividends(params.divParams())));
    }
    List<TradeResponse> trades = StockAsyncSupport.join(tradesFuture);
    List<DividendResponse> dividends = StockAsyncSupport.join(dividendsFuture);

    List<StockItem> stockItemList =
        preloadedStockItems != null
            ? preloadedStockItems
            : emptyIfNull(stockItemClient.getStockItems());
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    List<Account> accountList =
        preloadedAccounts != null
            ? preloadedAccounts
            : emptyIfNull(accountClient.getAccountsByUserId(userId));
    Map<UUID, String> accountNamesMap =
        accountList.stream().collect(Collectors.toMap(Account::id, Account::name));

    List<Activity> rawActivities = new ArrayList<>();

    for (TradeResponse t : trades) {
      // 거래 응답에 이미 종목명이 들어 있다(실측: 250건 전부 존재, 종목 목록과 값 동일).
      // 배당 경로와 같은 규칙으로 응답 값을 먼저 쓰고, 없을 때만 목록에서 찾는다.
      String stockName =
          t.stockItemName() != null
              ? t.stockItemName()
              : stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown"));
      String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "TRADE",
              t.stockItemId(),
              stockName,
              t.type().name(),
              t.quantity(),
              null,
              t.amount(),
              t.tradeDate(),
              t.accountId() != null ? List.of(t.accountId()) : List.of()));
    }

    for (DividendResponse d : dividends) {
      String stockName =
          d.stockItemName() != null
              ? d.stockItemName()
              : stockItemNames.getOrDefault(d.stockItemId(), msg("stock.label.unknown"));
      String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
      rawActivities.add(
          new Activity(
              "DIVIDEND",
              d.stockItemId(),
              stockName,
              null,
              null,
              msg("stock.activity.type.dividend.payout"),
              d.netAmount(),
              d.payDate() != null ? d.payDate() : d.recordDate(),
              d.accountId() != null ? List.of(d.accountId()) : List.of()));
    }

    return groupActivitiesByDay(rawActivities, zone);
  }
}
