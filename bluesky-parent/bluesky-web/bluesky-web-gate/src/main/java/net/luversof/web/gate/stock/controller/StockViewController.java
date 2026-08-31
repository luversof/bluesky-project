package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.luversof.boot.context.support.MessageUtil;
import io.github.luversof.boot.exception.BlueskyErrorMessage;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.exception.ErrorMessage;
import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutImportRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
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
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutSourceImportService;
import net.luversof.web.gate.stock.util.StockFormatUtil;
import net.luversof.web.gate.stock.util.StockOwnershipUtil;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

  private static final Logger log = LoggerFactory.getLogger(StockViewController.class);

  private static final String MONTHLY_DIVIDEND_TAG = "월배당";
  private static final String ADMIN_TAB_DATA_MANAGEMENT = "data-management";
  private static final String DIVIDEND_TAB_MONTHLY_REFERENCE = "monthly-reference";
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

  private String getLoginRedirectUrl(HttpServletRequest request) {
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int serverPort = request.getServerPort();

    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(scheme).append("://").append(serverName);
    if (serverPort != 80 && serverPort != 443) {
      urlBuilder.append(":").append(serverPort);
    }
    urlBuilder.append(request.getRequestURI());

    if (request.getQueryString() != null) {
      urlBuilder.append("?").append(request.getQueryString());
    }

    String encodedUrl = URLEncoder.encode(urlBuilder.toString(), StandardCharsets.UTF_8);
    return "redirect:/login?redirectUrl=" + encodedUrl;
  }

  private boolean isNotAuthenticated() {
    return UserUtil.getUserId() == null;
  }

  @BlueskyPreAuthorize
  @GetMapping
  public String index(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    // dashboard.jte 는 셸만 렌더하고 데이터는 htmx 조각이 각자 로드한다.
    // (계좌/종목 목록을 여기서 조회해도 템플릿이 쓰지 않아 API 2회가 낭비였다.)
    return "stock/dashboard";
  }

  @BlueskyPreAuthorize
  @GetMapping("/analytics")
  public String analyticsPage(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    return "stock/analytics";
  }

  @BlueskyPreAuthorize
  @GetMapping("/dashboard")
  public String dashboard(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    return "redirect:/stock";
  }

  @BlueskyPreAuthorize
  @GetMapping("/activity")
  public String activityPage(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    return "stock/activity";
  }

  @BlueskyPreAuthorize
  @GetMapping("/dividend")
  public String dividendPage(
      HttpServletRequest request,
      Model model,
      @RequestParam(required = false) String tab,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) String profileSort,
      @RequestParam(required = false) String profileDirection,
      @RequestParam(required = false) LocalDate payoutRecordDate,
      @RequestParam(required = false) LocalDate payoutPayDate) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String dividendTab = resolveDividendTab(tab);

    if (DIVIDEND_TAB_MONTHLY_REFERENCE.equals(dividendTab)) {
      return buildMonthlyDividendReferencePageRedirect(
          symbol, profileSort, profileDirection, payoutRecordDate, payoutPayDate);
    }

    if ("calendar".equals(dividendTab)) {
      populateDividendCalendarModel(UserUtil.getUserId(), model);
    }

    model.addAttribute("dividendTab", dividendTab);

    return "stock/dividend";
  }

  /** 배당 캘린더 모델: 보유 월배당 종목의 예상 월 배당을 지급 시기(월중/월말)별로 그룹핑한다. */
  private void populateDividendCalendarModel(UUID userId, Model model) {
    List<MonthlyDividendSnapshotResponse> rows = loadMonthlyDividendRows(userId);
    if (rows == null) {
      rows = List.of();
    }

    Map<String, String> payoutWindowBySymbol = new LinkedHashMap<>();
    for (MonthlyDividendProfileResponse profile : loadMonthlyDividendProfiles()) {
      String symbol = normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      if (symbol != null
          && profile.payoutWindow() != null
          && !payoutWindowBySymbol.containsKey(symbol)) {
        payoutWindowBySymbol.put(symbol, profile.payoutWindow());
      }
    }

    List<MonthlyDividendSnapshotResponse> midRows = new ArrayList<>();
    List<MonthlyDividendSnapshotResponse> endRows = new ArrayList<>();
    List<MonthlyDividendSnapshotResponse> otherRows = new ArrayList<>();
    for (MonthlyDividendSnapshotResponse row : rows) {
      String symbol = normalizeMonthlyDividendSymbol(row.stockItemSymbol());
      String window = symbol != null ? payoutWindowBySymbol.get(symbol) : null;
      if ("MID_MONTH".equals(window)) {
        midRows.add(row);
      } else if ("MONTH_END".equals(window)) {
        endRows.add(row);
      } else {
        otherRows.add(row);
      }
    }
    Comparator<MonthlyDividendSnapshotResponse> byExpectedDesc =
        Comparator.comparing(
                (MonthlyDividendSnapshotResponse row) ->
                    row.expectedMonthlyDividend() != null
                        ? row.expectedMonthlyDividend()
                        : BigDecimal.ZERO)
            .reversed();
    midRows.sort(byExpectedDesc);
    endRows.sort(byExpectedDesc);
    otherRows.sort(byExpectedDesc);

    model.addAttribute("midRows", midRows);
    model.addAttribute("endRows", endRows);
    model.addAttribute("otherRows", otherRows);
    model.addAttribute("midTotal", sumExpectedMonthlyDividend(midRows));
    model.addAttribute("endTotal", sumExpectedMonthlyDividend(endRows));
    model.addAttribute("otherTotal", sumExpectedMonthlyDividend(otherRows));
    model.addAttribute("monthlyTotal", sumExpectedMonthlyDividend(rows));
    model.addAttribute(
        "annualTotal", sumExpectedMonthlyDividend(rows).multiply(BigDecimal.valueOf(12)));
    // 최근 배당금(직전 1회) 기준 합계도 병행 제공: latestMonthlyDividendPerShare × 보유수량
    // 달력의 합계도 스냅샷 수량으로 계산된다. 요약 카드와 월배당 시뮬레이터에는 이 안내가 있는데
    // 달력에만 없어서, 같은 숫자가 한 화면에서는 "옛 수량 기준" 이라고 밝혀지고 다른 화면에서는
    // 아무 말 없이 나갔다(실측 2026-08-23: 8 종목 중 7 종목이 어긋나 1.66% 낮다).
    var calendarQuantityBasis =
        net.luversof.web.gate.stock.service.MonthlyDividendCalculator.currentQuantitySummary(
            rows, loadCurrentHoldings(userId).quantities());
    model.addAttribute("calendarStaleQuantityCount", calendarQuantityBasis.staleCount());
    model.addAttribute(
        "calendarCurrentQuantityTotal", calendarQuantityBasis.totalAtCurrentQuantity());

    model.addAttribute("midTotalLatest", sumLatestMonthlyDividend(midRows));
    model.addAttribute("endTotalLatest", sumLatestMonthlyDividend(endRows));
    model.addAttribute("otherTotalLatest", sumLatestMonthlyDividend(otherRows));
    model.addAttribute("monthlyTotalLatest", sumLatestMonthlyDividend(rows));
    model.addAttribute(
        "annualTotalLatest", sumLatestMonthlyDividend(rows).multiply(BigDecimal.valueOf(12)));

    // "평균" 기준은 최근 <b>12건</b>의 주당 배당을 평균한다(기간 기준이 아니라 건수 기준이다).
    // 상장이 얼마 안 된 종목은 이력이 12건에 못 미쳐 그만큼 짧은 기간의 평균이 되는데, 화면은 그냥
    // "평균" 이라고만 적어 그 차이를 알 수 없었다.
    //
    // 실측 2026-08-23: 8 종목 중 2 종목이 이력 10 건이었다(RISE 코리아밸류업위클리고정커버드콜 ·
    // TIGER 코리아배당다우존스위클리커버드콜, 각각 9 개월 구간). 두 종목의 예상 월배당 합은 32,518 원으로
    // 전체의 1.2% 라 금액 영향은 작지만, "1년 평균" 이라고 읽히는 값이 아닌 것은 밝혀야 한다.
    model.addAttribute("shortHistorySymbols", shortHistoryLabels(rows));
  }

  /** 이력이 12건에 못 미치는 종목의 "이름(건수)" 목록. 없으면 빈 목록. */
  private List<String> shortHistoryLabels(List<MonthlyDividendSnapshotResponse> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<String, Long> payoutCountBySymbol = new LinkedHashMap<>();
    try {
      List<MonthlyDividendPayoutResponse> payouts =
          monthlyDividendPayoutClient.findPayouts(new LinkedMultiValueMap<>());
      for (MonthlyDividendPayoutResponse payout :
          payouts == null ? List.<MonthlyDividendPayoutResponse>of() : payouts) {
        String symbol = normalizeMonthlyDividendSymbol(payout.stockItemSymbol());
        if (symbol != null) {
          payoutCountBySymbol.merge(symbol, 1L, Long::sum);
        }
      }
    } catch (RuntimeException ex) {
      // 이 안내가 없다고 화면이 못 뜰 이유는 없다.
      log.warn("월배당 지급 이력 건수를 읽지 못했다", ex);
      return List.of();
    }

    List<String> labels = new ArrayList<>();
    for (MonthlyDividendSnapshotResponse row : rows) {
      String symbol = normalizeMonthlyDividendSymbol(row.stockItemSymbol());
      long count = symbol != null ? payoutCountBySymbol.getOrDefault(symbol, 0L) : 0L;
      if (count > 0 && count < MONTHLY_DIVIDEND_AVERAGE_WINDOW) {
        labels.add(row.stockItemName() + "(" + count + ")");
      }
    }
    return labels;
  }

  /** api-stock 이 "1년 평균" 을 낼 때 쓰는 건수(MonthlyDividendPayoutService 의 limit(12)). */
  private static final int MONTHLY_DIVIDEND_AVERAGE_WINDOW = 12;

  /**
   * 원장 점검에서 규칙마다 받아 올 예시 개수. api-stock 의 상한은 100 이다.
   *
   * <p>기본값(3)으로 두면 화면이 발견의 절반 이상을 감춘다 &mdash; 실측 2026-08-23: 발견 45 건 중 25 건이 예시 밖이었다. 조치하려면 어느 행인지
   * 알아야 하므로 넉넉히 받아 온다(현재 가장 많은 규칙이 12 건).
   */
  static final int LEDGER_INTEGRITY_MAX_EXAMPLES = 20;

  /**
   * 화면에 원 단위로 찍히는 값. 소계는 행 표시값의 합이어야 사용자가 열을 더한 값과 맞는다.
   *
   * <p>실측 2026-08-23 월배당 8 종목: 정확한 합의 소수부가 버려지는 자리가 행과 소계에서 달라 <b>2 원</b> 차이가 났다. 지금은 양쪽 다 반올림해 같은
   * 값이 된다.
   */
  static long displayWon(BigDecimal amount) {
    return StockFormatUtil.displayWon(amount);
  }

  static BigDecimal sumExpectedMonthlyDividend(List<MonthlyDividendSnapshotResponse> rows) {
    return rows.stream()
        .map(
            row ->
                BigDecimal.valueOf(
                    displayWon(
                        row.expectedMonthlyDividend() != null
                            ? row.expectedMonthlyDividend()
                            : BigDecimal.ZERO)))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  static BigDecimal sumLatestMonthlyDividend(List<MonthlyDividendSnapshotResponse> rows) {
    return rows.stream()
        .map(row -> BigDecimal.valueOf(displayWon(latestMonthlyDividend(row))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal latestMonthlyDividend(MonthlyDividendSnapshotResponse row) {
    BigDecimal perShare =
        row.latestMonthlyDividendPerShare() != null
            ? row.latestMonthlyDividendPerShare()
            : BigDecimal.ZERO;
    long quantity = row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L;
    return perShare.multiply(BigDecimal.valueOf(quantity));
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile")
  public String saveMonthlyDividendProfile(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @ModelAttribute MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    try {
      normalizeMonthlyDividendProfileRequest(monthlyDividendProfileForm);
      validateMonthlyDividendProfileRequest(monthlyDividendProfileForm);
      monthlyDividendProfileClient.upsertProfile(monthlyDividendProfileForm);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, monthlyDividendProfileForm.getSymbol(), "profile-saved");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendProfileForm.getSymbol(),
          ex.getMessage(),
          monthlyDividendProfileForm,
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendProfileForm.getSymbol()));
    } catch (Exception ex) {
      log.warn("월배당 프로필 저장 실패: symbol={}", monthlyDividendProfileForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendProfileForm.getSymbol(),
          failureMessage(ex, "월배당 프로필을 저장하지 못했습니다."),
          monthlyDividendProfileForm,
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendProfileForm.getSymbol()));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile/delete")
  public String deleteMonthlyDividendProfile(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam String symbol) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    try {
      validateMonthlyDividendSymbol(normalizedSymbol);
      monthlyDividendProfileClient.deleteProfile(normalizedSymbol);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, normalizedSymbol, "profile-deleted");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutForm(normalizedSymbol));
    } catch (Exception ex) {
      log.warn("월배당 프로필 삭제 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          failureMessage(ex, "월배당 프로필을 삭제하지 못했습니다."),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutForm(normalizedSymbol));
    }
  }

  @BlueskyPreAuthorize
  @PutMapping(
      value = "/dividend/monthly-reference/profile/order",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> reorderMonthlyDividendProfiles(
      HttpServletRequest request,
      @RequestBody MonthlyDividendProfileReorderRequest monthlyDividendProfileReorderRequest) {
    if (isNotAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "message",
                  io.github.luversof.boot.context.support.MessageUtil.getMessage(
                      "stock.label.login.required"),
                  "isDisplayableMessage",
                  true));
    }

    try {
      normalizeMonthlyDividendProfileReorderRequest(monthlyDividendProfileReorderRequest);
      validateMonthlyDividendProfileReorderRequest(monthlyDividendProfileReorderRequest);
      monthlyDividendProfileClient.reorderProfiles(monthlyDividendProfileReorderRequest);
      return ResponseEntity.ok(Map.of("result", "profile-reordered"));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", ex.getMessage(), "isDisplayableMessage", true));
    } catch (Exception ex) {
      log.warn("월배당 프로필 순서 저장 실패", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "message",
                  failureMessage(ex, "월배당 프로필 순서를 저장하지 못했습니다."),
                  "isDisplayableMessage",
                  true));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout")
  public String saveMonthlyDividendPayout(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @ModelAttribute MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    try {
      normalizeMonthlyDividendPayoutRequest(monthlyDividendPayoutForm);
      validateMonthlyDividendPayoutRequest(monthlyDividendPayoutForm);
      monthlyDividendPayoutClient.upsertPayout(monthlyDividendPayoutForm);
      return buildMonthlyDividendReferenceRedirect(
          request,
          redirectAttributes,
          monthlyDividendPayoutForm.getSymbol(),
          "payout-saved",
          monthlyDividendPayoutForm.getRecordDate(),
          monthlyDividendPayoutForm.getPayDate());
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutForm.getSymbol(),
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutForm.getSymbol()),
          monthlyDividendPayoutForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 저장 실패: symbol={}", monthlyDividendPayoutForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutForm.getSymbol(),
          failureMessage(ex, "월배당 지급 이력을 저장하지 못했습니다."),
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutForm.getSymbol()),
          monthlyDividendPayoutForm);
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import")
  public String importMonthlyDividendPayouts(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @ModelAttribute MonthlyDividendPayoutImportRequest monthlyDividendPayoutImportForm) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    try {
      normalizeMonthlyDividendPayoutImportRequest(monthlyDividendPayoutImportForm);
      validateMonthlyDividendSymbol(monthlyDividendPayoutImportForm.getSymbol());

      List<MonthlyDividendPayoutUpsertRequest> importRequests =
          monthlyDividendPayoutImportParser.parse(
              monthlyDividendPayoutImportForm.getSymbol(),
              monthlyDividendPayoutImportForm.getBulkInput());
      saveMonthlyDividendPayoutRequests(importRequests);

      return buildMonthlyDividendReferenceRedirect(
          request,
          redirectAttributes,
          monthlyDividendPayoutImportForm.getSymbol(),
          "payout-imported");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutImportForm.getSymbol(),
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutImportForm.getSymbol()),
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendPayoutImportForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 일괄 저장 실패: symbol={}", monthlyDividendPayoutImportForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutImportForm.getSymbol(),
          failureMessage(ex, "월배당 지급 이력을 일괄 저장하지 못했습니다."),
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutImportForm.getSymbol()),
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendPayoutImportForm);
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import/source")
  public String importMonthlyDividendPayoutsFromSource(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam String symbol) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    MonthlyDividendProfileResponse profile = findMonthlyDividendProfile(normalizedSymbol);
    try {
      validateMonthlyDividendSymbol(normalizedSymbol);
      if (profile == null) {
        throw new IllegalArgumentException("저장된 월배당 프로필이 없습니다.");
      }
      if (!StringUtils.hasText(profile.sourceUrl())) {
        throw new IllegalArgumentException("저장된 출처 URL이 없습니다.");
      }

      List<MonthlyDividendPayoutUpsertRequest> importRequests =
          monthlyDividendPayoutSourceImportService.fetchImportRequests(
              normalizedSymbol, profile.sourceUrl());
      saveMonthlyDividendPayoutRequests(importRequests);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, normalizedSymbol, "payout-source-imported");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          profile != null
              ? buildDefaultMonthlyDividendProfileForm(profile)
              : buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutImportForm(normalizedSymbol));
    } catch (Exception ex) {
      log.warn("저장된 출처 URL 가져오기 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          failureMessage(ex, "저장된 출처 URL에서 월배당 지급 이력을 가져오지 못했습니다."),
          profile != null
              ? buildDefaultMonthlyDividendProfileForm(profile)
              : buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutImportForm(normalizedSymbol));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import/source/bulk")
  public String importMonthlyDividendPayoutsFromSourceBulk(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @RequestParam String payoutWindow) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String normalizedWindow =
        payoutWindow != null ? payoutWindow.trim().toUpperCase(Locale.ROOT) : "";
    if (!"MID_MONTH".equals(normalizedWindow) && !"MONTH_END".equals(normalizedWindow)) {
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceResultMessage",
          MessageUtil.getMessage(
              "stock.page.dividend.monthly.reference.payout.import.source.bulk.window.invalid"));
      redirectAttributes.addFlashAttribute("monthlyDividendReferenceResultIsError", true);
      return buildMonthlyDividendReferenceBulkRedirect(request);
    }

    // 선택한 지급 시기(월 중/월말) + 활성 + 출처 URL이 있는 프로필만 대상으로 한다.
    List<MonthlyDividendProfileResponse> targets =
        loadMonthlyDividendProfiles().stream()
            .filter(MonthlyDividendProfileResponse::active)
            .filter(profile -> normalizedWindow.equals(safeString(profile.payoutWindow())))
            .filter(profile -> StringUtils.hasText(profile.sourceUrl()))
            .toList();

    String windowLabel = resolveMonthlyDividendPayoutWindowLabel(normalizedWindow);

    if (targets.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceResultMessage",
          MessageFormat.format(
              MessageUtil.getMessage(
                  "stock.page.dividend.monthly.reference.payout.import.source.bulk.empty"),
              windowLabel));
      redirectAttributes.addFlashAttribute("monthlyDividendReferenceResultIsError", false);
      return buildMonthlyDividendReferenceBulkRedirect(request);
    }

    // 건별 실패는 잡아서 계속 진행하고, 성공/실패 건수와 실패 심볼을 집계해 결과로 안내한다.
    int successCount = 0;
    List<String> failedSymbols = new ArrayList<>();
    for (MonthlyDividendProfileResponse profile : targets) {
      String symbol = normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      try {
        List<MonthlyDividendPayoutUpsertRequest> importRequests =
            monthlyDividendPayoutSourceImportService.fetchImportRequests(
                symbol, profile.sourceUrl());
        saveMonthlyDividendPayoutRequests(importRequests);
        successCount++;
      } catch (Exception ex) {
        failedSymbols.add(symbol);
        log.warn(
            "월배당 출처 일괄 가져오기 실패: window={}, symbol={}, sourceUrl={}",
            normalizedWindow,
            symbol,
            profile.sourceUrl(),
            ex);
      }
    }

    String message =
        MessageFormat.format(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.payout.import.source.bulk.result"),
            windowLabel,
            targets.size(),
            successCount,
            failedSymbols.size());
    if (!failedSymbols.isEmpty()) {
      message +=
          " "
              + MessageFormat.format(
                  MessageUtil.getMessage(
                      "stock.page.dividend.monthly.reference.payout.import.source.bulk.failed.symbols"),
                  String.join(", ", failedSymbols));
    }
    redirectAttributes.addFlashAttribute("monthlyDividendReferenceResultMessage", message);
    redirectAttributes.addFlashAttribute(
        "monthlyDividendReferenceResultIsError", successCount == 0);

    return buildMonthlyDividendReferenceBulkRedirect(request);
  }

  private String resolveMonthlyDividendPayoutWindowLabel(String payoutWindow) {
    if ("MID_MONTH".equals(payoutWindow)) {
      return MessageUtil.getMessage(
          "stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
    }
    if ("MONTH_END".equals(payoutWindow)) {
      return MessageUtil.getMessage(
          "stock.page.dividend.monthly.reference.profile.payout.window.month.end");
    }
    return payoutWindow;
  }

  private String buildMonthlyDividendReferenceBulkRedirect(HttpServletRequest request) {
    String profileSort =
        monthlyDividendViewSupport.resolveProfileSort(request.getParameter("profileSort"));
    String profileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(
            profileSort, request.getParameter("profileDirection"));
    StringBuilder redirectUrl =
        new StringBuilder("redirect:/stock/admin?tab=").append(DIVIDEND_TAB_MONTHLY_REFERENCE);
    appendQueryParam(redirectUrl, "profileSort", profileSort);
    appendQueryParam(redirectUrl, "profileDirection", profileDirection);
    return redirectUrl.toString();
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/delete")
  public String deleteMonthlyDividendPayout(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam String symbol,
      @RequestParam LocalDate recordDate,
      @RequestParam LocalDate payDate) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    MonthlyDividendPayoutUpsertRequest payoutForm =
        buildDefaultMonthlyDividendPayoutForm(normalizedSymbol);
    payoutForm.setRecordDate(recordDate);
    payoutForm.setPayDate(payDate);

    try {
      validateMonthlyDividendSymbol(normalizedSymbol);
      if (recordDate == null) {
        throw new IllegalArgumentException("지급기준일은 필수입니다.");
      }
      if (payDate == null) {
        throw new IllegalArgumentException("실지급일은 필수입니다.");
      }
      monthlyDividendPayoutClient.deletePayout(normalizedSymbol, recordDate, payDate);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, normalizedSymbol, "payout-deleted");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          payoutForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 삭제 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          failureMessage(ex, "월배당 지급 이력을 삭제하지 못했습니다."),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          payoutForm);
    }
  }

  @BlueskyPreAuthorize
  @GetMapping("/trade")
  public String tradePage(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    UUID userId = UserUtil.getUserId();
    var accounts = loadAccounts(userId);
    model.addAttribute("accounts", accounts);

    var stockItems = loadStockItems();
    model.addAttribute("stockItems", stockItems);
    return "stock/trade";
  }

  /** 종목 상세: 한 종목의 보유/손익 요약 + 매매·배당 내역을 모아 보여준다(기존 엔드포인트 재활용). */
  @BlueskyPreAuthorize
  @GetMapping("/item")
  public String stockItemDetailPage(
      HttpServletRequest request,
      @RequestParam(required = false) String stockItemId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) java.time.Instant startDate,
      @RequestParam(required = false) java.time.Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    UUID userId = UserUtil.getUserId();

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
      model.addAttribute("contentReady", false);
      model.addAttribute(
          "stockItemIdParam",
          stockItem != null && stockItem.id() != null
              ? stockItem.id().toString()
              : (stockItemId != null ? stockItemId : ""));
      return "stock/stockItemDetail";
    }
    model.addAttribute("contentReady", true);

    if (stockItem == null || stockItem.id() == null) {
      model.addAttribute("stockItem", null);
      return "stock/stockItemDetail";
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

    var profitsFuture = stockAsync.supply(() -> tradeProfitClient.calculateProfit(profitParams));
    var snapshotFuture = stockAsync.supply(() -> tradeProfitClient.calculateProfit(snapshotParams));
    var tradesFuture = stockAsync.supply(() -> tradeClient.findTrades(tradeSearchParamsPre));
    var dividendsFuture = stockAsync.supply(() -> dividendClient.findDividends(dividendParamsPre));
    var timeSeriesFuture =
        stockAsync.supply(() -> tradeProfitClient.timeSeriesWithSummary(seriesParamsPre));
    var accountsFuture = stockAsync.supply(() -> accountClient.getAccountsByUserId(userId));

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

    model.addAttribute("holdingQuantity", holdingQuantity);
    model.addAttribute("averageBuyPrice", averageBuyPrice);
    model.addAttribute("currentPrice", currentPrice);
    model.addAttribute("evaluationAmount", evaluationAmount);
    model.addAttribute("evaluationProfit", evaluationProfit);
    model.addAttribute("realizedProfit", realizedProfit);
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
    model.addAttribute(
        "priceBasisDate",
        net.luversof.web.gate.stock.util.StockPriceBasisUtil.priceBasisDateWithFallback(
            snapshotProfits));

    model.addAttribute("trades", trades);
    model.addAttribute("dividends", dividends);
    return "stock/stockItemDetail";
  }

  /** 계좌 상세: 한 계좌의 보유/손익 요약 + 보유 종목 + 매매·배당 내역(종목 상세와 대칭, 필터 키만 account). */
  @BlueskyPreAuthorize
  @GetMapping("/account")
  public String accountDetailPage(
      HttpServletRequest request,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) java.time.Instant startDate,
      @RequestParam(required = false) java.time.Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    UUID userId = UserUtil.getUserId();

    UUID parsedId = parseUuidOrNull(accountId);
    // 계좌 단건 조회는 소유자를 가리지 않으므로 여기서 확인한다(규칙과 근거는 StockOwnershipUtil).
    Account account =
        parsedId != null
            ? StockOwnershipUtil.ownedOrNull(accountClient.getAccountById(parsedId), userId)
            : null;

    // 초기 진입(비 htmx)은 셸만 렌더하고, 콘텐츠는 전역 기간과 함께 htmx로 로드한다.
    // (풀페이지 새로고침 시에도 선택 기간이 서버에 적용되도록.)
    if (request.getHeader("HX-Request") == null) {
      model.addAttribute("contentReady", false);
      model.addAttribute(
          "accountIdParam",
          account != null && account.id() != null
              ? account.id().toString()
              : (accountId != null ? accountId : ""));
      return "stock/accountDetail";
    }
    model.addAttribute("contentReady", true);

    if (account == null || account.id() == null) {
      model.addAttribute("account", null);
      return "stock/accountDetail";
    }
    model.addAttribute("account", account);
    UUID resolvedId = account.id();

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

    var accProfitsFuture =
        stockAsync.supply(() -> tradeProfitClient.calculateProfit(accProfitParams));
    var accSnapshotFuture =
        stockAsync.supply(() -> tradeProfitClient.calculateProfit(accSnapshotParams));
    var accTradesFuture = stockAsync.supply(() -> tradeClient.findTrades(accTradeParams));
    var accDividendsFuture =
        stockAsync.supply(() -> dividendClient.findDividends(accDividendParams));
    var accTimeSeriesFuture =
        stockAsync.supply(() -> tradeProfitClient.timeSeries(accSeriesParams));
    var accStockItemsFuture = stockAsync.supply(this::loadStockItems);

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
    model.addAttribute("totalDividend", totalDividend);
    model.addAttribute("trades", trades);
    model.addAttribute("dividends", dividends);
    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute(
        "chartFormatter",
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(java.time.ZoneId.systemDefault()));
    return "stock/accountDetail";
  }

  /**
   * 백엔드가 알려준 실패 사유를 꺼낸다. 없으면 {@code null}.
   *
   * <p>api-stock 호출이 실패하면 bluesky-boot 의 {@code BlueskyClientResponseErrorHandler} 가 응답 본문을 {@link
   * BlueskyException} 으로 바꿔 던진다. 그 안에는 백엔드가 "사용자에게 보여도 되는 문구"로 표시한 실제 사유가 들어 있는데, 화면은 그것을 버리고 "입력값을
   * 확인해 주세요" 같은 문구로 덮고 있었다. 원인을 아는데도 모른다고 말하는 셈이다.
   *
   * <p>{@code displayableMessage} 가 아닌 메시지는 내부용(예외 클래스명 등)이라 그대로 보여주지 않는다.
   */
  static String remoteDisplayableMessage(Throwable throwable) {
    if (!(throwable instanceof BlueskyException blueskyException)) {
      return null;
    }
    List<ErrorMessage> candidates = new ArrayList<>();
    if (blueskyException.getErrorMessage() != null) {
      candidates.add(blueskyException.getErrorMessage());
    }
    if (blueskyException.getErrorMessageList() != null) {
      candidates.addAll(blueskyException.getErrorMessageList());
    }
    for (ErrorMessage candidate : candidates) {
      if (candidate instanceof BlueskyErrorMessage errorMessage
          && errorMessage.isDisplayableMessage()
          && errorMessage.getMessage() != null
          && !errorMessage.getMessage().isBlank()) {
        return errorMessage.getMessage();
      }
    }
    return null;
  }

  /**
   * 사용자에게 보여줄 실패 문구.
   *
   * <p>백엔드가 사유를 알려줬으면 그것을 그대로 쓰고, 아니면 결과만 말한다. 예전에는 원인을 모르는 경우에도 "입력값을 확인해 주세요"라고 적어 입력 탓으로 돌렸는데, 이
   * 경로는 지역 검증({@code IllegalArgumentException})이 이미 걸러낸 뒤라 정의상 입력 문제가 아닌 실패다 (연결 실패·서버 오류 등). 원인을
   * 모를 때 아는 척하지 않는다.
   */
  private String failureMessage(Throwable throwable, String fallback) {
    String remote = remoteDisplayableMessage(throwable);
    return remote != null ? remote : fallback;
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

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth")
  public String assetGrowthPage(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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
          failureMessage(ex, "월배당 데이터를 저장하지 못했습니다."),
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
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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
        "월배당 기준 데이터 등록은 배당 메뉴의 월배당 기준 데이터 탭에서 관리합니다.",
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
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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
          "배당주 검색 시트에서 보유/평단가를 가져오지 못했습니다. 시트 설정과 권한을 확인해 주세요.",
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

  private List<MonthlyDividendSnapshotResponse> loadMonthlyDividendRows(UUID userId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    return monthlyDividendSnapshotClient.findSnapshots(params);
  }

  private String resolveDividendTab(String tab) {
    if (DIVIDEND_TAB_MONTHLY_REFERENCE.equalsIgnoreCase(tab)) {
      return DIVIDEND_TAB_MONTHLY_REFERENCE;
    }
    if ("calendar".equalsIgnoreCase(tab)) {
      return "calendar";
    }
    return "history";
  }

  private String resolveAdminTab(String tab) {
    return DIVIDEND_TAB_MONTHLY_REFERENCE.equalsIgnoreCase(tab)
        ? DIVIDEND_TAB_MONTHLY_REFERENCE
        : ADMIN_TAB_DATA_MANAGEMENT;
  }

  private void populateMonthlyDividendReferenceModel(
      Model model,
      String requestedSymbol,
      String requestedProfileSort,
      String requestedProfileDirection,
      LocalDate requestedPayoutRecordDate,
      LocalDate requestedPayoutPayDate) {
    List<StockItem> stockItems = loadMonthlyDividendStockItems();
    String profileSort = monthlyDividendViewSupport.resolveProfileSort(requestedProfileSort);
    String profileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(profileSort, requestedProfileDirection);
    List<MonthlyDividendProfileResponse> profiles =
        monthlyDividendViewSupport.sortProfiles(
            loadMonthlyDividendProfiles(), profileSort, profileDirection);
    List<StockItem> selectableStockItems =
        mergeMonthlyDividendReferenceStockItems(stockItems, profiles);
    String selectedSymbol =
        resolveMonthlyDividendReferenceSymbol(
            model, requestedSymbol, profiles, selectableStockItems);
    MonthlyDividendProfileResponse selectedProfile =
        profiles.stream()
            .filter(
                profile -> selectedSymbol.equalsIgnoreCase(safeString(profile.stockItemSymbol())))
            .findFirst()
            .orElse(null);
    List<MonthlyDividendPayoutResponse> payouts = loadMonthlyDividendPayouts(selectedSymbol);
    MonthlyDividendPayoutResponse selectedPayout =
        resolveSelectedMonthlyDividendPayout(
            model, payouts, requestedPayoutRecordDate, requestedPayoutPayDate);

    model.addAttribute("stockItems", selectableStockItems);
    model.addAttribute("monthlyDividendProfiles", profiles);
    model.addAttribute("monthlyDividendProfileSort", profileSort);
    model.addAttribute("monthlyDividendProfileDirection", profileDirection);
    model.addAttribute("monthlyDividendPayouts", payouts);
    model.addAttribute("selectedMonthlyDividendSymbol", selectedSymbol);
    model.addAttribute(
        "selectedMonthlyDividendSourceUrl",
        selectedProfile != null ? safeString(selectedProfile.sourceUrl()) : "");
    model.addAttribute("monthlyDividendProfileExists", selectedProfile != null);
    model.addAttribute("monthlyDividendPayoutExists", selectedPayout != null);
    model.addAttribute(
        "selectedMonthlyDividendPayoutKey", buildMonthlyDividendPayoutKey(selectedPayout));
    model.addAttribute(
        "selectedMonthlyDividendPayoutRecordDate",
        selectedPayout != null ? String.valueOf(selectedPayout.recordDate()) : "");
    model.addAttribute(
        "selectedMonthlyDividendPayoutPayDate",
        selectedPayout != null ? String.valueOf(selectedPayout.payDate()) : "");
    model.addAttribute(
        "monthlyDividendReferenceSummary",
        monthlyDividendCalculator.buildReferenceSummary(selectedSymbol, payouts));

    if (!model.containsAttribute("monthlyDividendProfileForm")) {
      model.addAttribute(
          "monthlyDividendProfileForm",
          selectedProfile != null
              ? buildDefaultMonthlyDividendProfileForm(selectedProfile)
              : buildDefaultMonthlyDividendProfileForm(selectedSymbol, profiles));
    }
    if (!model.containsAttribute("monthlyDividendPayoutForm")) {
      model.addAttribute(
          "monthlyDividendPayoutForm",
          buildDefaultMonthlyDividendPayoutForm(selectedSymbol, selectedPayout));
    }
    if (!model.containsAttribute("monthlyDividendPayoutImportForm")) {
      model.addAttribute(
          "monthlyDividendPayoutImportForm",
          buildDefaultMonthlyDividendPayoutImportForm(selectedSymbol));
    }
    if (!model.containsAttribute("monthlyDividendReferenceErrorMessage")) {
      model.addAttribute("monthlyDividendReferenceErrorMessage", "");
    }
    if (!model.containsAttribute("monthlyDividendReferenceResult")) {
      model.addAttribute("monthlyDividendReferenceResult", "");
    }
  }

  private MonthlyDividendPayoutResponse resolveSelectedMonthlyDividendPayout(
      Model model,
      List<MonthlyDividendPayoutResponse> payouts,
      LocalDate requestedPayoutRecordDate,
      LocalDate requestedPayoutPayDate) {
    MonthlyDividendPayoutResponse selectedPayout =
        findMonthlyDividendPayout(payouts, requestedPayoutRecordDate, requestedPayoutPayDate);
    if (selectedPayout != null) {
      return selectedPayout;
    }

    Object payoutFormAttr = model.asMap().get("monthlyDividendPayoutForm");
    if (payoutFormAttr instanceof MonthlyDividendPayoutUpsertRequest payoutForm) {
      return findMonthlyDividendPayout(
          payouts, payoutForm.getRecordDate(), payoutForm.getPayDate());
    }

    return null;
  }

  private MonthlyDividendPayoutResponse findMonthlyDividendPayout(
      List<MonthlyDividendPayoutResponse> payouts, LocalDate recordDate, LocalDate payDate) {
    if (recordDate == null || payDate == null || payouts == null || payouts.isEmpty()) {
      return null;
    }

    return payouts.stream()
        .filter(row -> recordDate.equals(row.recordDate()) && payDate.equals(row.payDate()))
        .findFirst()
        .orElse(null);
  }

  private String buildMonthlyDividendPayoutKey(MonthlyDividendPayoutResponse payout) {
    return payout == null
        ? ""
        : buildMonthlyDividendPayoutKey(payout.recordDate(), payout.payDate());
  }

  private String buildMonthlyDividendPayoutKey(LocalDate recordDate, LocalDate payDate) {
    if (recordDate == null || payDate == null) {
      return "";
    }

    return recordDate + "|" + payDate;
  }

  private List<MonthlyDividendProfileResponse> loadMonthlyDividendProfiles() {
    List<MonthlyDividendProfileResponse> profiles =
        monthlyDividendProfileClient.findProfiles(new LinkedMultiValueMap<>());
    return profiles != null ? profiles : List.of();
  }

  private List<StockItem> mergeMonthlyDividendReferenceStockItems(
      List<StockItem> stockItems, List<MonthlyDividendProfileResponse> profiles) {
    Map<String, StockItem> stockItemsBySymbol = new LinkedHashMap<>();

    if (stockItems != null) {
      for (StockItem stockItem : stockItems) {
        if (stockItem == null || !StringUtils.hasText(stockItem.symbol())) {
          continue;
        }

        String normalizedSymbol = stockItem.symbol().trim().toUpperCase(Locale.ROOT);
        stockItemsBySymbol.put(normalizedSymbol, stockItem);
      }
    }

    if (profiles != null) {
      profiles.stream()
          .filter(profile -> StringUtils.hasText(profile.stockItemSymbol()))
          .sorted(
              Comparator.comparing(
                  profile -> safeString(profile.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER))
          .forEach(
              profile -> {
                String normalizedSymbol = profile.stockItemSymbol().trim().toUpperCase(Locale.ROOT);
                stockItemsBySymbol.putIfAbsent(
                    normalizedSymbol,
                    new StockItem(
                        profile.stockItemId(),
                        normalizedSymbol,
                        StringUtils.hasText(profile.stockItemName())
                            ? profile.stockItemName().trim()
                            : normalizedSymbol,
                        null,
                        List.of(MONTHLY_DIVIDEND_TAG)));
              });
    }

    return List.copyOf(stockItemsBySymbol.values());
  }

  private List<MonthlyDividendPayoutResponse> loadMonthlyDividendPayouts(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return List.of();
    }

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("symbol", symbol.trim().toUpperCase(Locale.ROOT));
    return monthlyDividendPayoutClient.findPayouts(params);
  }

  private MonthlyDividendProfileResponse findMonthlyDividendProfile(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    return loadMonthlyDividendProfiles().stream()
        .filter(row -> normalizedSymbol.equalsIgnoreCase(safeString(row.stockItemSymbol())))
        .findFirst()
        .orElse(null);
  }

  private String resolveMonthlyDividendReferenceSymbol(
      Model model,
      String requestedSymbol,
      List<MonthlyDividendProfileResponse> profiles,
      List<StockItem> stockItems) {
    String normalizedRequestedSymbol =
        normalizeMonthlyDividendReferenceSymbol(requestedSymbol, profiles, stockItems);
    if (normalizedRequestedSymbol != null) {
      return normalizedRequestedSymbol;
    }

    Object profileFormAttr = model.asMap().get("monthlyDividendProfileForm");
    if (profileFormAttr instanceof MonthlyDividendProfileUpsertRequest profileForm) {
      String normalizedProfileSymbol =
          normalizeMonthlyDividendReferenceSymbol(profileForm.getSymbol(), profiles, stockItems);
      if (normalizedProfileSymbol != null) {
        return normalizedProfileSymbol;
      }
    }

    Object payoutFormAttr = model.asMap().get("monthlyDividendPayoutForm");
    if (payoutFormAttr instanceof MonthlyDividendPayoutUpsertRequest payoutForm) {
      String normalizedPayoutSymbol =
          normalizeMonthlyDividendReferenceSymbol(payoutForm.getSymbol(), profiles, stockItems);
      if (normalizedPayoutSymbol != null) {
        return normalizedPayoutSymbol;
      }
    }

    Object payoutImportFormAttr = model.asMap().get("monthlyDividendPayoutImportForm");
    if (payoutImportFormAttr instanceof MonthlyDividendPayoutImportRequest payoutImportForm) {
      String normalizedPayoutImportSymbol =
          normalizeMonthlyDividendReferenceSymbol(
              payoutImportForm.getSymbol(), profiles, stockItems);
      if (normalizedPayoutImportSymbol != null) {
        return normalizedPayoutImportSymbol;
      }
    }

    if (!profiles.isEmpty() && StringUtils.hasText(profiles.get(0).stockItemSymbol())) {
      return profiles.get(0).stockItemSymbol().trim().toUpperCase(Locale.ROOT);
    }

    if (!stockItems.isEmpty() && StringUtils.hasText(stockItems.get(0).symbol())) {
      return stockItems.get(0).symbol().trim().toUpperCase(Locale.ROOT);
    }

    return "";
  }

  private String normalizeMonthlyDividendReferenceSymbol(
      String symbol, List<MonthlyDividendProfileResponse> profiles, List<StockItem> stockItems) {
    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    boolean existsInProfiles =
        profiles != null
            && profiles.stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    boolean existsInStockItems =
        stockItems != null
            && stockItems.stream()
                .map(StockItem::symbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    return existsInProfiles || existsInStockItems ? normalizedSymbol : null;
  }

  private MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(
      MonthlyDividendProfileResponse profile) {
    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    if (profile == null) {
      request.setActive(true);
      request.setDisplayOrder(1);
      request.setPayoutWindow("UNKNOWN");
      return request;
    }

    request.setSymbol(profile.stockItemSymbol());
    request.setSourceUrl(profile.sourceUrl());
    request.setPayoutWindow(profile.payoutWindow());
    request.setDisplayOrder(profile.displayOrder());
    request.setActive(profile.active());
    request.setNote(profile.note());
    request.setLastVerifiedDate(profile.lastVerifiedDate());
    return request;
  }

  private MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(
      String symbol, List<MonthlyDividendProfileResponse> profiles) {
    MonthlyDividendProfileUpsertRequest request = buildDefaultMonthlyDividendProfileForm(symbol);
    request.setDisplayOrder(resolveNextMonthlyDividendProfileDisplayOrder(profiles));
    return request;
  }

  private MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(
      String symbol) {
    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setActive(true);
    request.setDisplayOrder(1);
    request.setPayoutWindow("UNKNOWN");
    return request;
  }

  private int resolveNextMonthlyDividendProfileDisplayOrder(
      List<MonthlyDividendProfileResponse> profiles) {
    return profiles.stream()
        .map(MonthlyDividendProfileResponse::displayOrder)
        .filter(java.util.Objects::nonNull)
        .max(Integer::compareTo)
        .map(value -> value + 1)
        .orElse(1);
  }

  private MonthlyDividendPayoutUpsertRequest buildDefaultMonthlyDividendPayoutForm(String symbol) {
    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setRecordDate(LocalDate.now());
    request.setPayDate(LocalDate.now());
    return request;
  }

  private MonthlyDividendPayoutUpsertRequest buildDefaultMonthlyDividendPayoutForm(
      String symbol, MonthlyDividendPayoutResponse payout) {
    if (payout == null) {
      return buildDefaultMonthlyDividendPayoutForm(symbol);
    }

    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setRecordDate(payout.recordDate());
    request.setPayDate(payout.payDate());
    request.setDistributionRatePct(payout.distributionRatePct());
    request.setDividendAmountPerShare(payout.dividendAmountPerShare());
    request.setTaxableBasePerShare(payout.taxableBasePerShare());
    return request;
  }

  private MonthlyDividendPayoutImportRequest buildDefaultMonthlyDividendPayoutImportForm(
      String symbol) {
    MonthlyDividendPayoutImportRequest request = new MonthlyDividendPayoutImportRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    return request;
  }

  private void saveMonthlyDividendPayoutRequests(
      List<MonthlyDividendPayoutUpsertRequest> requests) {
    requests.forEach(
        request -> {
          normalizeMonthlyDividendPayoutRequest(request);
          validateMonthlyDividendPayoutRequest(request);
          monthlyDividendPayoutClient.upsertPayout(request);
        });
  }

  private String buildMonthlyDividendReferenceRedirect(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      String symbol,
      String result) {
    return buildMonthlyDividendReferenceRedirect(
        request, redirectAttributes, symbol, result, null, null);
  }

  private String buildMonthlyDividendReferencePageRedirect(
      String symbol,
      String profileSort,
      String profileDirection,
      LocalDate payoutRecordDate,
      LocalDate payoutPayDate) {
    String resolvedProfileSort = monthlyDividendViewSupport.resolveProfileSort(profileSort);
    String resolvedProfileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(resolvedProfileSort, profileDirection);
    StringBuilder redirectUrl =
        new StringBuilder("redirect:/stock/admin?tab=").append(DIVIDEND_TAB_MONTHLY_REFERENCE);
    appendQueryParam(redirectUrl, "symbol", symbol);
    appendQueryParam(redirectUrl, "profileSort", resolvedProfileSort);
    appendQueryParam(redirectUrl, "profileDirection", resolvedProfileDirection);
    appendQueryParam(redirectUrl, "payoutRecordDate", payoutRecordDate);
    appendQueryParam(redirectUrl, "payoutPayDate", payoutPayDate);
    return redirectUrl.toString();
  }

  private String buildMonthlyDividendReferenceRedirect(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      String symbol,
      String result,
      LocalDate payoutRecordDate,
      LocalDate payoutPayDate) {
    // 결과 코드는 URL 이 아니라 flash 로 전달한다. URL 쿼리에 남기면 새로고침/재진입마다
    // 이전 결과 메시지가 다시 표시되는 문제가 있다.
    if (redirectAttributes != null && StringUtils.hasText(result)) {
      redirectAttributes.addFlashAttribute("monthlyDividendReferenceResult", result);
    }
    String profileSort =
        monthlyDividendViewSupport.resolveProfileSort(request.getParameter("profileSort"));
    String profileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(
            profileSort, request.getParameter("profileDirection"));
    return buildMonthlyDividendReferencePageRedirect(
        symbol, profileSort, profileDirection, payoutRecordDate, payoutPayDate);
  }

  private String renderMonthlyDividendReferenceError(
      HttpServletRequest request,
      Model model,
      String symbol,
      String errorMessage,
      MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm,
      MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm) {
    return renderMonthlyDividendReferenceError(
        request,
        model,
        symbol,
        errorMessage,
        monthlyDividendProfileForm,
        monthlyDividendPayoutForm,
        buildDefaultMonthlyDividendPayoutImportForm(symbol));
  }

  private String renderMonthlyDividendReferenceError(
      HttpServletRequest request,
      Model model,
      String symbol,
      String errorMessage,
      MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm,
      MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm,
      MonthlyDividendPayoutImportRequest monthlyDividendPayoutImportForm) {
    model.addAttribute("adminTab", DIVIDEND_TAB_MONTHLY_REFERENCE);
    model.addAttribute("monthlyDividendProfileForm", monthlyDividendProfileForm);
    model.addAttribute("monthlyDividendPayoutForm", monthlyDividendPayoutForm);
    model.addAttribute("monthlyDividendPayoutImportForm", monthlyDividendPayoutImportForm);
    model.addAttribute("monthlyDividendReferenceErrorMessage", errorMessage);
    model.addAttribute("monthlyDividendReferenceResult", "");
    populateMonthlyDividendReferenceModel(
        model,
        symbol,
        request.getParameter("profileSort"),
        request.getParameter("profileDirection"),
        monthlyDividendPayoutForm != null ? monthlyDividendPayoutForm.getRecordDate() : null,
        monthlyDividendPayoutForm != null ? monthlyDividendPayoutForm.getPayDate() : null);
    return "stock/admin";
  }

  /** 원장의 현재 보유 수량(종목 단위, 계좌 합산). 조회에 실패하면 빈 맵이라 표시는 예전 그대로다. */
  /** 원장의 현재 보유 상태(종목 단위). 조회 한 번으로 수량과 평균단가를 함께 얻는다. */
  private record CurrentHoldings(
      Map<UUID, Integer> quantities, Map<UUID, BigDecimal> averageBuyPrices) {

    static CurrentHoldings empty() {
      return new CurrentHoldings(Map.of(), Map.of());
    }
  }

  /** 조회에 실패하면 빈 맵이라 표시는 예전 그대로다(없는 값을 지어내지 않는다). */
  private CurrentHoldings loadCurrentHoldings(UUID userId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    params.add("groupBy", "STOCKITEM");
    Map<UUID, Integer> quantities = new HashMap<>();
    Map<UUID, BigDecimal> averageBuyPrices = new HashMap<>();
    try {
      List<TradeProfit> rows = tradeProfitClient.calculateProfit(params);
      if (rows != null) {
        for (TradeProfit row : rows) {
          if (row.stockItemId() != null) {
            quantities.merge(row.stockItemId(), row.holdingQuantity(), Integer::sum);
            if (row.averageBuyPrice() != null) {
              averageBuyPrices.put(row.stockItemId(), row.averageBuyPrice());
            }
          }
        }
      }
    } catch (Exception ex) {
      log.warn("현재 보유 상태 조회 실패: userId={}", userId, ex);
      return CurrentHoldings.empty();
    }
    return new CurrentHoldings(quantities, averageBuyPrices);
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
    var currentHoldingsFuture = stockAsync.supply(() -> loadCurrentHoldings(userId));
    List<MonthlyDividendSnapshotResponse> allRows = loadMonthlyDividendRows(userId);
    List<MonthlyDividendProfileResponse> monthlyDividendProfiles =
        monthlyDividendViewSupport.sortProfiles(
            loadMonthlyDividendProfiles(), MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER, "asc");
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
      String profileSymbol = normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      if (profileSymbol != null
          && profile.payoutWindow() != null
          && !monthlyDividendPayoutWindowBySymbol.containsKey(profileSymbol)) {
        monthlyDividendPayoutWindowBySymbol.put(profileSymbol, profile.payoutWindow());
      }
    }

    // 스냅샷의 보유 수량은 사람이 갱신한 시점의 값이라 원장과 어긋날 수 있다. 이 표는 그 수량을
    // 그대로 '보유수량' 으로 찍으므로, 어긋나면 사용자는 자기 보유량을 잘못 읽는다
    // (실측 2026-08-23: 8 종목 중 7 종목이 달랐고 전부 현재가 더 많았다).
    CurrentHoldings currentHoldings =
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
            .map(this::normalizeMonthlyDividendSymbol)
            .filter(StringUtils::hasText)
            .distinct()
            .toList());
    model.addAttribute(
        "monthlyDividendReorderEnabled",
        MONTHLY_DIVIDEND_PROFILE_SORT_DISPLAY_ORDER.equals(monthlyDividendSort)
            && "asc".equals(monthlyDividendDirection));
    model.addAttribute("stockItems", loadMonthlyDividendStockItems());
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

  private List<Account> loadAccounts(UUID userId) {
    List<Account> accounts = accountClient.getAccountsByUserId(userId);
    if (accounts == null || accounts.isEmpty()) {
      return List.of();
    }

    return accounts.stream().filter(account -> account != null).toList();
  }

  private List<StockItem> loadStockItems() {
    List<StockItem> stockItems = stockItemClient.getStockItems();
    if (stockItems == null || stockItems.isEmpty()) {
      return List.of();
    }

    return stockItems.stream()
        .filter(stockItem -> stockItem != null)
        .sorted(
            Comparator.comparing(
                stockItem -> safeString(stockItem.symbol()), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private List<StockItem> loadMonthlyDividendStockItems() {
    List<StockItem> stockItems = stockItemClient.getStockItemsByTag(MONTHLY_DIVIDEND_TAG);
    if (stockItems == null || stockItems.isEmpty()) {
      return List.of();
    }

    return stockItems.stream()
        .filter(stockItem -> stockItem != null)
        .sorted(
            Comparator.comparing(
                stockItem -> safeString(stockItem.symbol()), String.CASE_INSENSITIVE_ORDER))
        .toList();
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
    appendQueryParam(redirectUrl, "sort", resolvedSort);
    appendQueryParam(
        redirectUrl,
        "direction",
        monthlyDividendViewSupport.resolveRowDirection(resolvedSort, direction));
    return redirectUrl.toString();
  }

  private void appendQueryParam(StringBuilder redirectUrl, String key, Object value) {
    if (value == null) {
      return;
    }

    String text = String.valueOf(value).trim();
    if (!StringUtils.hasText(text)) {
      return;
    }

    redirectUrl.append('&').append(key).append('=');
    redirectUrl.append(URLEncoder.encode(text, StandardCharsets.UTF_8));
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
            .filter(row -> normalizedSymbol.equalsIgnoreCase(safeString(row.stockItemSymbol())))
            .findFirst()
            .orElse(null);
    if (savedRow != null) {
      request.setAsOfDate(
          savedRow.asOfDate() != null ? savedRow.asOfDate() : request.getAsOfDate());
      request.setHeldQuantity(savedRow.heldQuantity());
      request.setAverageBuyPrice(savedRow.averageBuyPrice());
    }

    List<MonthlyDividendPayoutResponse> payouts = loadMonthlyDividendPayouts(normalizedSymbol);
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
    validateMonthlyDividendSymbol(request.getSymbol());

    List<MonthlyDividendPayoutResponse> payouts = loadMonthlyDividendPayouts(request.getSymbol());
    MonthlyDividendReferenceSummaryView summary =
        monthlyDividendCalculator.buildReferenceSummary(request.getSymbol(), payouts);
    if (summary.payoutCount() <= 0) {
      throw new IllegalArgumentException("월배당 기준 데이터가 없습니다. 배당 메뉴의 월배당 기준 데이터에서 먼저 등록해 주세요.");
    }

    LocalDate referenceDate =
        summary.latestPayDate() != null ? summary.latestPayDate() : summary.latestRecordDate();
    if (referenceDate == null) {
      throw new IllegalArgumentException("기준 데이터에 사용할 지급일 정보가 없습니다.");
    }

    request.setAsOfDate(referenceDate);
    request.setLatestMonthlyDividendPerShare(summary.latestDividendAmountPerShare());
    request.setAverageMonthlyDividendPerShare1y(summary.averageDividendAmountPerShare1y());
    request.setAverageTaxableBaseRatio1y(summary.averageTaxableBaseRatio1y());
  }

  private void normalizeMonthlyDividendProfileRequest(MonthlyDividendProfileUpsertRequest request) {
    request.setSymbol(normalizeMonthlyDividendSymbol(request.getSymbol()));
    request.setSourceUrl(trimToNull(request.getSourceUrl()));
    request.setNote(trimToNull(request.getNote()));
  }

  private void validateMonthlyDividendProfileRequest(MonthlyDividendProfileUpsertRequest request) {
    validateMonthlyDividendSymbol(request.getSymbol());
  }

  private void normalizeMonthlyDividendProfileReorderRequest(
      MonthlyDividendProfileReorderRequest request) {
    if (request == null || request.getSymbols() == null) {
      return;
    }

    request.setSymbols(
        request.getSymbols().stream()
            .filter(StringUtils::hasText)
            .map(this::normalizeMonthlyDividendSymbol)
            .toList());
  }

  private void validateMonthlyDividendProfileReorderRequest(
      MonthlyDividendProfileReorderRequest request) {
    if (request == null || request.getSymbols() == null || request.getSymbols().isEmpty()) {
      throw new IllegalArgumentException("변경할 월배당 프로필 순서가 없습니다.");
    }

    request.getSymbols().forEach(this::validateMonthlyDividendSymbol);
    if (request.getSymbols().size() != request.getSymbols().stream().distinct().count()) {
      throw new IllegalArgumentException("중복된 종목코드가 포함되어 있습니다.");
    }
  }

  private void normalizeMonthlyDividendPayoutRequest(MonthlyDividendPayoutUpsertRequest request) {
    request.setSymbol(normalizeMonthlyDividendSymbol(request.getSymbol()));
  }

  private void normalizeMonthlyDividendPayoutImportRequest(
      MonthlyDividendPayoutImportRequest request) {
    request.setSymbol(normalizeMonthlyDividendSymbol(request.getSymbol()));
    request.setBulkInput(
        StringUtils.hasText(request.getBulkInput()) ? request.getBulkInput().trim() : null);
  }

  private void validateMonthlyDividendPayoutRequest(MonthlyDividendPayoutUpsertRequest request) {
    validateMonthlyDividendSymbol(request.getSymbol());
    if (request.getRecordDate() == null) {
      throw new IllegalArgumentException("지급기준일은 필수입니다.");
    }
    if (request.getPayDate() == null) {
      throw new IllegalArgumentException("실지급일은 필수입니다.");
    }
    if (request.getPayDate().isBefore(request.getRecordDate())) {
      throw new IllegalArgumentException("실지급일은 지급기준일보다 빠를 수 없습니다.");
    }
    if (request.getDistributionRatePct() != null
        && request.getDistributionRatePct().compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("분배율은 0 이상이어야 합니다.");
    }
    requireNonNegative(request.getDividendAmountPerShare(), "주당 분배금은 0 이상이어야 합니다.");
    requireNonNegative(request.getTaxableBasePerShare(), "주당 과세표준액은 0 이상이어야 합니다.");
    // 과세표준은 분배금 중 과세 대상 몫이라 분배금을 넘을 수 없다. api-stock 도 같은 검증을 하지만,
    // 여기서 먼저 걸러야 사용자가 다른 항목과 같은 형식의 안내를 본다(서버까지 가면 일반 오류가 뜬다).
    if (request.getTaxableBasePerShare() != null
        && request.getDividendAmountPerShare() != null
        && request.getTaxableBasePerShare().compareTo(request.getDividendAmountPerShare()) > 0) {
      throw new IllegalArgumentException("주당 과세표준액은 주당 분배금보다 클 수 없습니다.");
    }
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
      throw new IllegalArgumentException("종목코드는 필수입니다.");
    }
    if (request.getAsOfDate() == null) {
      throw new IllegalArgumentException("기준일은 필수입니다.");
    }
    if (request.getHeldQuantity() == null || request.getHeldQuantity() <= 0) {
      throw new IllegalArgumentException("보유 수량은 1 이상이어야 합니다.");
    }
    requireNonNegative(request.getLatestMonthlyDividendPerShare(), "최근 주당 월배당금은 0 이상이어야 합니다.");
    requireNonNegative(
        request.getAverageMonthlyDividendPerShare1y(), "1년 평균 주당 월배당금은 0 이상이어야 합니다.");
    requireNonNegative(request.getAverageBuyPrice(), "매수 평단가는 0 이상이어야 합니다.");

    BigDecimal taxableBaseRatio = safe(request.getAverageTaxableBaseRatio1y());
    if (taxableBaseRatio.compareTo(BigDecimal.ZERO) < 0
        || taxableBaseRatio.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("1년 평균 과세표준 비중은 0에서 100 사이여야 합니다.");
    }
  }

  private void requireNonNegative(BigDecimal value, String message) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(message);
    }
  }

  List<MonthlyDividendSnapshotUpsertRequest> parseBulkInput(String bulkInput, UUID userId) {
    if (!StringUtils.hasText(bulkInput)) {
      throw new IllegalArgumentException("붙여넣기 데이터가 비어 있습니다.");
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
        throw new IllegalArgumentException((index + 1) + "번째 줄은 7개 열이 필요합니다.");
      }

      // 콤마로 나눈 줄에 열이 더 있으면 숫자의 천단위 콤마가 열을 갈라놓은 것이다.
      // 이대로 앞 7개만 쓰면 값이 한 칸씩 밀려도 전부 숫자로 읽혀 오류 없이 잘못 저장된다
      // (실측: "…,50,1,000,71,887" 이 9열이 되어 보유 수량 1,000 -> 1, 평단가 71,887 -> 000).
      // 탭으로 나눈 줄은 콤마가 값 안에 남아 있어 안전하므로 이 검사가 필요 없다.
      if (!line.contains("	") && columns.length > 7) {
        throw new IllegalArgumentException(
            (index + 1)
                + "번째 줄의 열이 7개보다 많습니다. 숫자에 천단위 콤마가 있으면 열이 잘못 나뉩니다."
                + " 탭으로 구분하거나 콤마를 빼고 붙여넣어 주세요.");
      }

      MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
      request.setUserId(userId);
      request.setSymbol(columns[0]);
      request.setAsOfDate(parseLocalDate(columns[1], index + 1));
      request.setLatestMonthlyDividendPerShare(
          parseBigDecimal(columns[2], index + 1, "최근 주당 월배당금"));
      request.setAverageMonthlyDividendPerShare1y(
          parseBigDecimal(columns[3], index + 1, "1년 평균 주당 월배당금"));
      request.setAverageTaxableBaseRatio1y(parseBigDecimal(columns[4], index + 1, "1년 평균 과세표준 비중"));
      request.setHeldQuantity(parseInteger(columns[5], index + 1, "보유 수량"));
      request.setAverageBuyPrice(parseBigDecimal(columns[6], index + 1, "매수 평단가"));

      normalizeMonthlyDividendRequest(request, userId);
      validateMonthlyDividendRequest(request);
      requests.add(request);
    }

    if (requests.isEmpty()) {
      throw new IllegalArgumentException("등록할 데이터가 없습니다.");
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
    String normalized = safeString(firstColumn).trim().toLowerCase(Locale.ROOT);
    return "symbol".equals(normalized) || "ticker".equals(normalized) || "종목코드".equals(normalized);
  }

  private LocalDate parseLocalDate(String value, int lineNumber) {
    try {
      return LocalDate.parse(value.trim().replace('/', '-').replace('.', '-'));
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(lineNumber + "번째 줄의 기준일 형식이 올바르지 않습니다.");
    }
  }

  private BigDecimal parseBigDecimal(String value, int lineNumber, String label) {
    try {
      return new BigDecimal(value.trim().replace(",", "").replace("%", ""));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(lineNumber + "번째 줄의 " + label + " 값이 올바르지 않습니다.");
    }
  }

  private Integer parseInteger(String value, int lineNumber, String label) {
    try {
      return Integer.valueOf(value.trim().replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(lineNumber + "번째 줄의 " + label + " 값이 올바르지 않습니다.");
    }
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }

  private String normalizeMonthlyDividendSymbol(String symbol) {
    return StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null;
  }

  private void validateMonthlyDividendSymbol(String symbol) {
    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    if (!StringUtils.hasText(normalizedSymbol)) {
      throw new IllegalArgumentException("종목코드는 필수입니다.");
    }

    if (normalizedSymbol.startsWith("HTTP://") || normalizedSymbol.startsWith("HTTPS://")) {
      throw new IllegalArgumentException(
          "종목코드에는 URL이 아니라 종목 심볼을 입력해 주세요. 출처 URL은 출처 URL 칸에 넣으면 됩니다.");
    }

    boolean knownStockSymbol =
        loadStockItems().stream()
                .map(StockItem::symbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals)
            || loadMonthlyDividendProfiles().stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    if (!knownStockSymbol) {
      throw new IllegalArgumentException("등록되지 않은 종목코드입니다. 종목 심볼을 다시 확인해 주세요.");
    }
  }

  private String safeString(String value) {
    return value != null ? value : "";
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
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
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

    if (DIVIDEND_TAB_MONTHLY_REFERENCE.equals(adminTab)) {
      // 결과 코드(monthlyDividendReferenceResult)는 POST 후 flash 로만 전달된다.
      // URL 쿼리로 받으면 새로고침마다 이전 결과 메시지가 재표시되는 버그가 있어 제거했다.
      populateMonthlyDividendReferenceModel(
          model, symbol, profileSort, profileDirection, payoutRecordDate, payoutPayDate);
    }

    return "stock/admin";
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
