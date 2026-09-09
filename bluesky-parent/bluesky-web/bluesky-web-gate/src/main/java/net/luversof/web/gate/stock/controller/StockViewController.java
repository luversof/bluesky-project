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

  /** 상세 화면의 서로 독립적인 api-stock 조회를 동시에 던지기 위한 실행기. */
  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.support.StockAsyncSupport stockAsync;

  private MonthlyDividendProfileClient monthlyDividendProfileClient;

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
      @RequestParam(required = false) String symbol) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String simulatorTab = resolveSimulatorTab(tab);
    model.addAttribute("simulatorTab", simulatorTab);

    if ("monthly-dividend".equals(simulatorTab)) {
      UUID userId = UserUtil.getUserId();
      // 결과 코드/저장건수는 POST 후 flash 로만 전달된다 (URL 쿼리로 받으면
      // 새로고침마다 이전 결과 메시지가 재표시되는 버그가 있어 제거 — 관리 페이지와 동일 패턴).
      populateMonthlyDividendModel(
          model, userId, sort, direction, keyword, minAnnualYield, positiveOnly, symbol);
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
      String prefillSymbol) {
    String monthlyDividendSort = monthlyDividendViewSupport.resolveRowSort(sort);
    String monthlyDividendDirection =
        monthlyDividendViewSupport.resolveRowDirection(monthlyDividendSort, direction);
    String monthlyDividendKeyword = keyword != null ? keyword.trim() : "";
    // 원장 수량 조회는 스냅샷/프로필 조회와 서로 의존이 없다. 순차로 붙이면 그대로 왕복이 더해진다
    // (실측 2026-08-23: 이 조회 하나가 p50 31ms).
    var currentHoldingsFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.loadCurrentHoldings(userId));
    List<MonthlyDividendSnapshotResponse> allRows =
        monthlyDividendReferenceSupport.loadMonthlyDividendRows(userId);
    List<MonthlyDividendProfileResponse> monthlyDividendProfiles =
        monthlyDividendViewSupport.sortProfiles(
            monthlyDividendReferenceSupport.loadMonthlyDividendProfiles(),
            MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER,
            "asc");
    Map<String, Integer> monthlyDividendProfileDisplayOrders =
        monthlyDividendViewSupport.buildProfileDisplayOrderMap(monthlyDividendProfiles);
    List<MonthlyDividendSnapshotResponse> filteredRows =
        monthlyDividendViewSupport.sortRows(
            monthlyDividendViewSupport.filterRows(
                allRows, monthlyDividendKeyword, minAnnualYield, positiveOnly),
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
        model, userId, sort, direction, keyword, minAnnualYield, positiveOnly, null);
    return "stock/simulator";
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
    if (dataStatusUserId != null) {
      try {
        model.addAttribute("dataStatus", dataStatusClient.findDataStatus(dataStatusUserId));
      } catch (RuntimeException e) {
        log.warn("data status lookup failed: {}", e.toString());
      }
      // 원장 점검도 같은 이유로 실패해도 화면은 떠야 한다. 다만 "이상 0 건"과 "검사가 못 돌았다"는
      // 구분돼야 하므로, 실패하면 모델에 아무것도 넣지 않고 화면이 그 사실을 따로 적는다.
      try {
        // 예시를 기본 3 건만 받으면 발견의 절반 이상을 화면에서 볼 수 없다(실측 2026-08-23: 45 건 중 25 건).
        // 조치하려면 어느 행인지 알아야 하므로 넉넉히 받아 접이식으로 보여 준다.
        model.addAttribute(
            "ledgerIntegrity",
            ledgerIntegrityClient.check(dataStatusUserId, LEDGER_INTEGRITY_MAX_EXAMPLES));
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
