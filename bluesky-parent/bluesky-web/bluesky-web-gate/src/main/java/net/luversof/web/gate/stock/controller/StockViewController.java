package net.luversof.web.gate.stock.controller;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendSnapshotClient;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutSourceImportService;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

  private static final Logger log = LoggerFactory.getLogger(StockViewController.class);

  private static final String ADMIN_TAB_DATA_MANAGEMENT = "data-management";
  private static final String MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER = "display-order";

  private AccountClient accountClient;

  private MonthlyDividendPayoutClient monthlyDividendPayoutClient;

  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.httpexchange.DataStatusClient dataStatusClient;

  @Autowired
  private net.luversof.web.gate.stock.httpexchange.LedgerIntegrityClient ledgerIntegrityClient;

  /** 관리 화면의 두 조회를 나란히 던지기 위한 공용 실행기(다른 화면들이 쓰는 것과 같다). */
  @Autowired private java.util.concurrent.ExecutorService stockRemoteCallExecutor;

  /** 상세 화면의 서로 독립적인 api-stock 조회를 동시에 던지기 위한 실행기. */
  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.support.StockAsyncSupport stockAsync;

  private MonthlyDividendProfileClient monthlyDividendProfileClient;

  /** 적립 추천에 쓰는 종목 단위 정보(연배당 수익률 · 분배금 추세)는 이 창구에만 있다. */
  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.httpexchange.MonthlyDividendCatalogClient
      monthlyDividendCatalogClient;

  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.service.MonthlyContributionPickSupport
      monthlyContributionPickSupport;

  private MonthlyDividendSnapshotClient monthlyDividendSnapshotClient;

  private StockItemClient stockItemClient;

  private MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser;

  private MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService;

  private StockAdminClient stockAdminClient;

  private MonthlyDividendCalculator monthlyDividendCalculator;

  private MonthlyDividendViewSupport monthlyDividendViewSupport;

  @Autowired
  private net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
      monthlyDividendReferenceSupport;

  private TradeProfitClient tradeProfitClient;

  private TradeClient tradeClient;

  private DividendClient dividendClient;

  @Autowired
  public void setAccountClient(AccountClient accountClient) {
    this.accountClient = accountClient;
  }

  @Autowired
  public void setTradeProfitClient(TradeProfitClient tradeProfitClient) {
    this.tradeProfitClient = tradeProfitClient;
  }

  @Autowired
  public void setTradeClient(TradeClient tradeClient) {
    this.tradeClient = tradeClient;
  }

  @Autowired
  public void setDividendClient(DividendClient dividendClient) {
    this.dividendClient = dividendClient;
  }

  @Autowired
  public void setMonthlyDividendViewSupport(MonthlyDividendViewSupport monthlyDividendViewSupport) {
    this.monthlyDividendViewSupport = monthlyDividendViewSupport;
  }

  public void setMonthlyDividendReferenceSupport(
      net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
          monthlyDividendReferenceSupport) {
    this.monthlyDividendReferenceSupport = monthlyDividendReferenceSupport;
  }

  @Autowired
  public void setStockAdminClient(StockAdminClient stockAdminClient) {
    this.stockAdminClient = stockAdminClient;
  }

  @Autowired
  public void setMonthlyDividendCalculator(MonthlyDividendCalculator monthlyDividendCalculator) {
    this.monthlyDividendCalculator = monthlyDividendCalculator;
  }

  @Autowired
  public void setMonthlyDividendPayoutClient(
      MonthlyDividendPayoutClient monthlyDividendPayoutClient) {
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
  }

  @Autowired
  public void setMonthlyDividendProfileClient(
      MonthlyDividendProfileClient monthlyDividendProfileClient) {
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
  }

  @Autowired
  public void setMonthlyDividendSnapshotClient(
      MonthlyDividendSnapshotClient monthlyDividendSnapshotClient) {
    this.monthlyDividendSnapshotClient = monthlyDividendSnapshotClient;
  }

  @Autowired
  public void setMonthlyDividendPayoutImportParser(
      MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser) {
    this.monthlyDividendPayoutImportParser = monthlyDividendPayoutImportParser;
  }

  @Autowired
  public void setMonthlyDividendPayoutSourceImportService(
      MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService) {
    this.monthlyDividendPayoutSourceImportService = monthlyDividendPayoutSourceImportService;
  }

  @Autowired
  public void setStockItemClient(StockItemClient stockItemClient) {
    this.stockItemClient = stockItemClient;
  }

  @BlueskyPreAuthorize
  @GetMapping
  public String index(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    // dashboard.jte 는 셸만 렌더하고 데이터는 htmx 조각이 각자 로드한다.
    // (계좌/종목 목록을 여기서 조회해도 템플릿이 쓰지 않아 API 2회가 낭비였다.)
    return "stock/dashboard";
  }

  @BlueskyPreAuthorize
  @GetMapping("/analytics")
  public String analyticsPage(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return "stock/analytics";
  }

  @BlueskyPreAuthorize
  @GetMapping("/dashboard")
  public String dashboard(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return "redirect:/stock";
  }

  @BlueskyPreAuthorize
  @GetMapping("/activity")
  public String activityPage(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return "stock/activity";
  }

  /** api-stock 이 "1년 평균" 을 낼 때 쓰는 건수(MonthlyDividendPayoutService 의 limit(12)). */

  /**
   * 원장 점검에서 규칙마다 받아 올 예시 개수. api-stock 의 상한은 100 이다.
   *
   * <p>기본값(3)으로 두면 화면이 발견의 절반 이상을 감춘다 &mdash; 실측 2026-08-23: 발견 45 건 중 25 건이 예시 밖이었다. 조치하려면 어느 행인지
   * 알아야 하므로 넉넉히 받아 온다(현재 가장 많은 규칙이 12 건).
   */
  static final int LEDGER_INTEGRITY_MAX_EXAMPLES = 20;

  @BlueskyPreAuthorize
  @GetMapping("/trade")
  public String tradePage(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    // 계좌·종목 목록은 조각(tradeList.jte)이 스스로 받아 그린다. 예전에는 여기서도 api-stock 을 두 번 불러 모델에 넣었지만
    // trade.jte 는 그 값을 읽지 않았다(실측 2026-09-09: 페이지마다 헛호출 2회, api-stock 이 내려가면 껍데기 대신 전체 오류 화면).
    // 껍데기는 백엔드 없이도 그려지고, 데이터 실패는 조각이 각자 안내한다 - 배당·활동 화면과 같은 규칙.
    return "stock/trade";
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth")
  public String assetGrowthPage(HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return "stock/assetGrowth";
  }

  /** 구 "실현 손익" 페이지 — "매매 내역"으로 통합됨. 북마크 호환용 리다이렉트. */
  @BlueskyPreAuthorize
  @GetMapping("/realized-profit")
  public String realizedProfitPage() {
    return "redirect:/stock/trade";
  }

  @BlueskyPreAuthorize
  @GetMapping("/simulator")
  public String simulatorPage(
      HttpServletRequest request,
      Model model,
      @RequestParam(required = false) String tab,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BigDecimal minAnnualYield,
      @RequestParam(defaultValue = "false") boolean positiveOnly,
      @RequestParam(required = false) String payoutWindow,
      @RequestParam(required = false) String account,
      @RequestParam(required = false) String symbol) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String simulatorTab = resolveSimulatorTab(tab);
    model.addAttribute("simulatorTab", simulatorTab);
    // 탭은 탭만 바꾼다 - 실측 2026-09-12: 월배당 표를 종목코드로 정렬한 뒤 다른 탭에 갔다 돌아오면
    // sort/direction 이 주소에서 사라져 기본 순서로 되돌아갔다. 배당 화면과 같은 규칙을 쓴다.
    String simulatorQuery = request.getQueryString();
    for (String each : new String[] {"sustainability", "monthly-dividend", "compound"}) {
      model.addAttribute(
          simulatorTabHrefAttribute(each),
          net.luversof.web.gate.stock.util.StockTabLinkUtil.tabHref(
              "/stock/simulator", simulatorQuery, each));
    }

    if ("monthly-dividend".equals(simulatorTab)) {
      UUID userId = UserUtil.getUserId();
      // 결과 코드/저장건수는 POST 후 flash 로만 전달된다 (URL 쿼리로 받으면
      // 새로고침마다 이전 결과 메시지가 재표시되는 버그가 있어 제거 — 관리 페이지와 동일 패턴).
      populateMonthlyDividendModel(
          model,
          userId,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          payoutWindow,
          account,
          symbol);
    }

    return "stock/simulator";
  }

  @BlueskyPreAuthorize
  @PostMapping("/simulator/monthly-dividend")
  public String saveMonthlyDividend(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @ModelAttribute MonthlyDividendSnapshotUpsertRequest monthlyDividendForm,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BigDecimal minAnnualYield,
      @RequestParam(defaultValue = "false") boolean positiveOnly) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    UUID userId = UserUtil.getUserId();
    try {
      normalizeMonthlyDividendRequest(monthlyDividendForm, userId);
      applyMonthlyDividendReferenceData(monthlyDividendForm);
      validateMonthlyDividendRequest(monthlyDividendForm);
      monthlyDividendSnapshotClient.upsertSnapshot(monthlyDividendForm);
      return buildMonthlyDividendRedirect(
          redirectAttributes,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          "single-saved",
          1);
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendError(
          model,
          userId,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          ex.getMessage(),
          monthlyDividendForm,
          "");
    } catch (Exception ex) {
      log.warn("월배당 데이터 저장 실패: userId={}", userId, ex);
      return renderMonthlyDividendError(
          model,
          userId,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.snapshot.save.failed")),
          monthlyDividendForm,
          "");
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/simulator/monthly-dividend/bulk")
  public String saveMonthlyDividendBulk(
      HttpServletRequest request,
      Model model,
      @RequestParam String bulkInput,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BigDecimal minAnnualYield,
      @RequestParam(defaultValue = "false") boolean positiveOnly) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    UUID userId = UserUtil.getUserId();
    return renderMonthlyDividendError(
        model,
        userId,
        sort,
        direction,
        keyword,
        minAnnualYield,
        positiveOnly,
        msg("stock.monthly.reference.error.reference.managed.elsewhere"),
        buildDefaultMonthlyDividendForm(),
        bulkInput);
  }

  @BlueskyPreAuthorize
  @PostMapping("/simulator/monthly-dividend/import-sheet")
  public String importMonthlyDividendFromSheet(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BigDecimal minAnnualYield,
      @RequestParam(defaultValue = "false") boolean positiveOnly) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    UUID userId = UserUtil.getUserId();
    try {
      int processed = stockAdminClient.monthlyDividendSnapshotImportFromSheet(userId);
      return buildMonthlyDividendRedirect(
          redirectAttributes,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          "sheet-imported",
          processed);
    } catch (Exception ex) {
      log.warn("배당주 검색 시트에서 월배당 보유/평단가 가져오기 실패: userId={}", userId, ex);
      return renderMonthlyDividendError(
          model,
          userId,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          msg("stock.monthly.reference.error.sheet.import.failed"),
          buildDefaultMonthlyDividendForm(),
          "");
    }
  }

  private String resolveSimulatorTab(String tab) {
    if ("monthly-dividend".equalsIgnoreCase(tab) || "monthly".equalsIgnoreCase(tab)) {
      return "monthly-dividend";
    }

    if ("compound".equalsIgnoreCase(tab)) {
      return "compound";
    }

    return "sustainability";
  }

  private String resolveAdminTab(String tab) {
    return MonthlyDividendReferenceSupport.DIVIDEND_TAB_MONTHLY_REFERENCE.equalsIgnoreCase(tab)
        ? MonthlyDividendReferenceSupport.DIVIDEND_TAB_MONTHLY_REFERENCE
        : ADMIN_TAB_DATA_MANAGEMENT;
  }

  private void populateMonthlyDividendModel(
      Model model,
      UUID userId,
      String sort,
      String direction,
      String keyword,
      BigDecimal minAnnualYield,
      boolean positiveOnly,
      String payoutWindow,
      String account,
      String prefillSymbol) {
    String monthlyDividendSort = monthlyDividendViewSupport.resolveRowSort(sort);
    String monthlyDividendDirection =
        monthlyDividendViewSupport.resolveRowDirection(monthlyDividendSort, direction);
    String monthlyDividendKeyword = keyword != null ? keyword.trim() : "";
    // 원장 수량 조회는 스냅샷/프로필 조회와 서로 의존이 없다. 순차로 붙이면 그대로 왕복이 더해진다
    // (실측 2026-08-23: 이 조회 하나가 p50 31ms).
    var currentHoldingsFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.loadCurrentHoldings(userId));
    // 표가 보여 주는 과세표준 비중은 스냅샷에 저장된 값이라, 지급 이력이 갱신돼도 사용자가 다시 채우기 전까지 옛 값이다
    // (실측 2026-09-11: 8 종목 전부 달랐고 최대 13.42% 對 100%). 같은 화면에서 차이를 알 수 있게 이력 기준 값도 함께 싣는다.
    var referenceRatioFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.referenceTaxableRatioBySymbol());
    // 표의 과세표준 비중은 원장 최근 1 년을 모든 계좌 합쳐 낸 값이라 과세이연 계좌 몫이 비중을 끌어내린다 - 행마다 그 나눔을 적는다
    // (실측 2026-09-17: 과세이연 계좌에서만 받은 두 종목이 0%, 사용자 결정: 계산은 두고 표기를 바로잡는다).
    var taxableRatioBasisFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.loadTaxableRatioBasis(userId));
    // 지급 시기 · 계좌 필터와 적립 추천이 같은 카탈로그를 쓴다 - 한 번만 받아 둘 다 같은 답을 내게 한다(사용자 요청 2026-09-23).
    var catalogFuture = stockAsync.supply(this::loadMonthlyDividendCatalog);
    List<MonthlyDividendSnapshotResponse> allRows =
        monthlyDividendReferenceSupport.loadMonthlyDividendRows(userId);
    List<MonthlyDividendProfileResponse> monthlyDividendProfiles =
        monthlyDividendViewSupport.sortProfiles(
            monthlyDividendReferenceSupport.loadMonthlyDividendProfiles(),
            MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER,
            "asc");
    Map<String, Integer> monthlyDividendProfileDisplayOrders =
        monthlyDividendViewSupport.buildProfileDisplayOrderMap(monthlyDividendProfiles);
    // 지급 시기(월중 · 월말) · 계좌(위탁 · ISA/연금) 자리로 좁힌다(사용자 요청 2026-09-23, 결정: 적립 추천과 같은 규칙).
    String monthlyDividendPayoutWindowFilter =
        net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.resolveSlotWindow(
            payoutWindow);
    String monthlyDividendAccountFilter =
        net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.resolveSlotAccount(
            account);
    List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse>
        monthlyDividendCatalog =
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(catalogFuture);
    boolean slotFilterRequested =
        !monthlyDividendPayoutWindowFilter.isEmpty() || !monthlyDividendAccountFilter.isEmpty();
    // 카탈로그를 못 받으면 자리를 못 정한다 - 조용히 전체를 보이면 걸린 줄로 읽히므로 화면에 알린다.
    boolean monthlyDividendSlotFilterUnavailable =
        slotFilterRequested && monthlyDividendCatalog == null;
    java.util.Set<String> slotSymbols =
        slotFilterRequested && monthlyDividendCatalog != null
            ? monthlyContributionPickSupport.symbolsInSlot(
                monthlyDividendPayoutWindowFilter,
                monthlyDividendAccountFilter,
                monthlyDividendCatalog)
            : null;
    List<MonthlyDividendSnapshotResponse> filteredRows =
        monthlyDividendViewSupport.sortRows(
            keepSlotRows(
                monthlyDividendViewSupport.filterRows(
                    allRows, monthlyDividendKeyword, minAnnualYield, positiveOnly),
                slotSymbols),
            monthlyDividendSort,
            monthlyDividendDirection,
            monthlyDividendProfileDisplayOrders);

    Map<String, String> monthlyDividendPayoutWindowBySymbol = new LinkedHashMap<>();
    for (MonthlyDividendProfileResponse profile : monthlyDividendProfiles) {
      String profileSymbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      if (profileSymbol != null
          && profile.payoutWindow() != null
          && !monthlyDividendPayoutWindowBySymbol.containsKey(profileSymbol)) {
        monthlyDividendPayoutWindowBySymbol.put(profileSymbol, profile.payoutWindow());
      }
    }

    // 스냅샷의 보유 수량은 사람이 갱신한 시점의 값이라 원장과 어긋날 수 있다. 이 표는 그 수량을
    // 그대로 '보유수량' 으로 찍으므로, 어긋나면 사용자는 자기 보유량을 잘못 읽는다
    // (실측 2026-08-23: 8 종목 중 7 종목이 달랐고 전부 현재가 더 많았다).
    MonthlyDividendReferenceSupport.CurrentHoldings currentHoldings =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(currentHoldingsFuture);
    Map<UUID, Integer> monthlyDividendCurrentQuantities = currentHoldings.quantities();
    model.addAttribute("monthlyDividendCurrentQuantities", monthlyDividendCurrentQuantities);
    model.addAttribute(
        "monthlyDividendCurrentAverageBuyPrices", currentHoldings.averageBuyPrices());
    // 이 표는 배당 기준값(스냅샷 시점)과 시세(최근 종가) 두 시점을 한 줄에 섞는다. 앞의 날짜만 적혀 있었다.
    model.addAttribute("monthlyDividendPriceBasisDate", currentHoldings.priceBasisDate());
    // 원장 조회가 실패하면 어긋난 줄에 붙던 "현재 N" 표시가 통째로 사라진다 - 사라진 표시는
    // "원장과 같다" 로 읽히므로(실측 2026-09-12: 8 줄 중 3 줄이 이 표시를 달고 있었다),
    // 실패했다는 사실을 화면에 남긴다.
    model.addAttribute("monthlyDividendCurrentHoldingsUnavailable", currentHoldings.unavailable());
    java.util.Map<String, BigDecimal> referenceTaxableRatioBySymbol =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(referenceRatioFuture);
    java.util.Map<java.util.UUID, BigDecimal> monthlyDividendReferenceTaxableRatios =
        new java.util.LinkedHashMap<>();
    for (MonthlyDividendSnapshotResponse row : allRows) {
      if (row.stockItemId() == null || row.stockItemSymbol() == null) {
        continue;
      }
      BigDecimal referenceRatio =
          referenceTaxableRatioBySymbol.get(
              row.stockItemSymbol().trim().toUpperCase(java.util.Locale.ROOT));
      if (referenceRatio != null) {
        monthlyDividendReferenceTaxableRatios.put(row.stockItemId(), referenceRatio);
      }
    }
    model.addAttribute(
        "monthlyDividendReferenceTaxableRatios", monthlyDividendReferenceTaxableRatios);
    model.addAttribute(
        "monthlyDividendTaxableRatioBasis",
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(taxableRatioBasisFuture));
    // 행마다 "지급 이력 기준 N%" 를 알려도 합계 카드는 저장값으로만 계산된다 - 실측 2026-09-11:
    // 총 예상 월 과세표준액이 220,539 인데 지급 이력 기준이면 994,375(4.5 배)였다. 세금은 이 값에 붙는다.
    BigDecimal monthlyDividendReferenceTaxableTotal = BigDecimal.ZERO;
    int monthlyDividendReferenceTaxableStaleCount = 0;
    for (MonthlyDividendSnapshotResponse row : filteredRows) {
      BigDecimal expectedDividend =
          row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO;
      BigDecimal referenceRatio =
          row.stockItemId() != null
              ? monthlyDividendReferenceTaxableRatios.get(row.stockItemId())
              : null;
      if (referenceRatio == null) {
        monthlyDividendReferenceTaxableTotal =
            monthlyDividendReferenceTaxableTotal.add(
                row.expectedTaxableBaseAmount() != null
                    ? row.expectedTaxableBaseAmount()
                    : BigDecimal.ZERO);
        continue;
      }
      monthlyDividendReferenceTaxableTotal =
          monthlyDividendReferenceTaxableTotal.add(
              expectedDividend
                  .multiply(referenceRatio)
                  .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP));
      BigDecimal savedRatio =
          row.averageTaxableBaseRatio1y() != null
              ? row.averageTaxableBaseRatio1y()
              : BigDecimal.ZERO;
      if (referenceRatio.subtract(savedRatio).abs().compareTo(BigDecimal.ONE) >= 0) {
        monthlyDividendReferenceTaxableStaleCount++;
      }
    }
    model.addAttribute(
        "monthlyDividendReferenceTaxableTotal", monthlyDividendReferenceTaxableTotal);
    model.addAttribute(
        "monthlyDividendReferenceTaxableStaleCount", monthlyDividendReferenceTaxableStaleCount);
    // 이번에 무엇을 적립할까(사용자 요청 2026-09-22). 자리는 지급 시기 x 계좌(과세표준 10% 경계)로 갈리고,
    // 점수는 연배당 수익률 + min(분배금 추세, 0) 이다. 연배당 수익률과 추세는 종목 단위 정보라 카탈로그에서만 온다.
    // 원격 호출이 실패해도 화면은 살린다 - 추천은 덧붙인 것이고, 실패는 로그로 남긴다.
    model.addAttribute(
        "monthlyContributionPicks", loadContributionPicks(filteredRows, monthlyDividendCatalog));
    model.addAttribute("monthlyDividendRows", filteredRows);
    model.addAttribute(
        "monthlyDividendSummary",
        monthlyDividendCalculator.buildSimulatorSummary(
            filteredRows, monthlyDividendPayoutWindowBySymbol));
    // 합계 카드도 스냅샷 수량으로 계산된다. 행에는 "현재 N 주" 경고가 뜨는데 헤드라인만 조용하면
    // 사용자는 합계를 현재 기준으로 읽는다(실측 2026-08-23: 7/8 종목이 어긋나 1.66% 낮았다).
    // 요약 화면의 다가오는 배당 카드와 같은 규칙·같은 문구를 쓴다.
    var monthlyDividendQuantityBasis =
        net.luversof.web.gate.stock.service.MonthlyDividendCalculator.currentQuantitySummary(
            filteredRows, monthlyDividendCurrentQuantities);
    model.addAttribute(
        "monthlyDividendStaleQuantityCount", monthlyDividendQuantityBasis.staleCount());
    model.addAttribute(
        "monthlyDividendCurrentQuantityTotal",
        monthlyDividendQuantityBasis.totalAtCurrentQuantity());
    model.addAttribute("monthlyDividendPayoutWindowBySymbol", monthlyDividendPayoutWindowBySymbol);
    model.addAttribute("monthlyDividendProfileDisplayOrders", monthlyDividendProfileDisplayOrders);
    model.addAttribute(
        "monthlyDividendProfileOrderedSymbols",
        monthlyDividendProfiles.stream()
            .map(MonthlyDividendProfileResponse::stockItemSymbol)
            .map(monthlyDividendReferenceSupport::normalizeMonthlyDividendSymbol)
            .filter(StringUtils::hasText)
            .distinct()
            .toList());
    model.addAttribute(
        "monthlyDividendReorderEnabled",
        MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER.equals(monthlyDividendSort)
            && "asc".equals(monthlyDividendDirection));
    model.addAttribute(
        "stockItems", monthlyDividendReferenceSupport.loadMonthlyDividendStockItems());
    model.addAttribute("monthlyDividendSort", monthlyDividendSort);
    model.addAttribute("monthlyDividendDirection", monthlyDividendDirection);
    model.addAttribute("monthlyDividendKeyword", monthlyDividendKeyword);
    model.addAttribute("monthlyDividendMinAnnualYield", minAnnualYield);
    model.addAttribute("monthlyDividendPositiveOnly", positiveOnly);
    model.addAttribute("monthlyDividendPayoutWindowFilter", monthlyDividendPayoutWindowFilter);
    model.addAttribute("monthlyDividendAccountFilter", monthlyDividendAccountFilter);
    model.addAttribute(
        "monthlyDividendSlotFilterUnavailable", monthlyDividendSlotFilterUnavailable);
    model.addAttribute("monthlyDividendHasSavedRows", !allRows.isEmpty());

    if (!model.containsAttribute("monthlyDividendForm")) {
      model.addAttribute(
          "monthlyDividendForm", buildDefaultMonthlyDividendForm(prefillSymbol, allRows));
    }
    if (!model.containsAttribute("monthlyDividendBulkInput")) {
      model.addAttribute("monthlyDividendBulkInput", "");
    }
    if (!model.containsAttribute("monthlyDividendErrorMessage")) {
      model.addAttribute("monthlyDividendErrorMessage", "");
    }
    if (!model.containsAttribute("monthlyDividendResult")) {
      model.addAttribute("monthlyDividendResult", "");
    }
    if (!model.containsAttribute("monthlyDividendSavedCount")) {
      model.addAttribute("monthlyDividendSavedCount", null);
    }
  }

  private String buildMonthlyDividendRedirect(
      RedirectAttributes redirectAttributes,
      String sort,
      String direction,
      String keyword,
      BigDecimal minAnnualYield,
      boolean positiveOnly,
      String result,
      Integer savedCount) {
    // 결과 코드는 URL 이 아니라 flash 로 전달한다 (URL 잔류 시 새로고침마다 재표시되는 버그 방지)
    if (redirectAttributes != null && StringUtils.hasText(result)) {
      redirectAttributes.addFlashAttribute("monthlyDividendResult", result);
      if (savedCount != null) {
        redirectAttributes.addFlashAttribute("monthlyDividendSavedCount", savedCount);
      }
    }
    StringBuilder redirectUrl = new StringBuilder("redirect:/stock/simulator?tab=monthly-dividend");
    String resolvedSort = monthlyDividendViewSupport.resolveRowSort(sort);
    StockViewSupport.appendQueryParam(redirectUrl, "sort", resolvedSort);
    StockViewSupport.appendQueryParam(
        redirectUrl,
        "direction",
        monthlyDividendViewSupport.resolveRowDirection(resolvedSort, direction));
    return redirectUrl.toString();
  }

  private String renderMonthlyDividendError(
      Model model,
      UUID userId,
      String sort,
      String direction,
      String keyword,
      BigDecimal minAnnualYield,
      boolean positiveOnly,
      String errorMessage,
      MonthlyDividendSnapshotUpsertRequest monthlyDividendForm,
      String bulkInput) {
    model.addAttribute("simulatorTab", "monthly-dividend");
    model.addAttribute("monthlyDividendForm", monthlyDividendForm);
    model.addAttribute("monthlyDividendBulkInput", bulkInput != null ? bulkInput : "");
    model.addAttribute("monthlyDividendErrorMessage", errorMessage);
    model.addAttribute("monthlyDividendResult", "");
    model.addAttribute("monthlyDividendSavedCount", null);
    populateMonthlyDividendModel(
        model, userId, sort, direction, keyword, minAnnualYield, positiveOnly, null, null, null);
    return "stock/simulator";
  }

  /** 탭 값에서 모델 속성 이름을 만든다(monthly-dividend -> simulatorTabHrefMonthlyDividend). */
  private static String simulatorTabHrefAttribute(String tabValue) {
    StringBuilder sb = new StringBuilder("simulatorTabHref");
    boolean upper = true;
    for (char c : tabValue.toCharArray()) {
      if (c == '-') {
        upper = true;
        continue;
      }
      sb.append(upper ? Character.toUpperCase(c) : c);
      upper = false;
    }
    return sb.toString();
  }

  private MonthlyDividendSnapshotUpsertRequest buildDefaultMonthlyDividendForm() {
    MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
    request.setAsOfDate(LocalDate.now());
    return request;
  }

  private MonthlyDividendSnapshotUpsertRequest buildDefaultMonthlyDividendForm(
      String symbol, List<MonthlyDividendSnapshotResponse> allRows) {
    MonthlyDividendSnapshotUpsertRequest request = buildDefaultMonthlyDividendForm();
    if (!StringUtils.hasText(symbol)) {
      return request;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    request.setSymbol(normalizedSymbol);

    MonthlyDividendSnapshotResponse savedRow =
        allRows.stream()
            .filter(
                row ->
                    normalizedSymbol.equalsIgnoreCase(
                        StockViewSupport.safeString(row.stockItemSymbol())))
            .findFirst()
            .orElse(null);
    if (savedRow != null) {
      request.setAsOfDate(
          savedRow.asOfDate() != null ? savedRow.asOfDate() : request.getAsOfDate());
      request.setHeldQuantity(savedRow.heldQuantity());
      request.setAverageBuyPrice(savedRow.averageBuyPrice());
    }

    List<MonthlyDividendPayoutResponse> payouts =
        monthlyDividendReferenceSupport.loadMonthlyDividendPayouts(normalizedSymbol);
    MonthlyDividendReferenceSummaryView summary =
        monthlyDividendCalculator.buildReferenceSummary(normalizedSymbol, payouts);
    if (summary.payoutCount() > 0) {
      LocalDate referenceDate =
          summary.latestPayDate() != null ? summary.latestPayDate() : summary.latestRecordDate();
      if (referenceDate != null) {
        request.setAsOfDate(referenceDate);
      }
      request.setLatestMonthlyDividendPerShare(summary.latestDividendAmountPerShare());
      request.setAverageMonthlyDividendPerShare1y(summary.averageDividendAmountPerShare1y());
      request.setAverageTaxableBaseRatio1y(summary.averageTaxableBaseRatio1y());
    }

    return request;
  }

  private void applyMonthlyDividendReferenceData(MonthlyDividendSnapshotUpsertRequest request) {
    monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(request.getSymbol());

    List<MonthlyDividendPayoutResponse> payouts =
        monthlyDividendReferenceSupport.loadMonthlyDividendPayouts(request.getSymbol());
    MonthlyDividendReferenceSummaryView summary =
        monthlyDividendCalculator.buildReferenceSummary(request.getSymbol(), payouts);
    if (summary.payoutCount() <= 0) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.reference.missing"));
    }

    LocalDate referenceDate =
        summary.latestPayDate() != null ? summary.latestPayDate() : summary.latestRecordDate();
    if (referenceDate == null) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.reference.pay.date.missing"));
    }

    request.setAsOfDate(referenceDate);
    request.setLatestMonthlyDividendPerShare(summary.latestDividendAmountPerShare());
    request.setAverageMonthlyDividendPerShare1y(summary.averageDividendAmountPerShare1y());
    request.setAverageTaxableBaseRatio1y(summary.averageTaxableBaseRatio1y());
  }

  private void normalizeMonthlyDividendRequest(
      MonthlyDividendSnapshotUpsertRequest request, UUID userId) {
    request.setUserId(userId);
    request.setSymbol(
        StringUtils.hasText(request.getSymbol())
            ? request.getSymbol().trim().toUpperCase(Locale.ROOT)
            : null);
  }

  private void validateMonthlyDividendRequest(MonthlyDividendSnapshotUpsertRequest request) {
    if (!StringUtils.hasText(request.getSymbol())) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.required"));
    }
    if (request.getAsOfDate() == null) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.as.of.date.required"));
    }
    if (request.getHeldQuantity() == null || request.getHeldQuantity() <= 0) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.held.quantity.min"));
    }
    StockViewSupport.requireNonNegative(
        request.getLatestMonthlyDividendPerShare(),
        msg("stock.monthly.reference.error.latest.dividend.negative"));
    StockViewSupport.requireNonNegative(
        request.getAverageMonthlyDividendPerShare1y(),
        msg("stock.monthly.reference.error.average.dividend.negative"));
    StockViewSupport.requireNonNegative(
        request.getAverageBuyPrice(),
        msg("stock.monthly.reference.error.average.buy.price.negative"));

    BigDecimal taxableBaseRatio = safe(request.getAverageTaxableBaseRatio1y());
    if (taxableBaseRatio.compareTo(BigDecimal.ZERO) < 0
        || taxableBaseRatio.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.taxable.ratio.range"));
    }
  }

  List<MonthlyDividendSnapshotUpsertRequest> parseBulkInput(String bulkInput, UUID userId) {
    if (!StringUtils.hasText(bulkInput)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.empty"));
    }

    List<MonthlyDividendSnapshotUpsertRequest> requests = new ArrayList<>();
    String[] lines = bulkInput.split("\\R");
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index] != null ? lines[index].trim() : "";
      if (!StringUtils.hasText(line)) {
        continue;
      }

      String[] columns = splitBulkColumns(line);
      if (columns.length == 0) {
        continue;
      }

      if (requests.isEmpty() && isMonthlyDividendHeader(columns[0])) {
        continue;
      }

      if (columns.length < 7) {
        throw new IllegalArgumentException(
            msg("stock.monthly.reference.error.bulk.columns.short", index + 1));
      }

      // 콤마로 나눈 줄에 열이 더 있으면 숫자의 천단위 콤마가 열을 갈라놓은 것이다.
      // 이대로 앞 7개만 쓰면 값이 한 칸씩 밀려도 전부 숫자로 읽혀 오류 없이 잘못 저장된다
      // (실측: "…,50,1,000,71,887" 이 9열이 되어 보유 수량 1,000 -> 1, 평단가 71,887 -> 000).
      // 탭으로 나눈 줄은 콤마가 값 안에 남아 있어 안전하므로 이 검사가 필요 없다.
      if (!line.contains("	") && columns.length > 7) {
        throw new IllegalArgumentException(
            msg("stock.monthly.reference.error.bulk.columns.long", index + 1));
      }

      MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
      request.setUserId(userId);
      request.setSymbol(columns[0]);
      request.setAsOfDate(parseLocalDate(columns[1], index + 1));
      request.setLatestMonthlyDividendPerShare(
          parseBigDecimal(
              columns[2], index + 1, msg("stock.monthly.reference.field.latest.dividend")));
      request.setAverageMonthlyDividendPerShare1y(
          parseBigDecimal(
              columns[3], index + 1, msg("stock.monthly.reference.field.average.dividend")));
      request.setAverageTaxableBaseRatio1y(
          parseBigDecimal(
              columns[4], index + 1, msg("stock.monthly.reference.field.taxable.ratio")));
      request.setHeldQuantity(
          parseInteger(columns[5], index + 1, msg("stock.monthly.reference.field.held.quantity")));
      request.setAverageBuyPrice(
          parseBigDecimal(
              columns[6], index + 1, msg("stock.monthly.reference.field.average.buy.price")));

      normalizeMonthlyDividendRequest(request, userId);
      validateMonthlyDividendRequest(request);
      requests.add(request);
    }

    if (requests.isEmpty()) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.nothing"));
    }

    return requests;
  }

  String[] splitBulkColumns(String line) {
    String[] rawColumns = line.contains("\t") ? line.split("\t") : line.split(",");
    List<String> columns = new ArrayList<>();
    for (String rawColumn : rawColumns) {
      columns.add(rawColumn != null ? rawColumn.trim() : "");
    }
    return columns.toArray(String[]::new);
  }

  /**
   * 보유 중인 월배당 종목으로 자리마다 하나씩 고른다(사용자 요청 2026-09-22).
   *
   * <p>보유 여부는 시뮬레이터가 이미 걸러 준 행으로 정한다 &mdash; 그 탭은 "내가 받을 배당" 이라 보유 종목만 다룬다. 연배당 수익률과 분배금 추세는 종목 단위
   * 정보라 카탈로그에서 가져온다.
   */
  private java.util.List<
          net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.ContributionPick>
      loadContributionPicks(
          java.util.List<MonthlyDividendSnapshotResponse> heldRows,
          java.util.List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse>
              catalog) {
    // 카탈로그를 못 받은 까닭은 loadMonthlyDividendCatalog 가 이미 남겼다.
    if (heldRows == null || heldRows.isEmpty() || catalog == null) {
      return java.util.List.of();
    }

    try {
      // 후보 만드는 규칙은 월배당 ETF 목록의 "이번 적립" 배지와 같이 쓴다(pickHeld).
      return monthlyContributionPickSupport.pickHeld(
          heldRows.stream().map(MonthlyDividendSnapshotResponse::stockItemSymbol).toList(),
          catalog);
    } catch (Exception ex) {
      // 조용히 삼키면 추천이 사라진 까닭을 못 찾는다.
      log.warn("적립 추천을 못 냈다(화면은 그대로 둔다)", ex);
      return java.util.List.of();
    }
  }

  /** 월배당 카탈로그. 실패하면 null - 적립 추천과 지급 시기 · 계좌 필터가 함께 쉬고, 까닭은 여기서 남긴다. */
  private java.util.List<net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse>
      loadMonthlyDividendCatalog() {
    try {
      return monthlyDividendCatalogClient.findCatalog(
          new org.springframework.util.LinkedMultiValueMap<>());
    } catch (Exception ex) {
      log.warn("월배당 카탈로그를 못 받았다 - 적립 추천 · 지급 시기/계좌 필터를 건너뛴다", ex);
      return null;
    }
  }

  /** 지급 시기 · 계좌 자리에 드는 행만 남긴다. 자리 조건이 없으면(null) 그대로. */
  static java.util.List<MonthlyDividendSnapshotResponse> keepSlotRows(
      java.util.List<MonthlyDividendSnapshotResponse> rows, java.util.Set<String> slotSymbols) {
    if (slotSymbols == null) {
      return rows;
    }
    return rows.stream()
        .filter(
            row ->
                row.stockItemSymbol() != null && slotSymbols.contains(row.stockItemSymbol().trim()))
        .toList();
  }

  private boolean isMonthlyDividendHeader(String firstColumn) {
    String normalized = StockViewSupport.safeString(firstColumn).trim().toLowerCase(Locale.ROOT);
    return "symbol".equals(normalized) || "ticker".equals(normalized) || "종목코드".equals(normalized);
  }

  private LocalDate parseLocalDate(String value, int lineNumber) {
    try {
      return LocalDate.parse(value.trim().replace('/', '-').replace('.', '-'));
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.date.invalid", lineNumber));
    }
  }

  private BigDecimal parseBigDecimal(String value, int lineNumber, String label) {
    try {
      return new BigDecimal(value.trim().replace(",", "").replace("%", ""));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.value.invalid", lineNumber, label));
    }
  }

  private Integer parseInteger(String value, int lineNumber, String label) {
    try {
      return Integer.valueOf(value.trim().replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.value.invalid", lineNumber, label));
    }
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  @BlueskyPreAuthorize
  @GetMapping("/admin")
  public String adminPage(
      HttpServletRequest request,
      Model model,
      @RequestParam(required = false) String tab,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) String profileSort,
      @RequestParam(required = false) String profileDirection,
      @RequestParam(required = false) LocalDate payoutRecordDate,
      @RequestParam(required = false) LocalDate payoutPayDate) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String adminTab = resolveAdminTab(tab);
    model.addAttribute("adminTab", adminTab);

    // 데이터 최신 시점은 서버에서 구한다. 예전에는 브라우저 로컬의 '마지막 갱신 클릭 시각'만 보여줘
    // 다른 브라우저에서 보거나 갱신이 실패했을 때 실제로 어디까지 채워졌는지 알 수 없었다.
    // 조회에 실패해도 관리 화면 자체는 떠야 하므로 값 없이 계속 진행한다.
    UUID dataStatusUserId = UserUtil.getUserId();
    // 이 두 값은 데이터 관리 탭의 adminActions 조각만 쓴다 - 월배당 기준 데이터 탭에서는 받아서 버렸다.
    // 실측 2026-09-12: 그 탭 문서가 89ms(64~96) 였고 데이터 관리 탭은 67ms 다. 안 쓰는 탭에서는 묻지 않는다.
    boolean adminDataStatusNeeded =
        !MonthlyDividendReferenceSupport.DIVIDEND_TAB_MONTHLY_REFERENCE.equals(adminTab);
    if (dataStatusUserId != null && adminDataStatusNeeded) {
      // 두 조회는 서로 의존이 없는데 순차로 돌고 있었다 - 실측 2026-09-10: 데이터 상태 113ms +
      // 원장 점검 45ms 가 그대로 합산돼 이 화면만 TTFB 165~244ms 였다(대시보드 15ms·배당 25ms).
      // 한꺼번에 던지고 결과만 모은다.
      java.util.concurrent.CompletableFuture<?> dataStatusFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> dataStatusClient.findDataStatus(dataStatusUserId), stockRemoteCallExecutor);
      java.util.concurrent.CompletableFuture<?> ledgerFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> ledgerIntegrityClient.check(dataStatusUserId, LEDGER_INTEGRITY_MAX_EXAMPLES),
              stockRemoteCallExecutor);
      try {
        model.addAttribute("dataStatus", dataStatusFuture.join());
      } catch (RuntimeException e) {
        log.warn("data status lookup failed: {}", e.toString());
      }
      // 원장 점검도 같은 이유로 실패해도 화면은 떠야 한다. 다만 "이상 0 건"과 "검사가 못 돌았다"는
      // 구분돼야 하므로, 실패하면 모델에 아무것도 넣지 않고 화면이 그 사실을 따로 적는다.
      try {
        // 예시를 기본 3 건만 받으면 발견의 절반 이상을 화면에서 볼 수 없다(실측 2026-08-23: 45 건 중 25 건).
        // 조치하려면 어느 행인지 알아야 하므로 넉넉히 받아 접이식으로 보여 준다.
        model.addAttribute("ledgerIntegrity", ledgerFuture.join());
      } catch (RuntimeException e) {
        log.warn("ledger integrity check failed: userId={}", dataStatusUserId, e);
      }
    }

    if (MonthlyDividendReferenceSupport.DIVIDEND_TAB_MONTHLY_REFERENCE.equals(adminTab)) {
      // 결과 코드(monthlyDividendReferenceResult)는 POST 후 flash 로만 전달된다.
      // URL 쿼리로 받으면 새로고침마다 이전 결과 메시지가 재표시되는 버그가 있어 제거했다.
      monthlyDividendReferenceSupport.populateMonthlyDividendReferenceModel(
          model, symbol, profileSort, profileDirection, payoutRecordDate, payoutPayDate);
    }

    return "stock/admin";
  }
}
