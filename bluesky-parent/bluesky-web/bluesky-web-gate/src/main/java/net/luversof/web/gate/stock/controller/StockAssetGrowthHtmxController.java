package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DataFirstDateClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockAssetGrowthHtmxController extends StockBaseHtmxController {
  private static final Logger logger =
      LoggerFactory.getLogger(StockAssetGrowthHtmxController.class);

  private final DataFirstDateClient dataFirstDateClient;

  private final java.util.concurrent.ExecutorService stockRemoteCallExecutor;

  public StockAssetGrowthHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      DataFirstDateClient dataFirstDateClient,
      java.util.concurrent.ExecutorService stockRemoteCallExecutor,
      MessageSource messageSource) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
    this.dataFirstDateClient = dataFirstDateClient;
    this.stockRemoteCallExecutor = stockRemoteCallExecutor;
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/view")
  public String assetGrowthView(
      TradeProfitRequest request,
      @RequestParam(required = false) java.util.List<String> stockTagList,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return loginRequiredView(model);
    }

    request.setUserId(userId);
    String effectiveRangeMode = rangeMode;

    // 기간 판정과 기본값은 원격 조회에 의존하지 않는다. 먼저 확정해 두면 아래 조회들을
    // 한꺼번에 던질 수 있다(예전에는 종목목록 -> 계좌목록 -> 시계열 -> 최초일자 -> 기간손익 순으로
    // 왕복이 줄줄이 이어져, 백엔드 처리 67.6ms 인 화면이 109.1ms 걸렸다).
    boolean clientProvidedRange =
        !((request.getStartDate() == null || request.getStartDate().toEpochMilli() == 0)
            && (request.getEndDate() == null || request.getEndDate().toEpochMilli() == 0)
            && (rangeMode == null || rangeMode.isBlank()));

    // If no date range provided, default to this year (ytd).
    // 단, '전체(all)'는 빈 기간으로 전체 데이터를 의미하므로 YTD 기본값을 적용하지 않는다.
    if (request.getStartDate() == null
        && request.getEndDate() == null
        && !"all".equalsIgnoreCase(rangeMode)) {
      ZoneId zone = resolveZoneIdOrDefault(request.getTimeZone());
      var preset = resolvePresetRange(rangeMode, zone);
      request.setStartDate(preset.start());
      request.setEndDate(preset.end());
      if (effectiveRangeMode == null || effectiveRangeMode.isBlank()) {
        effectiveRangeMode = preset.mode();
      }
    }

    // 1단계: 서로 의존이 없는 조회를 동시에 던진다.
    var stockItemsFuture =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> emptyIfNull(stockItemClient.getStockItems()), stockRemoteCallExecutor);
    var accountsFuture =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> emptyIfNull(accountClient.getAccountsByUserId(userId)), stockRemoteCallExecutor);
    var dataFirstDateFuture =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> dataFirstDateClient.findDataFirstDate(userId), stockRemoteCallExecutor);

    // 계좌 필터가 있으면 보내기 전에 이 사용자 계좌로 좁힌다. 없는 id 가 하나라도 섞이면 api-stock 이
    // 요청을 거절해 이 화면이 통째로 오류가 됐다. 여기 retainAvailableIds 는 지금까지 드롭다운 표시에만
    // 쓰였고 조회 파라미터는 원본 그대로 나갔다. 필터가 없으면 기다리지 않고 그대로 던진다.
    boolean emptyAccountSelection = narrowToOwnedAccounts(request, accountsFuture);
    // 고른 계좌가 하나도 유효하지 않으면 빈 목록이 되는데, 그대로 보내면 파라미터가 아예 빠져
    // '필터 없음'(= 전체)이 되어 오히려 전부 보인다. 목록 화면과 같이 조회를 건너뛴다.

    TradeProfitRequest dateOnlyReqPre = new TradeProfitRequest();
    dateOnlyReqPre.setUserId(userId);
    dateOnlyReqPre.setStartDate(request.getStartDate());
    dateOnlyReqPre.setEndDate(request.getEndDate());
    dateOnlyReqPre.setTimeZone(request.getTimeZone());
    var dateOnlyParams = dateOnlyReqPre.toParams();
    var dateRangeProfitFuture =
        clientProvidedRange
            ? java.util.concurrent.CompletableFuture.supplyAsync(
                () -> emptyIfNull(tradeProfitClient.calculateProfit(dateOnlyParams)),
                stockRemoteCallExecutor)
            : null;

    // 태그가 선택되지 않았다면 종목 id 집합은 종목 목록 조회 결과와 무관하다
    // (resolveStockTagSelection 이 종목 목록을 보는 것은 태그가 있을 때뿐).
    // 그런 경우엔 가장 무거운 시계열 조회(실측 38ms)도 1단계에서 함께 던진다.
    // normalizeStockTags 와 같은 판정: 공백뿐인 값은 태그로 치지 않는다.
    boolean noStockTagSelected =
        stockTagList == null
            || stockTagList.stream().noneMatch(org.springframework.util.StringUtils::hasText);
    StockTagSelection earlySelection =
        noStockTagSelected
            ? resolveStockTagSelection(null, request.getStockItemIdList(), stockTagList)
            : null;
    java.util.concurrent.CompletableFuture<
            net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesResult>
        earlySeriesFuture = null;
    if (earlySelection != null && !emptyAccountSelection) {
      request.setStockItemIdList(earlySelection.requestedStockItemIds());
      var earlySeriesParams = request.toParams();
      earlySeriesParams.add("granularity", "AUTO");
      earlySeriesFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> tradeProfitClient.timeSeriesWithSummary(earlySeriesParams),
              stockRemoteCallExecutor);
    }

    // 매매 이력의 거래 조회는 계좌/종목 필터를 아예 싣지 않고(기간만 본다), 기간도 위에서 이미
    // 확정됐다. 그런데 지금까지는 1단계 응답을 다 받은 뒤에야 출발했다(실측: +6ms). 함께 던진다.
    // 종목 목록은 조회가 끝난 뒤 이름을 붙일 때만 쓰므로 나중에 넘겨도 된다.
    ZoneId tradeHistoryZone = resolveZoneIdOrDefault(request.getTimeZone());
    String tradeHistoryFrom =
        request.getStartDate() != null
            ? request.getStartDate().atZone(tradeHistoryZone).toLocalDate().toString()
            : "";
    String tradeHistoryTo =
        request.getEndDate() != null
            ? request.getEndDate().atZone(tradeHistoryZone).toLocalDate().minusDays(1).toString()
            : "";
    // 태그를 고르지 않았다면 종목 id 집합이 이미 확정돼 있어(earlySelection) 여기서 바로 던질 수 있다.
    // 태그를 골랐다면 종목 목록을 받아야 대상이 정해지므로 아래에서 던진다.
    java.util.concurrent.CompletableFuture<List<TradeResponse>> earlyTradeHistoryFuture = null;
    if (earlySelection != null && !emptyAccountSelection) {
      var earlyTradeHistoryParams =
          tradeHistoryParams(
              userId,
              tradeHistoryFrom,
              tradeHistoryTo,
              request.getAccountIdList(),
              earlySelection.requestedStockItemIds());
      earlyTradeHistoryFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> emptyIfNull(tradeClient.findTrades(earlyTradeHistoryParams)),
              stockRemoteCallExecutor);
    }

    // Load filter source lists (full account/stock lists for the detail filter form).
    List<StockItem> stockItemList =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(stockItemsFuture);
    List<net.luversof.web.gate.stock.domain.Account> accountList =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountsFuture);

    // Resolve tag selection -> stock item ids (tags drive the stock selection).
    StockTagSelection stockTagSelection =
        earlySelection != null
            ? earlySelection
            : resolveStockTagSelection(stockItemList, request.getStockItemIdList(), stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    request.setStockItemIdList(stockTagSelection.requestedStockItemIds());

    // 필터를 걸었는데 남는 종목이 하나도 없으면(예: 이 사용자가 거래한 적 없는 태그) 종목 목록이 빈
    // 리스트가 된다. 그대로 API 로 보내면 파라미터가 하나도 실리지 않아 '필터 없음'(= 전체)이 된다
    // (실측: 없는 태그로 조회하면 다른 화면은 0 건인데 이 화면만 250 건 전체가 그대로 나왔다).
    // 계좌 쪽과 같은 방식으로 조회를 건너뛴다.
    boolean emptyStockSelection =
        request.getStockItemIdList() != null && request.getStockItemIdList().isEmpty();
    boolean emptySelection = emptyAccountSelection || emptyStockSelection;

    List<UUID> requestedAccountIds = request.getAccountIdList();
    List<UUID> requestedStockItemIds = request.getStockItemIdList();

    // 매매 이력 패널은 예전엔 이 응답이 그려진 뒤에야 별도 요청으로 채워졌다(실측: 화면 완료 286ms 중
    // 127ms 가 두 번째 왕복 대기). 같은 기간으로 여기서 함께 계산해 왕복을 없앤다.
    // 시작 시점만 앞당기고 결과는 마지막에 수거하므로 아래 원격 호출들과 겹쳐 돌아간다.
    final var tradeHistoryAccountIds = request.getAccountIdList();
    final var tradeHistoryStockIds = request.getStockItemIdList();
    final var tradeHistoryPreFuture = earlyTradeHistoryFuture;
    var tradeHistoryFuture =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () ->
                emptySelection
                    ? emptyTradeHistory(tradeHistoryFrom, tradeHistoryTo, 20)
                    : buildTradeHistoryData(
                        request.getUserId(),
                        tradeHistoryFrom,
                        tradeHistoryTo,
                        1,
                        20,
                        stockItemList,
                        tradeHistoryAccountIds,
                        tradeHistoryStockIds,
                        tradeHistoryPreFuture),
            stockRemoteCallExecutor);
    // 페이징 링크가 같은 필터를 유지하도록 질의 문자열을 만들어 넘긴다.
    model.addAttribute(
        "tradeHistoryFilterQuery",
        tradeHistoryFilterQuery(tradeHistoryAccountIds, tradeHistoryStockIds));

    // Chart data for the SELECTED range. The upstream aggregateTimeSeries simulates from
    // the first trade (carrying prior holdings) and only outputs from the requested start,
    // so a range query already reflects carried-over holdings at the right granularity
    // (AUTO picks DAILY for short windows). No client-side x-axis windowing needed.
    // 시리즈와 기간 요약을 한 번에 받는다. 예전에는 요약을 별도 프래그먼트가 다시 호출해
    // 같은 시뮬레이션(전체 거래 이력)이 두 번 돌았다.
    net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesResult timeSeriesResult;
    if (emptySelection) {
      timeSeriesResult = null;
    } else if (earlySeriesFuture != null) {
      timeSeriesResult =
          net.luversof.web.gate.stock.support.StockAsyncSupport.join(earlySeriesFuture);
    } else {
      var seriesParams = request.toParams();
      seriesParams.add("granularity", "AUTO");
      timeSeriesResult = tradeProfitClient.timeSeriesWithSummary(seriesParams);
    }
    List<TradeProfitTimeSeriesPoint> timeSeries =
        timeSeriesResult != null ? timeSeriesResult.series() : null;
    addPeriodSummaryAttributes(model, timeSeriesResult != null ? timeSeriesResult.summary() : null);
    model.addAttribute(
        "yearlySummaries",
        timeSeriesResult != null && timeSeriesResult.yearly() != null
            ? timeSeriesResult.yearly()
            : List.of());

    // 날짜 네비게이션의 "이전" 가드용 최초 데이터 일자.
    // 전 기간 시계열을 다시 집계하면(기간 제거 timeSeries) 이 화면에서만 초 단위가 걸렸다.
    // 집계 엔드포인트 1회로 대체한다.
    var zone = resolveZoneIdOrDefault(request.getTimeZone());
    var firstDateResponse =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dataFirstDateFuture);
    java.time.Instant firstInstant = firstDateResponse.tradeFirstDate();
    if (firstDateResponse.dividendFirstDate() != null
        && (firstInstant == null || firstDateResponse.dividendFirstDate().isBefore(firstInstant))) {
      firstInstant = firstDateResponse.dividendFirstDate();
    }
    java.time.LocalDate dataFirstDate =
        firstInstant != null ? firstInstant.atZone(zone).toLocalDate() : null;

    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    model.addAttribute("rangeMode", effectiveRangeMode);
    model.addAttribute("startDate", request.getStartDate());
    model.addAttribute("endDate", request.getEndDate());
    model.addAttribute("timeZone", request.getTimeZone());

    // Detail filter form model. Like realized-profit: when a date range is chosen,
    // narrow the account/stock dropdowns to items that have trades in that range,
    // but keep previously-selected items visible even if absent from the range.
    Set<UUID> tradeAccountIds;
    Set<UUID> tradeStockIds;
    List<net.luversof.web.gate.stock.domain.Account> filteredAccountList;
    List<StockItem> filteredStockItemList;
    if (clientProvidedRange) {
      // id 집합만 쓰므로 이름을 붙이지 않는다. 예전에는 이름 붙이기 안에서 계좌·종목 목록을
      // 한 번씩 더 읽어, 이 프래그먼트 한 건이 종목 목록을 3번·계좌 목록을 2번 조회했다(실측).
      var dateRangeEnriched =
          new ArrayList<>(
              net.luversof.web.gate.stock.support.StockAsyncSupport.join(dateRangeProfitFuture));
      tradeAccountIds =
          dateRangeEnriched.stream()
              .map(tp -> tp.accountId())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      tradeStockIds =
          dateRangeEnriched.stream()
              .map(tp -> tp.stockItemId())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
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

    Set<UUID> availableAccountIds =
        accountList.stream().map(a -> a.id()).collect(Collectors.toSet());
    // 여기 값은 드롭다운의 선택 표시에 쓰인다. 없는 id 가 하나라도 섞이면 선택 자체를 지워서,
    // 데이터는 계좌로 걸러져 있는데 드롭다운만 "전체"로 보이는 어긋남이 있었다(실측: 응답 차이가
    // option 의 selected 속성 한 곳뿐). 유효한 id 만 남긴다.
    List<UUID> effectiveAccountIds = retainAvailableIds(requestedAccountIds, availableAccountIds);
    if (effectiveAccountIds == null) {
      effectiveAccountIds = List.of();
    }

    Set<UUID> availableStockIds =
        stockItemList.stream().map(s -> s.id()).collect(Collectors.toSet());
    List<UUID> effectiveStockItemIds =
        (requestedStockItemIds != null && availableStockIds.containsAll(requestedStockItemIds))
            ? requestedStockItemIds
            : (stockTagSelection.hasFilter() ? List.of() : requestedStockItemIds);
    if (effectiveStockItemIds == null) {
      effectiveStockItemIds = List.of();
    }

    // Final dropdown lists: narrowed list + previously-selected items not in range.
    List<net.luversof.web.gate.stock.domain.Account> finalAccountList;
    if (clientProvidedRange) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null || tradeAccountIds.contains(sel)) {
            continue;
          }
          accountList.stream()
              .filter(a -> a.id().equals(sel))
              .findFirst()
              .ifPresent(
                  a -> {
                    if (finalAccountList.stream().noneMatch(x -> x.id().equals(a.id()))) {
                      finalAccountList.add(0, a);
                    }
                  });
        }
      }
    } else {
      finalAccountList = new ArrayList<>(filteredAccountList);
    }

    List<StockItem> finalStockItemList;
    if (clientProvidedRange) {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null || tradeStockIds.contains(sel)) {
            continue;
          }
          stockItemList.stream()
              .filter(s -> s.id().equals(sel))
              .findFirst()
              .ifPresent(
                  s -> {
                    if (finalStockItemList.stream().noneMatch(x -> x.id().equals(s.id()))) {
                      finalStockItemList.add(0, s);
                    }
                  });
        }
      }
    } else {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
    }

    model.addAttribute("accountList", finalAccountList);
    model.addAttribute("stockItemList", finalStockItemList);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute("selectedAccountIds", effectiveAccountIds);
    model.addAttribute("selectedStockItemIds", effectiveStockItemIds);
    model.addAttribute("selectedStockTags", selectedStockTags);
    // 껍데기(CompletionException)를 벗기는 규칙은 StockAsyncSupport.join 한 곳에만 둔다.
    model.addAttribute(
        "tradeHistoryData",
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(tradeHistoryFuture));
    return "stock/htmx/asset-growth";
  }

  /**
   * 기간 요약 값을 화면 모델에 실는다. 요약이 없으면 '계산 불가'로 렌더된다.
   *
   * <p>같은 패키지의 테스트가 직접 부를 수 있게 package-private 이다 &mdash; 요약에 필드를 늘리고 <b>여기에 넣는 것을 잊으면</b> 화면은 조용히
   * "계산 불가" 를 그린다. api-stock 이 값을 못 낸 것과 구분되지 않아, 원인을 엉뚱한 곳에서 찾게 된다.
   */
  void addPeriodSummaryAttributes(Model model, TradeProfitTimeSeriesSummary summary) {
    model.addAttribute("periodReturnRatePct", summary != null ? summary.growthRatePct() : null);
    model.addAttribute("returnCalculable", summary != null && summary.growthRatePct() != null);
    model.addAttribute(
        "timeWeightedReturnPct", summary != null ? summary.timeWeightedReturnPct() : null);
    model.addAttribute("periodProfit", summary != null ? summary.periodProfit() : null);
    model.addAttribute("principalDelta", summary != null ? summary.principalDelta() : null);
    model.addAttribute("unrealizedStart", summary != null ? summary.unrealizedStart() : null);
    model.addAttribute("unrealizedEnd", summary != null ? summary.unrealizedEnd() : null);
    model.addAttribute("unrealizedEndPct", summary != null ? summary.unrealizedEndPct() : null);
    model.addAttribute("recoveredAmount", summary != null ? summary.recoveredAmount() : null);
    model.addAttribute("netNewProfit", summary != null ? summary.netNewProfit() : null);
    model.addAttribute("maxDrawdownPct", summary != null ? summary.maxDrawdownPct() : null);
    model.addAttribute(
        "maxDrawdownPeakDate", summary != null ? summary.maxDrawdownPeakDate() : null);
    model.addAttribute(
        "maxDrawdownTroughDate", summary != null ? summary.maxDrawdownTroughDate() : null);
    model.addAttribute("currentDrawdownPct", summary != null ? summary.currentDrawdownPct() : null);
    model.addAttribute("openingValue", summary != null ? summary.openingValue() : null);
    model.addAttribute("closingValue", summary != null ? summary.closingValue() : null);
    model.addAttribute(
        "periodProfitRatePct", summary != null ? summary.periodProfitRatePct() : null);
    model.addAttribute("peakValue", summary != null ? summary.peakValue() : null);
    model.addAttribute("peakValueDate", summary != null ? summary.peakValueDate() : null);
    model.addAttribute("troughValue", summary != null ? summary.troughValue() : null);
    model.addAttribute("troughValueDate", summary != null ? summary.troughValueDate() : null);
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/period-return")
  public String assetGrowthPeriodReturn(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String timeZone,
      Model model) {
    var userId = UserUtil.getUserId();
    // 로그인이 풀렸으면 같은 화면의 다른 조각들과 같은 안내를 돌려준다. 예전에는 여기만 '계산 불가'
    // 자리표시자를 그렸다(실측: 비로그인 호출이 1,632바이트짜리 빈 요약, 다른 조각 13개는 344바이트 안내).
    if (userId == null) {
      return loginRequiredView(model);
    }
    // 템플릿은 이 두 값을 '빈 문자열 기본값'으로 선언하고 isBlank() 로 검사한다. 모델에 null 을 넣으면
    // 그 기본값이 무효화되어 렌더 중 NPE 가 나고 화면이 500 이 된다(실측: 파라미터 없이 호출하면 500).
    model.addAttribute("fromDate", from != null ? from : "");
    model.addAttribute("toDate", to != null ? to : "");
    TradeProfitTimeSeriesSummary summary;
    try {
      summary = loadPeriodSummary(userId, from, to, timeZone);
    } catch (RuntimeException ex) {
      // 실패를 삼키고 빈 요약을 그리면 "계산할 수 없는 기간" 과 구분되지 않는다.
      logger.warn("Failed to load period summary: from={} to={} tz={}", from, to, timeZone, ex);
      return remoteFailureView(model);
    }
    addPeriodSummaryAttributes(model, summary);
    return "stock/htmx/fragments/assetGrowthPeriodReturnSummary";
  }

  @GetMapping("/holdings-snapshot")
  public String holdingsSnapshot(
      @RequestParam(required = false) String date,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) String timeZone,
      Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return loginRequiredView(model);
    }
    if (date == null || date.isBlank()) {
      model.addAttribute("holdings", List.of());
      model.addAttribute("date", "");
      return "stock/htmx/holdings-snapshot";
    }
    var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
    params.add("userId", userId.toString());
    params.add("date", date);
    if (accountId != null && !accountId.isBlank()) {
      params.add("accountId", accountId);
    }
    // 일자 집계 기준 타임존. 빠뜨리면 api-stock 이 서버 기본 타임존으로 계산해
    // 컨테이너가 UTC 인 환경에서 클릭한 날짜와 하루 어긋난다.
    if (timeZone != null && !timeZone.isBlank()) {
      params.add("timeZone", timeZone);
    }
    List<HoldingsSnapshotItem> holdings = emptyIfNull(tradeProfitClient.holdingsSnapshot(params));
    model.addAttribute("holdings", holdings);
    model.addAttribute("date", date);
    return "stock/htmx/holdings-snapshot";
  }

  /** 매매 이력 계산(원격 호출 포함). 프래그먼트 요청과 자산성장 인라인 렌더가 공유한다. */
  private TradeHistoryData buildTradeHistoryData(
      UUID userId, String from, String to, int page, int size) {
    return buildTradeHistoryData(userId, from, to, page, size, null);
  }

  /** preloadedStockItems 가 있으면 종목 목록을 다시 읽지 않는다(같은 요청에서 이미 읽은 목록 재사용). */
  /** 매매 이력 거래 조회 파라미터. 조기 발사와 일반 경로가 반드시 같은 값을 쓰도록 한 곳에서 만든다. */
  /** 매매 이력 패널의 페이징 링크에 붙일 필터 질의 문자열(비어 있으면 ""). */
  private static String tradeHistoryFilterQuery(
      List<UUID> accountIdList, List<UUID> stockItemIdList) {
    StringBuilder sb = new StringBuilder();
    if (accountIdList != null) {
      for (UUID id : accountIdList) {
        if (id != null) sb.append("&accountIdList=").append(id);
      }
    }
    if (stockItemIdList != null) {
      for (UUID id : stockItemIdList) {
        if (id != null) sb.append("&stockItemIdList=").append(id);
      }
    }
    return sb.toString();
  }

  private org.springframework.util.MultiValueMap<String, String> tradeHistoryParams(
      UUID userId, String from, String to) {
    return tradeHistoryParams(userId, from, to, null, null);
  }

  /**
   * 매매 이력 조회 파라미터.
   *
   * <p>이 패널은 화면의 계좌/종목 필터를 전혀 싣지 않고 있었다. 위 차트는 필터를 타는데 아래 목록만 전체를 보여줘서, 계좌를 바꿔도 목록이 그대로였다(실측: 서로 다른
   * 세 계좌에서 매매 20건이 완전히 동일했고, API 로 보면 각 계좌의 실제 거래는 전부 달랐다).
   */
  private org.springframework.util.MultiValueMap<String, String> tradeHistoryParams(
      UUID userId, String from, String to, List<UUID> accountIdList, List<UUID> stockItemIdList) {
    Instant tradeStart =
        (from != null && !from.isBlank())
            ? LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant()
            : null;
    // 종료는 '그 다음 날 0시'가 아니라 '그 날의 마지막 순간'이어야 한다. 백엔드는 endDate 를
    // isAfter 로만 걸러내므로(= 경계 포함), 다음 날 0시를 그대로 주면 그 시각 거래가 범위에 들어온다.
    // 거래 시각이 모두 자정(T00:00:00Z)이라 종료일 다음 날 거래가 통째로 딸려왔다
    // (실측: 2026-08-01~2026-08-18 조회에 08-19 거래 5건이 섞여 10행).
    Instant tradeEnd =
        (to != null && !to.isBlank())
            ? LocalDate.parse(to)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .minusMillis(1)
            : null;
    return new TradeSearchRequest(userId, accountIdList, stockItemIdList, tradeStart, tradeEnd)
        .toParams();
  }

  private TradeHistoryData buildTradeHistoryData(
      UUID userId,
      String from,
      String to,
      int page,
      int size,
      List<StockItem> preloadedStockItems) {
    return buildTradeHistoryData(
        userId, from, to, page, size, preloadedStockItems, null, null, null);
  }

  /** 거래 조회를 호출부가 이미 던져 뒀으면 그 future 를 그대로 쓴다. */
  private TradeHistoryData buildTradeHistoryData(
      UUID userId,
      String from,
      String to,
      int page,
      int size,
      List<StockItem> preloadedStockItems,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList,
      java.util.concurrent.CompletableFuture<List<TradeResponse>> preTradesFuture) {
    var allFromApi =
        preTradesFuture != null
            ? net.luversof.web.gate.stock.support.StockAsyncSupport.join(preTradesFuture)
            : emptyIfNull(
                tradeClient.findTrades(
                    tradeHistoryParams(userId, from, to, accountIdList, stockItemIdList)));

    List<StockItem> stockItems =
        preloadedStockItems != null
            ? preloadedStockItems
            : emptyIfNull(stockItemClient.getStockItems());
    Map<UUID, String> stockItemNames =
        stockItems.stream().collect(Collectors.toMap(StockItem::id, StockItem::name, (l, r) -> l));

    var allTrades =
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
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toCollection(ArrayList::new));

    BigDecimal totalBuy =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.BUY)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSell =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalRealizedProfit =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalFee =
        allTrades.stream()
            .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTax =
        allTrades.stream()
            .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    int totalItems = allTrades.size();
    if (size <= 0) size = 20;
    int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / size) : 0;
    int currentPage = Math.max(1, Math.min(page, Math.max(1, totalPages)));
    int fromIdx = (currentPage - 1) * size;
    int toIdx = Math.min(fromIdx + size, totalItems);
    List<TradeResponse> pagedTrades =
        fromIdx < totalItems ? allTrades.subList(fromIdx, toIdx) : Collections.emptyList();

    String periodFrom = (from != null && !from.isBlank()) ? from : "";
    String periodTo = (to != null && !to.isBlank()) ? to : "";
    String tradePeriod =
        periodFrom.isEmpty() && periodTo.isEmpty()
            ? msg("stock.label.period.all")
            : periodFrom + (periodTo.isEmpty() ? "" : " ~ " + periodTo);

    // 화면에는 오름차순(오래된 것 위)으로 표시 — 조회/페이징은 그대로(1페이지=최신 묶음).
    List<TradeResponse> displayTrades = new java.util.ArrayList<>(pagedTrades);
    java.util.Collections.reverse(displayTrades);
    return new TradeHistoryData(
        displayTrades,
        totalItems,
        currentPage,
        totalPages,
        size,
        periodFrom,
        periodTo,
        totalBuy,
        totalSell,
        totalRealizedProfit,
        totalFee,
        totalTax,
        tradePeriod,
        new Pagination(
            new PageImpl<>(pagedTrades, PageRequest.of(currentPage - 1, size), totalItems)));
  }

  /** 매매 이력 패널이 쓰는 값 묶음. 프래그먼트 응답과 자산성장 화면 인라인 렌더가 같은 계산을 공유한다. */
  public record TradeHistoryData(
      List<TradeResponse> trades,
      int totalItems,
      int currentPage,
      int totalPages,
      int pageSize,
      String from,
      String to,
      BigDecimal totalBuy,
      BigDecimal totalSell,
      BigDecimal totalRealizedProfit,
      BigDecimal totalFee,
      BigDecimal totalTax,
      String tradePeriod,
      Pagination pagination) {}

  private static TradeHistoryData emptyTradeHistory(String from, String to, int size) {
    return new TradeHistoryData(
        List.of(),
        0,
        1,
        0,
        size,
        from == null ? "" : from,
        to == null ? "" : to,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        "",
        null);
  }

  @GetMapping("/trade-history")
  public String tradeHistory(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String timeZone,
      Model model) {
    var userId = UserUtil.getUserId();
    // 로그인이 풀렸는데 빈 목록을 그리면 "해당 기간의 매매 내역이 없습니다" 로 보여, 사용자는 데이터가
    // 없다고 오해한다(실측: 세션 만료 상태에서 다른 조각은 모두 "로그인이 필요합니다" 인데 이 조각만
    // 빈 결과였다). 같은 상황이면 같은 안내를 돌려준다.
    if (userId == null) {
      return loginRequiredView(model);
    }
    // 화면의 계좌/종목 필터를 그대로 이어받는다. 없으면 예전처럼 전체를 본다.
    TradeHistoryData data =
        buildTradeHistoryData(
            userId, from, to, page, size, null, accountIdList, stockItemIdList, null);
    addTradeHistoryAttributes(
        model, data, net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone));
    model.addAttribute(
        "tradeHistoryFilterQuery", tradeHistoryFilterQuery(accountIdList, stockItemIdList));
    return "stock/htmx/tradeHistory";
  }

  private static void addTradeHistoryAttributes(
      Model model, TradeHistoryData data, java.time.ZoneId zone) {
    // 날짜 칸은 이 화면의 기간 표시와 같은 존으로 찍는다. timeZone 이 없으면 예전처럼 서버 존이다.
    model.addAttribute("zone", zone);
    model.addAttribute("trades", data.trades());
    model.addAttribute("totalItems", data.totalItems());
    model.addAttribute("currentPage", data.currentPage());
    model.addAttribute("totalPages", data.totalPages());
    model.addAttribute("pagination", data.pagination());
    model.addAttribute("pageSize", data.pageSize());
    model.addAttribute("from", data.from());
    model.addAttribute("to", data.to());
    model.addAttribute("totalBuy", data.totalBuy());
    model.addAttribute("totalSell", data.totalSell());
    model.addAttribute("totalRealizedProfit", data.totalRealizedProfit());
    model.addAttribute("totalFee", data.totalFee());
    model.addAttribute("totalTax", data.totalTax());
    model.addAttribute("tradePeriod", data.tradePeriod());
  }

  private ZoneId resolveZoneId(String timeZone) {
    return resolveZoneIdOrDefault(timeZone);
  }

  /**
   * 요약만 필요한 호출용. 시리즈와 요약을 함께 주는 엔드포인트를 재사용하되 시리즈는 받지 않는다.
   *
   * <p>실측(사용자 실데이터, {@code granularity=DAILY}): 시리즈까지 받으면 전체 기간 응답이 1,655,289 바이트인데 이 메서드가 실제로 쓰는
   * 요약+연도별은 8,420 바이트다 — <b>99.5% 를 받아서 버렸다</b>(6,442 포인트). 5 년 99.3%, 1 년 98.4%.
   *
   * <p>{@code includeSeries=false} 를 모르는 옛 api-stock 은 이 파라미터를 무시하고 지금까지처럼 전체를 돌려준다. 그래도 이 메서드는 요약만
   * 꺼내 쓰므로 동작은 같다.
   */
  /**
   * 기간 요약을 읽는다. 테스트에서 직접 부르려고 package-private 로 둔다.
   *
   * <p>답이 두 갈래라는 것이 핵심이다 &mdash; 입력이 없거나 말이 안 되는 기간이면 {@code null}(정상적으로 계산할 값이 없음), 원격 호출이 실패하면
   * <b>예외를 그대로 올린다</b>. 예전에는 둘 다 {@code null} 이라 화면에서 구분되지 않았다.
   */
  TradeProfitTimeSeriesSummary loadPeriodSummary(
      UUID userId, String from, String to, String timeZone) {
    // 입력이 없거나 말이 안 되는 기간은 '계산할 값이 없다'(정상). null 로 답한다.
    if (userId == null || from == null || from.isBlank() || to == null || to.isBlank()) {
      return null;
    }
    LocalDate fromDate;
    LocalDate toDate;
    try {
      fromDate = LocalDate.parse(from);
      toDate = LocalDate.parse(to);
    } catch (java.time.format.DateTimeParseException ex) {
      return null;
    }
    if (toDate.isBefore(fromDate)) {
      return null;
    }
    ZoneId zone = resolveZoneId(timeZone);
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);
    request.setStartDate(fromDate.atStartOfDay(zone).toInstant());
    request.setEndDate(toDate.plusDays(1).atStartOfDay(zone).toInstant());
    request.setTimeZone(timeZone);
    var params = request.toParams();
    params.add("granularity", "DAILY");
    params.add("includeSeries", "false");
    // 원격 호출 실패는 여기서 삼키지 않는다. 호출자가 '불러오지 못했다' 로 답해야 하기 때문이다.
    var result = tradeProfitClient.timeSeriesWithSummary(params);
    return result != null ? result.summary() : null;
  }
}
