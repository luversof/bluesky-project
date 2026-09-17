package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.support.StockViewSupport;
import net.luversof.web.gate.stock.util.StockFormatUtil;
import net.luversof.web.gate.stock.util.StockOwnershipUtil;
import net.luversof.web.gate.stock.util.StockRangePresetUtil;
import net.luversof.web.gate.stock.util.StockZoneUtil;

/**
 * 종목 상세 · 계좌 상세 화면.
 *
 * <p>{@code StockViewController} 가 2,863 줄이라 상세 화면 하나를 고칠 때마다 배당 기준 관리나 시뮬레이터 코드를 지나다녀야 했다. 이 둘은 조회
 * 전용이고(쓰는 엔드포인트가 없다) 다른 화면과 나눠 쓰는 것이 인증 잔손질뿐이라, 가장 깨끗하게 떨어지는 자리였다.
 *
 * <p>동작은 그대로다 &mdash; 옮기기만 했다.
 */
@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockDetailViewController {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(StockDetailViewController.class);

  @Autowired private AccountClient accountClient;

  @Autowired private StockItemClient stockItemClient;

  @Autowired private TradeClient tradeClient;

  @Autowired private TradeProfitClient tradeProfitClient;

  @Autowired private DividendClient dividendClient;

  @Autowired
  private net.luversof.web.gate.stock.httpexchange.DataFirstDateClient dataFirstDateClient;

  /**
   * 이 화면(종목 또는 계좌)의 최초 데이터 일자. 날짜 선택기의 하한이자 '가장 이른 기간으로'(«) 의 목표다.
   *
   * <p>이 값이 없으면 « 는 아무 일도 못 해 비활성으로 그려진다 - 실측 2026-09-13: 상세 두 화면이 이 값을 안 넘겨 눌러도 기간이 그대로였다.
   */
  private String detailDataFirstDate(
      java.util.UUID userId,
      java.util.UUID stockItemId,
      java.util.UUID accountId,
      java.time.ZoneId zone) {
    try {
      var response = dataFirstDateClient.findDataFirstDate(userId, stockItemId, accountId);
      if (response == null) {
        return "";
      }
      return net.luversof.web.gate.stock.util.StockFirstDateUtil.earliestLocalDate(
          response.tradeFirstDate(), response.dividendFirstDate(), zone);
    } catch (Exception ex) {
      // 하한을 모르면 « 가 비활성이 될 뿐이다 - 화면 전체를 막지 않는다.
      log.warn("최초 데이터 일자 조회 실패: userId={}", userId, ex);
      return "";
    }
  }

  /**
   * 기간을 걸지 않은 가격 이력. 고른 기간 안에 시세 점이 하나도 없을 때만 부른다.
   *
   * <p>실패해도 화면을 막지 않는다 - 현재가가 비어 있다고 표시될 뿐이다.
   */
  private List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> fullPriceHistory(
      UUID stockItemId) {
    try {
      var history = stockItemClient.getPriceHistory(stockItemId, new LinkedMultiValueMap<>());
      return history != null ? history : List.of();
    } catch (Exception ex) {
      log.warn("전 구간 가격 이력 조회 실패: stockItemId={}", stockItemId, ex);
      return List.of();
    }
  }

  @Autowired private net.luversof.web.gate.stock.support.StockAsyncSupport stockAsync;

  /** 종목 목록(종목코드 순). 이름/심볼로 종목을 찾을 때 쓴다. */
  private List<StockItem> loadStockItems() {
    return StockViewSupport.sortedBySymbol(stockItemClient.getStockItems());
  }

  /**
   * 종목 상세 위쪽의 전환기 목록 &mdash; <b>지금 보유 중인 종목</b>.
   *
   * <p>예전에는 다른 종목을 보려면 뒤로 가서 목록을 다시 찾아야 했다. 평가액이 큰 것부터 두어, 자주 보는 것이 위에 온다.
   *
   * <p>전량 매도한 종목을 보고 있으면 스냅샷에 없다. 그때도 지금 보는 것을 목록 맨 앞에 넣어, 어디에 있는지 알 수 있게 한다.
   */
  private List<net.luversof.web.gate.stock.domain.DetailNavEntry> stockNavEntries(
      List<net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem> holdings,
      UUID currentId,
      net.luversof.web.gate.stock.domain.StockItem currentItem,
      String currentQuery) {
    List<net.luversof.web.gate.stock.domain.DetailNavEntry> entries = new ArrayList<>();
    boolean currentIncluded = false;
    if (holdings != null) {
      var sorted = new ArrayList<>(holdings);
      sorted.sort(
          Comparator.comparing(
                  (net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem h) ->
                      h.value() != null ? h.value() : BigDecimal.ZERO)
              .reversed());
      for (var holding : sorted) {
        if (holding == null || holding.stockItemId() == null) {
          continue;
        }
        boolean current = holding.stockItemId().equals(currentId);
        currentIncluded = currentIncluded || current;
        entries.add(
            new net.luversof.web.gate.stock.domain.DetailNavEntry(
                holding.name() != null ? holding.name() : holding.symbol(),
                String.format("%,d", StockFormatUtil.displayWon(holding.value())),
                net.luversof.web.gate.stock.util.StockTabLinkUtil.switchHref(
                    "/stock/item", currentQuery, "stockItemId", holding.stockItemId().toString()),
                current));
      }
    }
    if (!currentIncluded && currentItem != null && currentItem.id() != null) {
      entries.add(
          0,
          new net.luversof.web.gate.stock.domain.DetailNavEntry(
              currentItem.name() != null ? currentItem.name() : currentItem.symbol(),
              "",
              net.luversof.web.gate.stock.util.StockTabLinkUtil.switchHref(
                  "/stock/item", currentQuery, "stockItemId", currentItem.id().toString()),
              true));
    }
    return entries;
  }

  /** 계좌 상세 위쪽의 전환기 목록. 계좌는 몇 개뿐이라 이름만으로 충분하다. */
  private List<net.luversof.web.gate.stock.domain.DetailNavEntry> accountNavEntries(
      List<Account> accounts, UUID currentId, String currentQuery) {
    List<net.luversof.web.gate.stock.domain.DetailNavEntry> entries = new ArrayList<>();
    if (accounts == null) {
      return entries;
    }
    for (Account account : accounts) {
      if (account == null || account.id() == null) {
        continue;
      }
      entries.add(
          new net.luversof.web.gate.stock.domain.DetailNavEntry(
              account.name() != null ? account.name() : "-",
              "",
              net.luversof.web.gate.stock.util.StockTabLinkUtil.switchHref(
                  "/stock/account", currentQuery, "accountId", account.id().toString()),
              account.id().equals(currentId)));
    }
    return entries;
  }

  /** 종목 상세: 한 종목의 보유/손익 요약 + 매매·배당 내역을 모아 보여준다(기존 엔드포인트 재활용). */
  @BlueskyPreAuthorize
  /**
   * 날짜 없이 프리셋 상태({@code rangeMode})만 실려 온 요청의 기간을 정한다.
   *
   * <p>실측 2026-09-11: 상세 화면은 {@code rangeMode} 를 모델에 넘기기만 하고 기간으로 바꾸지 않았다. 그래서 {@code
   * /stock/item?stockItemId=..&rangeMode=ytd} 로 들어가면 올해가 아니라 전 기간이 나왔고(삼성전자 실현손익 138,569,333 = 전 기간
   * 값), 프리셋 버튼도 하나도 눌린 상태가 아니었다. 목록 화면들은 이미 같은 규칙을 서버에서 계산한다.
   *
   * <p>날짜가 하나라도 실려 있으면 그것이 기간이다. {@code all} 은 기간을 걸지 않겠다는 뜻이라 그대로 둔다. 프리셋 상태가 아예 없으면 이 화면의 기존
   * 기본값(기간 없음 = 전체)을 유지한다 &mdash; 목록 화면의 올해 기본값을 여기에 끌어오지 않는다.
   *
   * @return {@code [startDate, endDate]}
   */
  private java.time.Instant[] resolveRange(
      String rangeMode, java.time.Instant startDate, java.time.Instant endDate, String timeZone) {
    if (startDate != null
        || endDate != null
        || !StockRangePresetUtil.hasMode(rangeMode)
        || StockRangePresetUtil.isAll(rangeMode)) {
      // 뒤집힌 기간은 목록 화면들과 같은 규칙으로 바로잡는다 - 실측 2026-09-11: 이 화면만 뒤집힌 채로 두어
      // 배지가 "2026-09-12 ~ 2025-12-31" 로 뜨고 내역이 비었다.
      return StockRangePresetUtil.ordered(startDate, endDate);
    }
    var preset = StockRangePresetUtil.resolve(rangeMode, StockZoneUtil.resolve(timeZone));
    return new java.time.Instant[] {preset.start(), preset.end()};
  }

  @GetMapping("/item")
  public String stockItemDetailPage(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(required = false) String stockItemId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) java.time.Instant startDate,
      @RequestParam(required = false) java.time.Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    UUID userId = UserUtil.getUserId();
    java.time.Instant[] resolvedRange = resolveRange(rangeMode, startDate, endDate, timeZone);
    startDate = resolvedRange[0];
    endDate = resolvedRange[1];

    // id 우선(잘못된/빈 값은 무시), 없으면 종목명(또는 심볼)으로 해석.
    StockItem stockItem = null;
    UUID parsedId = parseUuidOrNull(stockItemId);
    if (parsedId != null) {
      stockItem = stockItemClient.getStockItemById(parsedId).orElse(null);
    } else if (name != null && !name.isBlank()) {
      String target = name.trim();
      stockItem =
          loadStockItems().stream()
              .filter(
                  item ->
                      item != null
                          && (target.equals(item.name()) || target.equalsIgnoreCase(item.symbol())))
              .findFirst()
              .orElse(null);
    }
    // 초기 진입(비 htmx)은 셸만 렌더하고, 콘텐츠는 전역 기간과 함께 htmx로 로드한다.
    // (풀페이지 새로고침 시에도 선택 기간이 서버에 적용되도록.)
    if (request.getHeader("HX-Request") == null) {
      // 없는 id 는 404 다. 화면은 그대로 그리되(껍데기 + '찾을 수 없음' 조각) 상태 코드로는 사실을 말한다 - 실측 2026-09-09:
      // ?stockItemId=garbage 가 200 이라 감시·북마크·검색엔진 모두 '있는 화면' 으로 봤다.
      if (stockItem == null || stockItem.id() == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      // 404 인데 문서 제목이 정상 화면과 같으면 탭·방문기록·북마크가 "종목 상세" 라고 말한다
      // (실측 2026-09-12: 없는 id 도 제목이 "종목 상세 · Bluesky Stock"). 없는 경로의 404 는 이미 제목으로 말한다.
      // 조건을 변수로 묶지 않는다 - DetailNotFoundTest 가 "404 는 엔티티가 없을 때만" 을 이 형태로 고정한다.
      model.addAttribute("notFound", stockItem == null || stockItem.id() == null);
      model.addAttribute("contentReady", false);
      model.addAttribute(
          "stockItemIdParam",
          stockItem != null && stockItem.id() != null
              ? stockItem.id().toString()
              : (stockItemId != null ? stockItemId : ""));
      return "stock/stockItemDetail";
    }
    model.addAttribute("contentReady", true);
    // htmx 요청은 조각 템플릿만 렌더한다. 예전엔 레이아웃 전체를 렌더하고 클라이언트가 hx-select 로 골라 썼다
    // (실측 2026-09-10: 종목 상세 조각 107,201자 중 레이아웃 28,447자 = 27% 가 버려졌다).

    if (stockItem == null || stockItem.id() == null) {
      model.addAttribute("stockItem", null);
      return "stock/htmx/stockItemDetailContent";
    }
    model.addAttribute("stockItem", stockItem);
    UUID resolvedId = stockItem.id();

    // 종목이 정해진 뒤의 여섯 조회는 서로 의존이 없다. 순차로 던지면 응답시간이 그대로 합산된다
    // (실측: 백엔드 7회 34.1ms 인데 화면은 84.5ms — 왕복이 줄줄이 이어진 탓).
    // 요청 파라미터를 먼저 다 만들고 한꺼번에 던진 뒤, 쓰는 자리에서 결과를 받는다.
    TradeProfitRequest profitRequest = new TradeProfitRequest();
    profitRequest.setUserId(userId);
    profitRequest.setStockItemIdList(List.of(resolvedId));
    profitRequest.setStartDate(startDate);
    profitRequest.setEndDate(endDate);
    var profitParams = profitRequest.toParams();

    TradeProfitRequest snapshotRequestPre = new TradeProfitRequest();
    snapshotRequestPre.setUserId(userId);
    snapshotRequestPre.setStockItemIdList(List.of(resolvedId));
    var snapshotParams = snapshotRequestPre.toParams();

    var tradeSearchParamsPre =
        new TradeSearchRequest(userId, null, List.of(resolvedId), startDate, endDate).toParams();

    MultiValueMap<String, String> dividendParamsPre = new LinkedMultiValueMap<>();
    dividendParamsPre.add("userId", userId.toString());
    dividendParamsPre.add("stockItemIdList", resolvedId.toString());
    if (startDate != null) {
      dividendParamsPre.add("startDate", startDate.toString());
    }
    if (endDate != null) {
      dividendParamsPre.add("endDate", endDate.toString());
    }

    TradeProfitRequest seriesRequestPre = new TradeProfitRequest();
    seriesRequestPre.setUserId(userId);
    seriesRequestPre.setStockItemIdList(List.of(resolvedId));
    seriesRequestPre.setStartDate(startDate);
    seriesRequestPre.setEndDate(endDate);
    var seriesParamsPre = seriesRequestPre.toParams();
    seriesParamsPre.add("granularity", "AUTO");
    // 차트용 시리즈와 '기간별 손익' 표를 한 번의 시뮬레이션으로 함께 받는다. 따로 부르면 같은 이력을
    // 두 번 돌린다. 쪼갬 단위(달/해)는 조회 기간 길이에 따라 api-stock 이 고른다.
    seriesParamsPre.add("breakdown", "AUTO");

    // 기간을 고르지 않은 기본 진입에서는 두 파라미터가 같아져 같은 호출이 두 번 나갔다(실측 2026-09-10).
    var calls = stockAsync.deduper();
    var profitsFuture =
        calls.supply(
            List.of("calculateProfit", profitParams),
            () -> tradeProfitClient.calculateProfit(profitParams));
    var snapshotFuture =
        calls.supply(
            List.of("calculateProfit", snapshotParams),
            () -> tradeProfitClient.calculateProfit(snapshotParams));
    var tradesFuture = stockAsync.supply(() -> tradeClient.findTrades(tradeSearchParamsPre));
    var dividendsFuture = stockAsync.supply(() -> dividendClient.findDividends(dividendParamsPre));
    var timeSeriesFuture =
        stockAsync.supply(() -> tradeProfitClient.timeSeriesWithSummary(seriesParamsPre));
    var accountsFuture = stockAsync.supply(() -> accountClient.getAccountsByUserId(userId));
    // 보유 기간 · 연평균 수익률 - 자산 현황과 같은 입력(최초 매수일 · 종목별 누적 배당 · 하루치 순현금흐름)을 같은 API 로 받는다.
    // 기간 선택과 무관한 '보유 전체' 값이라 기간을 싣지 않는다(사용자 요청 2026-09-17). 존은 '오늘' 을 정하는 존과 같게 싣는다 -
    // 흐름의 날짜와 오늘이 다른 존이면 하루가 어긋난다.
    MultiValueMap<String, String> holdingReturnParams = new LinkedMultiValueMap<>();
    holdingReturnParams.add("userId", userId.toString());
    holdingReturnParams.add("stockItemIdList", resolvedId.toString());
    holdingReturnParams.add(
        "timeZone", net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone).getId());
    var firstBuyDateFuture =
        stockAsync.supply(() -> tradeClient.findFirstBuyDateByStockItem(holdingReturnParams));
    var dividendTotalFuture =
        stockAsync.supply(() -> dividendClient.findDividendTotalByStockItem(holdingReturnParams));
    var cashFlowFuture =
        stockAsync.supply(() -> tradeClient.findCashFlowsByStockItem(holdingReturnParams));
    // 전환기(다른 종목으로 바로 가기) 목록. 다른 조회와 함께 던지므로 왕복이 늘지 않는다
    // (실측 2026-08-31: holdingsSnapshot 50~60ms, 이 화면의 다른 호출보다 빠르다).
    var navHoldingsParams = new LinkedMultiValueMap<String, String>();
    navHoldingsParams.add("userId", userId.toString());
    navHoldingsParams.add(
        "date",
        java.time.LocalDate.now(net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone))
            .toString());
    if (timeZone != null && !timeZone.isBlank()) {
      navHoldingsParams.add("timeZone", timeZone);
    }
    var navHoldingsFuture =
        stockAsync.supply(() -> tradeProfitClient.holdingsSnapshot(navHoldingsParams));

    // 주가 차트용 일별 종가. 다른 조회와 함께 던지므로 왕복이 늘지 않는다
    // (실측 2026-09-01 삼성전자 전 구간 1,593 행 · 71.8 KB · 94ms).
    var priceHistoryParams = new LinkedMultiValueMap<String, String>();
    if (startDate != null) {
      priceHistoryParams.add(
          "startDate",
          startDate
              .atZone(net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone))
              .toLocalDate()
              .toString());
    }
    if (endDate != null) {
      // endDate 는 배타적이라 하루를 빼야 화면의 다른 날짜 칸과 같은 마지막 날이 된다.
      priceHistoryParams.add(
          "endDate",
          endDate
              .atZone(net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone))
              .toLocalDate()
              .minusDays(1)
              .toString());
    }
    var priceHistoryFuture =
        stockAsync.supply(() -> stockItemClient.getPriceHistory(resolvedId, priceHistoryParams));

    List<TradeProfit> profits =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(profitsFuture);
    if (profits == null) {
      profits = List.of();
    }
    BigDecimal totalBuyCost = sumTradeProfit(profits, TradeProfit::totalBuyCost);
    // 표시하는 실현손익은 매도 거래에 기록된 값(증권사 기준)으로 통일한다. 앱이 평균단가로 다시 계산한
    // realizedProfitNet 을 쓰면 같은 화면의 거래 행 합계와 어긋난다(실측 2026-08-23: 28 종목이 달랐고
    // 합계 차이 0.11%). 매도 54 건 전부 기록값이 있어 잃는 값은 없다.
    BigDecimal realizedProfit = sumTradeProfit(profits, TradeProfit::realizedProfit);
    // 실현 손익 카드의 수익률 분모 - 판 주식의 원가(매도 금액 - 기록 실현손익). 실현손익과 같은 기간 행에서 구해야 기간을 바꿔도
    // 분자와 분모가 같은 매도를 말한다(사용자 선택 2026-09-17 "카드마다 그 원금 대비"). 이 기간에 판 적이 없으면 0 이고 카드는 비율 대신
    // 사유를 적는다.
    BigDecimal periodSellAmount = sumTradeProfit(profits, TradeProfit::totalSellAmount);
    BigDecimal realizedCostBasis =
        periodSellAmount.signum() > 0 ? periodSellAmount.subtract(realizedProfit) : BigDecimal.ZERO;

    // 보유 스냅샷(수량·평균단가·현재가·평가)은 기간 미적용 호출로 구한다.
    // API 의 totalBuyCost 는 "기간 내 매수원가"이고 holdingQuantity 는 전체 누적 보유량이라
    // 기간 원가 ÷ 누적 수량 식의 혼합 계산은 잘못된 평균단가를 만들고,
    // 기간이 지정되면(hasDateRange) API 가 현재가/평가를 계산하지 않아 0 으로 표시된다.
    List<TradeProfit> snapshotProfits =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(snapshotFuture);
    if (snapshotProfits == null) {
      snapshotProfits = List.of();
    }
    int holdingQuantity = snapshotProfits.stream().mapToInt(TradeProfit::holdingQuantity).sum();
    BigDecimal evaluationAmount = sumTradeProfit(snapshotProfits, TradeProfit::evaluationAmount);
    // 평가손익도 실현손익과 같은 기준(기본값)을 쓴다. 두 값은 각각 닫힌 삼중항이라
    // (기록실현+기본평가=totalProfit, Net실현+Net평가=totalProfitNet) 섞으면 '실현+평가=총' 이
    // 깨진다(실측 2026-08-23: 혼합 시 61행 중 18행 불일치).
    // 자산현황·포트폴리오가 이미 기본값을 쓰므로 그쪽에 맞춘다.
    BigDecimal evaluationProfit = sumTradeProfit(snapshotProfits, TradeProfit::evaluationProfit);
    BigDecimal currentPrice =
        snapshotProfits.stream()
            .map(TradeProfit::currentPrice)
            .filter(java.util.Objects::nonNull)
            .filter(price -> price.signum() > 0)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    // 평균단가 = 보유원가 합 ÷ 보유수량 합 (행별 averageBuyPrice 의 수량 가중 평균)
    BigDecimal holdingCost =
        snapshotProfits.stream()
            .map(
                p ->
                    (p.averageBuyPrice() != null ? p.averageBuyPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(p.holdingQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageBuyPrice =
        holdingQuantity > 0
            ? holdingCost.divide(
                BigDecimal.valueOf(holdingQuantity), 0, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // 매매 내역 (이 종목, 최신순, 기간 적용)
    List<TradeResponse> trades =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(tradesFuture);
    if (trades == null) {
      trades = List.of();
    }
    trades =
        trades.stream()
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    // 배당 내역 (이 종목, 최신순)
    List<DividendResponse> dividends =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dividendsFuture);
    if (dividends == null) {
      dividends = List.of();
    }
    dividends =
        dividends.stream()
            .sorted(
                Comparator.comparing(
                    DividendResponse::payDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    BigDecimal totalDividend =
        dividends.stream()
            .map(dividend -> dividend.netAmount() != null ? dividend.netAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 보유 평가액·원가 추이 (차트용, 기간 적용 AUTO 단위) + 기간별 손익 표
    var timeSeriesResult =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(timeSeriesFuture);
    List<TradeProfitTimeSeriesPoint> timeSeries =
        timeSeriesResult != null && timeSeriesResult.series() != null
            ? timeSeriesResult.series()
            : List.of();
    model.addAttribute("timeSeries", timeSeries);
    // 구간이 하나뿐이면 위의 합산 손익을 되풀이할 뿐이라 조각이 스스로 그리지 않는다.
    model.addAttribute(
        "periodBreakdown",
        timeSeriesResult != null && timeSeriesResult.breakdown() != null
            ? timeSeriesResult.breakdown()
            : List.of());
    // 표를 못 내는 구간에서도 자리를 비우지 않는다 - 자산 성장과 같은 규칙을 쓴다.
    // 이 화면은 해 단위 표도 그리므로 monthlyOnly 는 false 다.
    model.addAttribute(
        "periodBreakdownNote",
        net.luversof.web.gate.stock.util.StockBreakdownNoteUtil.note(
            timeSeriesResult != null ? timeSeriesResult.breakdown() : null, false));
    var priceHistory =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(priceHistoryFuture);
    model.addAttribute("priceHistory", priceHistory != null ? priceHistory : List.of());
    // 거래한 적이 없는 종목은 손익 행이 없어 현재가가 0 으로 떨어졌다 - 실측 2026-09-11: 기업은행(024110)
    // 상세가 "현재가 0" 을 찍었지만 가격 이력에는 2026-04-03 종가 21,350 이 있었다. 값이 없는 것과 0 원인 것은
    // 다르고, 여기서는 값이 있다. 같은 이력으로 가격 차트를 이미 그리므로 그 마지막 점을 쓴다.
    var lastPricePoint =
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.lastPricePoint(priceHistory);
    // 그 이력은 고른 기간으로 잘려 있다 - 창 안에 시세 점이 하나도 없으면 폴백도 못 걸려 다시 0 이 남는다.
    // 실측 2026-09-13 기업은행(024110): '전체' 는 21,350 인데 '최근 1개월' 과 2016 년은 둘 다 "현재가 0" 이었다
    // (이 종목 이력은 2026-03-25~04-03 여덟 점뿐이다). 현재가는 기간 값이 아니라 시점 값이다 - 보유 종목은
    // 기간을 좁혀도 같은 값을 보여준다(삼성전자 2016 년 화면도 269,500). 기간 없는 호출로 마지막 점을 한 번 더 구한다.
    // 이 추가 호출은 창이 빈 경우에만 나간다(실측 25ms · 369B).
    if (currentPrice.signum() == 0 && lastPricePoint == null) {
      lastPricePoint =
          net.luversof.web.gate.stock.util.StockPriceBasisUtil.lastPricePoint(
              fullPriceHistory(resolvedId));
    }
    if (currentPrice.signum() == 0 && lastPricePoint != null) {
      currentPrice = lastPricePoint.closePrice();
    }

    // 합산 손익의 '평가' 몫은 <b>기간 평가 변동</b>이다(기말 평가손익 - 기초 평가손익).
    //
    // 예전에는 기간이 아닌 현재 시점 평가손익을 더했다. 그러면 아래 '기간별 손익' 표의 합과 맞지 않는다
    // - 실측 2026-09-01 삼성전자 '올해' 2.9 억 차이, '최근 1년' 은 부호까지 반대(-0.26 억)였다.
    // 같은 화면의 두 숫자가 안 맞으면 어느 쪽이 맞는지 알 수 없다.
    //
    // 평가 변동으로 바꾸면 (평가 변동 + 실현손익 + 배당) 이 요약의 periodProfit 과 1 원 오차 없이 같다
    // (실측 두 기간 모두 차이 0). 표의 구간별 손익을 다 더한 값도 같은 수다.
    var periodSummary = timeSeriesResult != null ? timeSeriesResult.summary() : null;
    BigDecimal periodUnrealizedDelta =
        periodSummary == null
            ? null
            : (periodSummary.unrealizedEnd() != null
                    ? periodSummary.unrealizedEnd()
                    : BigDecimal.ZERO)
                .subtract(
                    periodSummary.unrealizedStart() != null
                        ? periodSummary.unrealizedStart()
                        : BigDecimal.ZERO);
    model.addAttribute("periodUnrealizedDelta", periodUnrealizedDelta);
    // 비율도 자산 성장 화면과 같은 정의를 쓴다(기초 평가액 + 기간 중 순유입 원금 대비).
    model.addAttribute(
        "periodProfitRatePct", periodSummary != null ? periodSummary.periodProfitRatePct() : null);
    model.addAttribute(
        "stockNavEntries",
        stockNavEntries(
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(navHoldingsFuture),
            resolvedId,
            stockItem,
            request.getQueryString()));
    model.addAttribute(
        "chartFormatter",
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(java.time.ZoneId.systemDefault()));

    // 기간 필터 모델 (날짜 필터 바)
    // 바꾸는 규칙은 StockZoneUtil.resolve 한 곳에만 둔다(잘못된 값이면 서버 기본 존).
    java.time.ZoneId filterZone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
    model.addAttribute(
        "filterStartLocal", startDate != null ? startDate.atZone(filterZone).toLocalDate() : null);
    model.addAttribute(
        "filterEndLocal",
        endDate != null ? endDate.atZone(filterZone).toLocalDate().minusDays(1) : null);
    model.addAttribute("filterStartInstant", startDate);
    model.addAttribute("filterEndInstant", endDate);
    model.addAttribute("filterTimeZone", timeZone);
    model.addAttribute("filterRangeMode", rangeMode);
    // '전체' 는 날짜를 안 보내 기간 배지가 빌 수밖에 없다. 그때 대신 적을, 이 화면이 덮은 구간.
    // 사용자 전체의 최초 일자가 아니라 <b>이 종목/계좌의</b> 시계열에서 뽑아야 맞는 날짜가 나온다.
    var covered =
        net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
            timeSeries,
            net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint::timestamp,
            filterZone);
    String detailFirstDate = detailDataFirstDate(userId, resolvedId, null, filterZone);
    // 배지의 시작은 시계열의 첫 점과 이 화면 데이터의 최초일 중 이른 쪽이다 - 시계열은 평가액이
    // 잡히는 날부터라 첫 거래보다 늦게 시작할 수 있다(실측 2026-09-13: 삼성전자 19 일 · 한투 위탁 13 일).
    model.addAttribute(
        "coveredStartLocal",
        net.luversof.web.gate.stock.util.StockFirstDateUtil.coveredStart(
            covered.startDate(), detailFirstDate));
    // 시계열이 없는 종목(배당만 있는 경우)은 끝을 못 정해 배지가 "~ ?" 로 나갔다 -
    // 화면이 그린 매매·배당의 마지막 날로 메운다(실측 2026-09-13: 하나금융지주).
    java.time.LocalDate contentLast =
        net.luversof.web.gate.stock.util.StockFirstDateUtil.later(
            net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
                    trades,
                    net.luversof.web.gate.stock.dto.response.TradeResponse::tradeDate,
                    filterZone)
                .endDate(),
            net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
                    dividends,
                    net.luversof.web.gate.stock.dto.response.DividendResponse::payDate,
                    filterZone)
                .endDate());
    model.addAttribute(
        "coveredEndLocal",
        net.luversof.web.gate.stock.util.StockFirstDateUtil.coveredEnd(
            covered.endDate(), contentLast));
    // 이 종목의 최초 일자. 날짜 선택기 하한이자 '가장 이른 기간으로'(«) 의 목표다.
    model.addAttribute("dataFirstDate", detailFirstDate);
    // 빈 기간 안내에 "전체 기간으로 보기" 를 띄울지 화면이 판단하려면 지금 모드를 알아야 한다.
    model.addAttribute("rangeMode", rangeMode == null ? "" : rangeMode);

    // 보유 전체 기준 보유 기간 · 연평균 수익률(복리). 자산 현황의 종목 줄과 <b>같은 계산 · 같은 입력</b>이다 -
    // 자산 현황의 종목 줄과 같은 계산 · 같은 입력이다 - 연평균은 이 종목의 하루치 순현금흐름에 지금 평가액을 더한 XIRR(사용자 결정
    // 2026-09-17), 상쇄율은 평가손익과 누적 배당(세후, totalByStockItem). 두 화면이 다른 식을 쓰면 같은 종목이 두 연평균을 갖는다.
    // 사용자 요청 2026-09-17: "종목 상세에서 보유 손익 + 배당 손익, 보유 기간 대비 손익".
    java.util.Map<UUID, java.time.LocalDate> firstBuyDates =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(firstBuyDateFuture);
    java.util.Map<UUID, BigDecimal> dividendTotals =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dividendTotalFuture);
    java.util.Map<UUID, List<net.luversof.web.gate.stock.dto.response.StockCashFlowResponse>>
        cashFlows = net.luversof.web.gate.stock.support.StockAsyncSupport.join(cashFlowFuture);
    java.time.LocalDate holdingFirstBuyDate =
        firstBuyDates != null ? firstBuyDates.get(resolvedId) : null;
    BigDecimal holdingDividendTotal =
        dividendTotals != null && dividendTotals.get(resolvedId) != null
            ? dividendTotals.get(resolvedId)
            : BigDecimal.ZERO;
    List<net.luversof.web.gate.stock.dto.response.StockCashFlowResponse> holdingCashFlows =
        cashFlows != null ? cashFlows.get(resolvedId) : null;
    model.addAttribute("holdingFirstBuyDate", holdingFirstBuyDate);
    model.addAttribute(
        "holdingReturn",
        net.luversof.web.gate.stock.util.StockHoldingReturnUtil.of(
            holdingFirstBuyDate,
            java.time.LocalDate.now(
                net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone)),
            holdingCashFlows,
            evaluationAmount,
            evaluationProfit,
            holdingDividendTotal));
    // 합산 손익의 세 몫(평가 변동 · 실현 · 배당)에 붙일 비율의 분모. 합산 비율(periodProfitRatePct)과 같은 분모라야 세 비율의
    // 합이 합산 비율이 된다 - api-stock 은 '넣어 둔 돈' 을 기초 평가액 + max(기간 순유입, 0) 으로 둔다(TradeProfitService).
    model.addAttribute(
        "periodCapitalBase",
        periodSummary == null
            ? null
            : (periodSummary.openingValue() != null
                    ? periodSummary.openingValue()
                    : BigDecimal.ZERO)
                .add(
                    (periodSummary.principalDelta() != null
                            ? periodSummary.principalDelta()
                            : BigDecimal.ZERO)
                        .max(BigDecimal.ZERO)));

    model.addAttribute("holdingQuantity", holdingQuantity);
    model.addAttribute("averageBuyPrice", averageBuyPrice);
    model.addAttribute("currentPrice", currentPrice);
    // 그래도 값이 없는 종목이 있다 - 실측: 86 종목 중 4 종목(0177R0 · 0190G0 · 0210E0 · 0219E0)은 시세 이력 자체가 없다.
    // 그 화면에서 "현재가 0" 은 잰 값처럼 읽힌다. 같은 화면의 다른 구역은 모두 "…없습니다" 라고 말하는데 이 카드만 0 을 적었다.
    model.addAttribute("currentPriceUnknown", currentPrice.signum() == 0);
    model.addAttribute("evaluationAmount", evaluationAmount);
    model.addAttribute("evaluationProfit", evaluationProfit);
    model.addAttribute("realizedProfit", realizedProfit);
    model.addAttribute("realizedCostBasis", realizedCostBasis);
    model.addAttribute("totalBuyCost", totalBuyCost);
    model.addAttribute("totalDividend", totalDividend);
    // 계좌별 보유 현황 (이 종목을 보유한 계좌별 분해; 기간 미적용 스냅샷 기준)
    Map<UUID, String> accountNameById = new HashMap<>();
    List<Account> userAccounts =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountsFuture);
    if (userAccounts != null) {
      for (Account acc : userAccounts) {
        if (acc != null && acc.id() != null) {
          accountNameById.put(acc.id(), acc.name() != null ? acc.name() : "-");
        }
      }
    }
    String resolvedStockName = stockItem.name() != null ? stockItem.name() : "-";
    List<TradeProfit> accountHoldings =
        snapshotProfits.stream()
            .filter(p -> p.holdingQuantity() > 0)
            .map(
                p ->
                    TradeProfit.withNames(
                        p, resolvedStockName, accountNameById.getOrDefault(p.accountId(), "-")))
            .sorted(
                Comparator.comparing(
                        (TradeProfit p) ->
                            p.evaluationAmount() != null ? p.evaluationAmount() : BigDecimal.ZERO)
                    .reversed())
            .toList();
    model.addAttribute("accountHoldings", accountHoldings);

    // 이 화면의 "현재가"·평가액도 마지막으로 수집된 종가 기준이다. 어느 날 기준인지 밝히지 않으면
    // 실시간 시세로 오해할 수 있다(실측: 오늘이 2026-08-22 인데 보유 15종목의 currentPriceDate 가
    // 모두 2026-08-20 이었다). 자산현황·포트폴리오와 같은 표기를 쓴다.
    // 전량 매도한 종목은 보유 행이 없어 보유 기준으로는 날짜가 나오지 않는다. 그러면 안내 줄만 사라지고
    // 멈춰 있는 현재가는 그대로 남아 오늘 값처럼 보인다. 이 화면은 종목 하나만 다루므로 그 종목의
    // 마지막 종가 일자로 되돌린다.
    // 기준일도 같은 규칙으로 되돌린다 - 값만 채우고 날짜가 없으면 멈춰 있는 종가가 오늘 값처럼 보인다.
    java.time.LocalDate priceBasisDate =
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.priceBasisDateWithFallback(
            snapshotProfits);
    if (priceBasisDate == null && lastPricePoint != null) {
      priceBasisDate = lastPricePoint.tradeDate();
    }
    model.addAttribute("priceBasisDate", priceBasisDate);

    model.addAttribute("trades", trades);
    model.addAttribute("dividends", dividends);
    return "stock/htmx/stockItemDetailContent";
  }

  /** 계좌 상세: 한 계좌의 보유/손익 요약 + 보유 종목 + 매매·배당 내역(종목 상세와 대칭, 필터 키만 account). */
  @BlueskyPreAuthorize
  @GetMapping("/account")
  public String accountDetailPage(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) java.time.Instant startDate,
      @RequestParam(required = false) java.time.Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    UUID userId = UserUtil.getUserId();
    java.time.Instant[] resolvedRange = resolveRange(rangeMode, startDate, endDate, timeZone);
    startDate = resolvedRange[0];
    endDate = resolvedRange[1];

    UUID parsedId = parseUuidOrNull(accountId);
    // 계좌 단건 조회는 소유자를 가리지 않으므로 여기서 확인한다(규칙과 근거는 StockOwnershipUtil).
    Account account =
        parsedId != null
            ? StockOwnershipUtil.ownedOrNull(accountClient.getAccountById(parsedId), userId)
            : null;

    // 초기 진입(비 htmx)은 셸만 렌더하고, 콘텐츠는 전역 기간과 함께 htmx로 로드한다.
    // (풀페이지 새로고침 시에도 선택 기간이 서버에 적용되도록.)
    if (request.getHeader("HX-Request") == null) {
      // 없는 id 는 404 다. 화면은 그대로 그리되(껍데기 + '찾을 수 없음' 조각) 상태 코드로는 사실을 말한다 - 실측 2026-09-09:
      // ?accountId=garbage 가 200 이라 감시·북마크·검색엔진 모두 '있는 화면' 으로 봤다.
      if (account == null || account.id() == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      model.addAttribute("notFound", account == null || account.id() == null);
      model.addAttribute("contentReady", false);
      model.addAttribute(
          "accountIdParam",
          account != null && account.id() != null
              ? account.id().toString()
              : (accountId != null ? accountId : ""));
      return "stock/accountDetail";
    }
    model.addAttribute("contentReady", true);
    // htmx 요청은 조각 템플릿만 렌더한다. 예전엔 레이아웃 전체를 렌더하고 클라이언트가 hx-select 로 골라 썼다
    // (실측 2026-09-10: 종목 상세 조각 107,201자 중 레이아웃 28,447자 = 27% 가 버려졌다).

    if (account == null || account.id() == null) {
      model.addAttribute("account", null);
      return "stock/htmx/accountDetailContent";
    }
    model.addAttribute("account", account);
    UUID resolvedId = account.id();
    model.addAttribute(
        "accountNavEntries",
        accountNavEntries(
            accountClient.getAccountsByUserId(userId), resolvedId, request.getQueryString()));

    // 계좌가 정해진 뒤의 다섯 조회는 서로 의존이 없다. 순차로 던지면 왕복이 줄줄이 이어진다
    // (실측: 백엔드 7회 27.3ms 인데 화면은 75.3ms). 파라미터를 먼저 만들고 한꺼번에 던진다.
    TradeProfitRequest profitRequestPre = new TradeProfitRequest();
    profitRequestPre.setUserId(userId);
    profitRequestPre.setAccountIdList(List.of(resolvedId));
    profitRequestPre.setStartDate(startDate);
    profitRequestPre.setEndDate(endDate);
    var accProfitParams = profitRequestPre.toParams();

    TradeProfitRequest snapshotRequestPre = new TradeProfitRequest();
    snapshotRequestPre.setUserId(userId);
    snapshotRequestPre.setAccountIdList(List.of(resolvedId));
    var accSnapshotParams = snapshotRequestPre.toParams();

    var accTradeParams =
        new TradeSearchRequest(userId, List.of(resolvedId), null, startDate, endDate).toParams();

    MultiValueMap<String, String> accDividendParams = new LinkedMultiValueMap<>();
    accDividendParams.add("userId", userId.toString());
    accDividendParams.add("accountIdList", resolvedId.toString());
    if (startDate != null) {
      accDividendParams.add("startDate", startDate.toString());
    }
    if (endDate != null) {
      accDividendParams.add("endDate", endDate.toString());
    }

    TradeProfitRequest seriesRequestPre = new TradeProfitRequest();
    seriesRequestPre.setUserId(userId);
    seriesRequestPre.setAccountIdList(List.of(resolvedId));
    seriesRequestPre.setStartDate(startDate);
    seriesRequestPre.setEndDate(endDate);
    var accSeriesParams = seriesRequestPre.toParams();
    accSeriesParams.add("granularity", "AUTO");

    // 기간을 고르지 않은 기본 진입에서는 두 파라미터가 같아져 같은 호출이 두 번 나갔다(실측 2026-09-10).
    var accCalls = stockAsync.deduper();
    var accProfitsFuture =
        accCalls.supply(
            List.of("calculateProfit", accProfitParams),
            () -> tradeProfitClient.calculateProfit(accProfitParams));
    var accSnapshotFuture =
        accCalls.supply(
            List.of("calculateProfit", accSnapshotParams),
            () -> tradeProfitClient.calculateProfit(accSnapshotParams));
    var accTradesFuture = stockAsync.supply(() -> tradeClient.findTrades(accTradeParams));
    var accDividendsFuture =
        stockAsync.supply(() -> dividendClient.findDividends(accDividendParams));
    var accTimeSeriesFuture =
        stockAsync.supply(() -> tradeProfitClient.timeSeries(accSeriesParams));
    var accStockItemsFuture = stockAsync.supply(this::loadStockItems);
    // 연평균 수익률(XIRR) 카드 입력 - 이 계좌의 매수 · 매도 · 배당 하루치 순현금흐름(종목별로 온다). 기간 선택과 무관한
    // '이 계좌 전체 거래' 값이라 기간을 싣지 않는다(사용자 선택 2026-09-17). 존은 '오늘' 을 정하는 존과 같게 싣는다 -
    // 흐름의 날짜와 오늘이 다른 존이면 하루가 어긋난다.
    MultiValueMap<String, String> accCashFlowParams = new LinkedMultiValueMap<>();
    accCashFlowParams.add("userId", userId.toString());
    accCashFlowParams.add("accountIdList", resolvedId.toString());
    accCashFlowParams.add(
        "timeZone", net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone).getId());
    var accCashFlowFuture =
        stockAsync.supply(() -> tradeClient.findCashFlowsByStockItem(accCashFlowParams));

    // 종목 id → 종목명 (보유/내역 표의 종목명 + 종목 상세 링크용)
    Map<UUID, String> stockNameById = new HashMap<>();
    net.luversof.web.gate.stock.support.StockAsyncSupport.join(accStockItemsFuture)
        .forEach(
            s -> {
              if (s.id() != null) {
                stockNameById.put(s.id(), s.name() != null ? s.name() : "-");
              }
            });
    model.addAttribute("stockNameById", stockNameById);

    // 기간 지표(실현손익·기간 매수원가)는 기간 적용 호출로 집계
    List<TradeProfit> profits =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accProfitsFuture);
    if (profits == null) {
      profits = List.of();
    }
    BigDecimal totalBuyCost = sumTradeProfit(profits, TradeProfit::totalBuyCost);
    // 표시하는 실현손익은 매도 거래에 기록된 값(증권사 기준)으로 통일한다. 앱이 평균단가로 다시 계산한
    // realizedProfitNet 을 쓰면 같은 화면의 거래 행 합계와 어긋난다(실측 2026-08-23: 28 종목이 달랐고
    // 합계 차이 0.11%). 매도 54 건 전부 기록값이 있어 잃는 값은 없다.
    BigDecimal realizedProfit = sumTradeProfit(profits, TradeProfit::realizedProfit);

    // 보유 종목 테이블/평가 합계는 기간 미적용 스냅샷 호출로 구한다.
    // 기간이 지정되면(hasDateRange) API 가 현재가/평가를 계산하지 않아
    // 보유 종목의 평가금액·평가손익이 전부 0 으로 표시되고 정렬도 무의미해진다.
    List<TradeProfit> snapshotProfits =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accSnapshotFuture);
    if (snapshotProfits == null) {
      snapshotProfits = List.of();
    }
    List<TradeProfit> enriched =
        snapshotProfits.stream()
            .map(
                p ->
                    TradeProfit.withNames(
                        p, stockNameById.getOrDefault(p.stockItemId(), "-"), account.name()))
            .toList();
    List<TradeProfit> holdings =
        enriched.stream()
            .filter(p -> p.holdingQuantity() > 0)
            .sorted(
                Comparator.comparing(
                        (TradeProfit p) ->
                            p.evaluationAmount() != null ? p.evaluationAmount() : BigDecimal.ZERO)
                    .reversed())
            .toList();
    BigDecimal evaluationAmount = sumTradeProfit(enriched, TradeProfit::evaluationAmount);
    // 평가손익도 실현손익과 같은 기준(기본값)을 쓴다. 두 값은 각각 닫힌 삼중항이라
    // (기록실현+기본평가=totalProfit, Net실현+Net평가=totalProfitNet) 섞으면 '실현+평가=총' 이
    // 깨진다(실측 2026-08-23: 혼합 시 61행 중 18행 불일치).
    // 자산현황·포트폴리오가 이미 기본값을 쓰므로 그쪽에 맞춘다.
    BigDecimal evaluationProfit = sumTradeProfit(enriched, TradeProfit::evaluationProfit);

    // 매매 내역 (이 계좌, 최신순, 기간 적용)
    List<TradeResponse> trades =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accTradesFuture);
    if (trades == null) {
      trades = List.of();
    }
    trades =
        trades.stream()
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    // 배당 내역 (이 계좌, 최신순)
    List<DividendResponse> dividends =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accDividendsFuture);
    if (dividends == null) {
      dividends = List.of();
    }
    dividends =
        dividends.stream()
            .sorted(
                Comparator.comparing(
                    DividendResponse::payDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    BigDecimal totalDividend =
        dividends.stream()
            .map(dividend -> dividend.netAmount() != null ? dividend.netAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 평가액·원가 추이 (차트용, 기간 적용)
    List<TradeProfitTimeSeriesPoint> timeSeries =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accTimeSeriesFuture);
    if (timeSeries == null) {
      timeSeries = List.of();
    }

    // 기간 필터 모델 (날짜 필터 바)
    // 바꾸는 규칙은 StockZoneUtil.resolve 한 곳에만 둔다(잘못된 값이면 서버 기본 존).
    java.time.ZoneId filterZone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
    model.addAttribute(
        "filterStartLocal", startDate != null ? startDate.atZone(filterZone).toLocalDate() : null);
    model.addAttribute(
        "filterEndLocal",
        endDate != null ? endDate.atZone(filterZone).toLocalDate().minusDays(1) : null);
    model.addAttribute("filterStartInstant", startDate);
    model.addAttribute("filterEndInstant", endDate);
    model.addAttribute("filterTimeZone", timeZone);
    model.addAttribute("filterRangeMode", rangeMode);
    // '전체' 는 날짜를 안 보내 기간 배지가 빌 수밖에 없다. 그때 대신 적을, 이 화면이 덮은 구간.
    // 사용자 전체의 최초 일자가 아니라 <b>이 종목/계좌의</b> 시계열에서 뽑아야 맞는 날짜가 나온다.
    var covered =
        net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
            timeSeries,
            net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint::timestamp,
            filterZone);
    String detailFirstDate = detailDataFirstDate(userId, null, resolvedId, filterZone);
    // 배지의 시작은 시계열의 첫 점과 이 화면 데이터의 최초일 중 이른 쪽이다 - 시계열은 평가액이
    // 잡히는 날부터라 첫 거래보다 늦게 시작할 수 있다(실측 2026-09-13: 삼성전자 19 일 · 한투 위탁 13 일).
    model.addAttribute(
        "coveredStartLocal",
        net.luversof.web.gate.stock.util.StockFirstDateUtil.coveredStart(
            covered.startDate(), detailFirstDate));
    // 시계열이 없는 종목(배당만 있는 경우)은 끝을 못 정해 배지가 "~ ?" 로 나갔다 -
    // 화면이 그린 매매·배당의 마지막 날로 메운다(실측 2026-09-13: 하나금융지주).
    java.time.LocalDate contentLast =
        net.luversof.web.gate.stock.util.StockFirstDateUtil.later(
            net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
                    trades,
                    net.luversof.web.gate.stock.dto.response.TradeResponse::tradeDate,
                    filterZone)
                .endDate(),
            net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(
                    dividends,
                    net.luversof.web.gate.stock.dto.response.DividendResponse::payDate,
                    filterZone)
                .endDate());
    model.addAttribute(
        "coveredEndLocal",
        net.luversof.web.gate.stock.util.StockFirstDateUtil.coveredEnd(
            covered.endDate(), contentLast));
    // 이 계좌의 최초 일자(종목 상세와 같은 규칙).
    model.addAttribute("dataFirstDate", detailFirstDate);
    // 빈 기간 안내에 "전체 기간으로 보기" 를 띄울지 화면이 판단하려면 지금 모드를 알아야 한다.
    model.addAttribute("rangeMode", rangeMode == null ? "" : rangeMode);

    model.addAttribute("holdings", holdings);
    model.addAttribute("holdingCount", holdings.size());
    // 이 화면의 "현재가"·평가액도 마지막으로 수집된 종가 기준이다. 어느 날 기준인지 밝히지 않으면
    // 실시간 시세로 오해할 수 있다(실측: 오늘이 2026-08-22 인데 보유 15종목의 currentPriceDate 가
    // 모두 2026-08-20 이었다). 자산현황·포트폴리오와 같은 표기를 쓴다.
    model.addAttribute("priceBasisDate", latestPriceBasisDate(holdings));
    model.addAttribute("evaluationAmount", evaluationAmount);
    model.addAttribute("totalBuyCost", totalBuyCost);
    model.addAttribute("evaluationProfit", evaluationProfit);
    model.addAttribute("realizedProfit", realizedProfit);
    // 기록된 실현손익은 계좌를 합친 원가를 따르므로 이 계좌 페이지의 헤드라인이 이 계좌의 매매와
    // 크게 다를 수 있다(실측 2026-08-23: 연금저축1 은 그 계좌 매매 기준의 1/5, ISA 는 반대로 104 배).
    // 매매 화면의 계좌별 표와 같은 규칙·같은 문구를 쓴다. 종목 상세에는 붙이지 않는다 - 기록값이
    // 종목 단위 기준이라 36 종목 전부 값의 0.009% 안에서 맞는다.
    BigDecimal realizedProfitOwnBasis = sumTradeProfit(profits, TradeProfit::realizedProfitNet);
    model.addAttribute("realizedProfitOwnBasis", realizedProfitOwnBasis);
    // 실현 손익 카드의 수익률 분모 - 종목 상세와 같은 규칙(판 주식의 원가 = 매도 금액 - 기록 실현손익, 같은 기간 행).
    // 기록값이 계좌를 합친 원가를 따르므로 '이 계좌 기준' 줄에는 그 기준의 분모를 따로 둔다 - 이 계좌 매매로 다시 계산한 원가
    // (매도 총수령액 - 이 계좌 기준 실현손익). 실측 2026-09-17 연금저축1: 기록 +415,053 / 7,788,782 = +5.3%,
    // 이 계좌 기준 +2,063,739 / 6,139,752 = +33.6% (사용자 선택: 카드 금액 기준 + 차이 줄에 그 기준 비율).
    // 이 기간에 판 적이 없으면 둘 다 0 이고 카드는 비율 대신 사유를 적는다.
    BigDecimal periodSellAmount = sumTradeProfit(profits, TradeProfit::totalSellAmount);
    model.addAttribute(
        "realizedCostBasis",
        periodSellAmount.signum() > 0
            ? periodSellAmount.subtract(realizedProfit)
            : BigDecimal.ZERO);
    model.addAttribute(
        "realizedOwnCostBasis",
        periodSellAmount.signum() > 0
            ? sumTradeProfit(profits, TradeProfit::totalSellProceeds)
                .subtract(realizedProfitOwnBasis)
            : BigDecimal.ZERO);
    // 연평균 수익률(XIRR) - 이 계좌의 모든 종목 흐름을 합치고 지금 평가액을 오늘 들어온 돈으로 친다(종목 상세 · 자산 현황과 같은 계산).
    // 기간은 이 계좌의 첫 흐름(= 최초 매수)부터 오늘까지. 실측 2026-09-17: 위탁 +30.1% · KB +2.3% · ISA -0.1% ·
    // 연금저축1 +12.6% · 연금저축2 -4.4%.
    List<net.luversof.web.gate.stock.dto.response.StockCashFlowResponse> accountCashFlows =
        net.luversof.web.gate.stock.util.StockHoldingReturnUtil.combine(
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(accCashFlowFuture));
    java.time.LocalDate accountFirstTradeDate =
        net.luversof.web.gate.stock.util.StockHoldingReturnUtil.firstDate(accountCashFlows);
    model.addAttribute("accountFirstTradeDate", accountFirstTradeDate);
    model.addAttribute(
        "accountReturn",
        net.luversof.web.gate.stock.util.StockHoldingReturnUtil.of(
            accountFirstTradeDate,
            java.time.LocalDate.now(filterZone),
            accountCashFlows,
            evaluationAmount,
            evaluationProfit,
            BigDecimal.ZERO));
    model.addAttribute("totalDividend", totalDividend);
    model.addAttribute("trades", trades);
    model.addAttribute("dividends", dividends);
    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute(
        "chartFormatter",
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(java.time.ZoneId.systemDefault()));
    return "stock/htmx/accountDetailContent";
  }

  private static UUID parseUuidOrNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private BigDecimal sumTradeProfit(
      List<TradeProfit> profits, java.util.function.Function<TradeProfit, BigDecimal> extractor) {
    return profits.stream()
        .map(profit -> extractor.apply(profit) != null ? extractor.apply(profit) : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 보유 종목 중 가장 최근 시세 기준일. 없으면 {@code null} 이고 화면은 표기를 생략한다.
   *
   * <p>수집이 종목마다 다른 날에 끝날 수 있어 가장 최근 값을 쓴다(자산현황 조각과 같은 규칙).
   */
  private java.time.LocalDate latestPriceBasisDate(List<TradeProfit> holdings) {
    return net.luversof.web.gate.stock.util.StockPriceBasisUtil.latestPriceBasisDate(holdings);
  }
}
