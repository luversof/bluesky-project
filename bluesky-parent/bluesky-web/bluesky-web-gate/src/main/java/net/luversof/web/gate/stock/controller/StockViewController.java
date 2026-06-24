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

    UUID userId = UserUtil.getUserId();
    var accounts = loadAccounts(userId);
    model.addAttribute("accounts", accounts);
    model.addAttribute("userId", userId);

    var stockItems = loadStockItems();
    model.addAttribute("stockItems", stockItems);
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
      @RequestParam(required = false) LocalDate payoutPayDate,
      @RequestParam(required = false) String result) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String dividendTab = resolveDividendTab(tab);

    if (DIVIDEND_TAB_MONTHLY_REFERENCE.equals(dividendTab)) {
      return buildMonthlyDividendReferencePageRedirect(
          symbol, profileSort, profileDirection, result, payoutRecordDate, payoutPayDate);
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
  }

  private BigDecimal sumExpectedMonthlyDividend(List<MonthlyDividendSnapshotResponse> rows) {
    return rows.stream()
        .map(
            row ->
                row.expectedMonthlyDividend() != null
                    ? row.expectedMonthlyDividend()
                    : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile")
  public String saveMonthlyDividendProfile(
      HttpServletRequest request,
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
          request, monthlyDividendProfileForm.getSymbol(), "profile-saved");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendProfileForm.getSymbol(),
          ex.getMessage(),
          monthlyDividendProfileForm,
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendProfileForm.getSymbol()));
    } catch (Exception ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendProfileForm.getSymbol(),
          "월배당 프로필을 저장하지 못했습니다. 입력값을 확인해 주세요.",
          monthlyDividendProfileForm,
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendProfileForm.getSymbol()));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile/delete")
  public String deleteMonthlyDividendProfile(
      HttpServletRequest request, Model model, @RequestParam String symbol) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    try {
      validateMonthlyDividendSymbol(normalizedSymbol);
      monthlyDividendProfileClient.deleteProfile(normalizedSymbol);
      return buildMonthlyDividendReferenceRedirect(request, normalizedSymbol, "profile-deleted");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          buildDefaultMonthlyDividendPayoutForm(normalizedSymbol));
    } catch (Exception ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          "월배당 프로필을 삭제하지 못했습니다. 다시 확인해 주세요.",
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
          .body(Map.of("message", "로그인이 필요합니다.", "isDisplayableMessage", true));
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
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "message", "월배당 프로필 순서를 저장하지 못했습니다. 다시 시도해 주세요.", "isDisplayableMessage", true));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout")
  public String saveMonthlyDividendPayout(
      HttpServletRequest request,
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
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutForm.getSymbol(),
          "월배당 지급 이력을 저장하지 못했습니다. 입력값을 확인해 주세요.",
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutForm.getSymbol()),
          monthlyDividendPayoutForm);
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import")
  public String importMonthlyDividendPayouts(
      HttpServletRequest request,
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
          request, monthlyDividendPayoutImportForm.getSymbol(), "payout-imported");
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
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutImportForm.getSymbol(),
          "월배당 지급 이력을 일괄 저장하지 못했습니다. 입력값을 확인해 주세요.",
          buildDefaultMonthlyDividendProfileForm(monthlyDividendPayoutImportForm.getSymbol()),
          buildDefaultMonthlyDividendPayoutForm(monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendPayoutImportForm);
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import/source")
  public String importMonthlyDividendPayoutsFromSource(
      HttpServletRequest request, Model model, @RequestParam String symbol) {
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
          request, normalizedSymbol, "payout-source-imported");
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
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          "저장된 출처 URL에서 월배당 지급 이력을 가져오지 못했습니다. 다시 확인해 주세요.",
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
      return buildMonthlyDividendReferenceRedirect(request, normalizedSymbol, "payout-deleted");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          payoutForm);
    } catch (Exception ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          "월배당 지급 이력을 삭제하지 못했습니다. 다시 확인해 주세요.",
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
    if (stockItem == null || stockItem.id() == null) {
      model.addAttribute("stockItem", null);
      return "stock/stockItemDetail";
    }
    model.addAttribute("stockItem", stockItem);
    UUID resolvedId = stockItem.id();

    // 이 종목의 보유/손익 집계 (계좌 합산)
    TradeProfitRequest profitRequest = new TradeProfitRequest();
    profitRequest.setUserId(userId);
    profitRequest.setStockItemIdList(List.of(resolvedId));
    List<TradeProfit> profits = tradeProfitClient.calculateProfit(profitRequest.toParams());
    if (profits == null) {
      profits = List.of();
    }
    int holdingQuantity = profits.stream().mapToInt(TradeProfit::holdingQuantity).sum();
    BigDecimal totalBuyCost = sumTradeProfit(profits, TradeProfit::totalBuyCost);
    BigDecimal evaluationAmount = sumTradeProfit(profits, TradeProfit::evaluationAmount);
    BigDecimal evaluationProfit = sumTradeProfit(profits, TradeProfit::evaluationProfitNet);
    BigDecimal realizedProfit = sumTradeProfit(profits, TradeProfit::realizedProfitNet);
    BigDecimal currentPrice =
        profits.stream()
            .map(TradeProfit::currentPrice)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    BigDecimal averageBuyPrice =
        holdingQuantity > 0
            ? totalBuyCost.divide(
                BigDecimal.valueOf(holdingQuantity), 0, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // 매매 내역 (이 종목, 최신순)
    TradeSearchRequest tradeSearchRequest =
        new TradeSearchRequest(userId, null, List.of(resolvedId), null, null);
    List<TradeResponse> trades = tradeClient.findTrades(tradeSearchRequest.toParams());
    if (trades == null) {
      trades = List.of();
    }
    trades =
        trades.stream()
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    // 배당 내역 (이 종목, 최신순)
    MultiValueMap<String, String> dividendParams = new LinkedMultiValueMap<>();
    dividendParams.add("userId", userId.toString());
    dividendParams.add("stockItemIdList", resolvedId.toString());
    List<DividendResponse> dividends = dividendClient.findDividends(dividendParams);
    if (dividends == null) {
      dividends = List.of();
    }
    dividends =
        dividends.stream()
            .sorted(
                Comparator.comparing(
                    DividendResponse::payDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    BigDecimal totalDividend =
        dividends.stream()
            .map(dividend -> dividend.netAmount() != null ? dividend.netAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 보유 평가액·원가 추이 (차트용, 전체 기간 AUTO 단위)
    TradeProfitRequest seriesRequest = new TradeProfitRequest();
    seriesRequest.setUserId(userId);
    seriesRequest.setStockItemIdList(List.of(resolvedId));
    MultiValueMap<String, String> seriesParams = seriesRequest.toParams();
    seriesParams.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> timeSeries = tradeProfitClient.timeSeries(seriesParams);
    if (timeSeries == null) {
      timeSeries = List.of();
    }
    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute(
        "chartFormatter",
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(java.time.ZoneId.systemDefault()));

    model.addAttribute("holdingQuantity", holdingQuantity);
    model.addAttribute("averageBuyPrice", averageBuyPrice);
    model.addAttribute("currentPrice", currentPrice);
    model.addAttribute("evaluationAmount", evaluationAmount);
    model.addAttribute("evaluationProfit", evaluationProfit);
    model.addAttribute("realizedProfit", realizedProfit);
    model.addAttribute("totalBuyCost", totalBuyCost);
    model.addAttribute("totalDividend", totalDividend);
    model.addAttribute("trades", trades);
    model.addAttribute("dividends", dividends);
    return "stock/stockItemDetail";
  }

  /** 계좌 상세: 한 계좌의 보유/손익 요약 + 보유 종목 + 매매·배당 내역(종목 상세와 대칭, 필터 키만 account). */
  @BlueskyPreAuthorize
  @GetMapping("/account")
  public String accountDetailPage(
      HttpServletRequest request, @RequestParam(required = false) String accountId, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    UUID userId = UserUtil.getUserId();

    UUID parsedId = parseUuidOrNull(accountId);
    Account account = parsedId != null ? accountClient.getAccountById(parsedId).orElse(null) : null;
    if (account == null || account.id() == null) {
      model.addAttribute("account", null);
      return "stock/accountDetail";
    }
    model.addAttribute("account", account);
    UUID resolvedId = account.id();

    // 종목 id → 종목명 (보유/내역 표의 종목명 + 종목 상세 링크용)
    Map<UUID, String> stockNameById = new HashMap<>();
    loadStockItems()
        .forEach(
            s -> {
              if (s.id() != null) {
                stockNameById.put(s.id(), s.name() != null ? s.name() : "-");
              }
            });
    model.addAttribute("stockNameById", stockNameById);

    // 이 계좌의 종목별 보유/손익
    TradeProfitRequest profitRequest = new TradeProfitRequest();
    profitRequest.setUserId(userId);
    profitRequest.setAccountIdList(List.of(resolvedId));
    List<TradeProfit> profits = tradeProfitClient.calculateProfit(profitRequest.toParams());
    if (profits == null) {
      profits = List.of();
    }
    List<TradeProfit> enriched =
        profits.stream()
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
    BigDecimal totalBuyCost = sumTradeProfit(enriched, TradeProfit::totalBuyCost);
    BigDecimal evaluationProfit = sumTradeProfit(enriched, TradeProfit::evaluationProfitNet);
    BigDecimal realizedProfit = sumTradeProfit(enriched, TradeProfit::realizedProfitNet);

    // 매매 내역 (이 계좌, 최신순)
    TradeSearchRequest tradeSearchRequest =
        new TradeSearchRequest(userId, List.of(resolvedId), null, null, null);
    List<TradeResponse> trades = tradeClient.findTrades(tradeSearchRequest.toParams());
    if (trades == null) {
      trades = List.of();
    }
    trades =
        trades.stream()
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    // 배당 내역 (이 계좌, 최신순)
    MultiValueMap<String, String> dividendParams = new LinkedMultiValueMap<>();
    dividendParams.add("userId", userId.toString());
    dividendParams.add("accountIdList", resolvedId.toString());
    List<DividendResponse> dividends = dividendClient.findDividends(dividendParams);
    if (dividends == null) {
      dividends = List.of();
    }
    dividends =
        dividends.stream()
            .sorted(
                Comparator.comparing(
                    DividendResponse::payDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    BigDecimal totalDividend =
        dividends.stream()
            .map(dividend -> dividend.netAmount() != null ? dividend.netAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 평가액·원가 추이 (차트용)
    TradeProfitRequest seriesRequest = new TradeProfitRequest();
    seriesRequest.setUserId(userId);
    seriesRequest.setAccountIdList(List.of(resolvedId));
    MultiValueMap<String, String> seriesParams = seriesRequest.toParams();
    seriesParams.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> timeSeries = tradeProfitClient.timeSeries(seriesParams);
    if (timeSeries == null) {
      timeSeries = List.of();
    }

    model.addAttribute("holdings", holdings);
    model.addAttribute("holdingCount", holdings.size());
    model.addAttribute("evaluationAmount", evaluationAmount);
    model.addAttribute("totalBuyCost", totalBuyCost);
    model.addAttribute("evaluationProfit", evaluationProfit);
    model.addAttribute("realizedProfit", realizedProfit);
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

  @BlueskyPreAuthorize
  @GetMapping("/realized-profit")
  public String realizedProfitPage(HttpServletRequest request, Model model) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }
    return "stock/realizedProfit";
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
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) String result,
      @RequestParam(required = false) Integer savedCount) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String simulatorTab = resolveSimulatorTab(tab);
    model.addAttribute("simulatorTab", simulatorTab);

    if ("monthly-dividend".equals(simulatorTab)) {
      UUID userId = UserUtil.getUserId();
      populateMonthlyDividendModel(
          model, userId, sort, direction, keyword, minAnnualYield, positiveOnly, symbol);
      model.addAttribute("monthlyDividendResult", result != null ? result : "");
      model.addAttribute("monthlyDividendSavedCount", savedCount);
    }

    return "stock/simulator";
  }

  @BlueskyPreAuthorize
  @PostMapping("/simulator/monthly-dividend")
  public String saveMonthlyDividend(
      HttpServletRequest request,
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
          sort, direction, keyword, minAnnualYield, positiveOnly, "single-saved", 1);
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
      return renderMonthlyDividendError(
          model,
          userId,
          sort,
          direction,
          keyword,
          minAnnualYield,
          positiveOnly,
          "월배당 데이터를 저장하지 못했습니다. 종목코드와 입력값을 확인해 주세요.",
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
          sort, direction, keyword, minAnnualYield, positiveOnly, "sheet-imported", processed);
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
      HttpServletRequest request, String symbol, String result) {
    return buildMonthlyDividendReferenceRedirect(request, symbol, result, null, null);
  }

  private String buildMonthlyDividendReferencePageRedirect(
      String symbol,
      String profileSort,
      String profileDirection,
      String result,
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
    appendQueryParam(redirectUrl, "result", result);
    return redirectUrl.toString();
  }

  private String buildMonthlyDividendReferenceRedirect(
      HttpServletRequest request,
      String symbol,
      String result,
      LocalDate payoutRecordDate,
      LocalDate payoutPayDate) {
    String profileSort =
        monthlyDividendViewSupport.resolveProfileSort(request.getParameter("profileSort"));
    String profileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(
            profileSort, request.getParameter("profileDirection"));
    return buildMonthlyDividendReferencePageRedirect(
        symbol, profileSort, profileDirection, result, payoutRecordDate, payoutPayDate);
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

    model.addAttribute("monthlyDividendRows", filteredRows);
    model.addAttribute(
        "monthlyDividendSummary", monthlyDividendCalculator.buildSimulatorSummary(filteredRows));
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

  private boolean hasMonthlyDividendTag(StockItem stockItem) {
    if (stockItem == null || stockItem.tags() == null || stockItem.tags().isEmpty()) {
      return false;
    }

    return stockItem.tags().stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .anyMatch(MONTHLY_DIVIDEND_TAG::equals);
  }

  private String buildMonthlyDividendRedirect(
      String sort,
      String direction,
      String keyword,
      BigDecimal minAnnualYield,
      boolean positiveOnly,
      String result,
      Integer savedCount) {
    StringBuilder redirectUrl = new StringBuilder("redirect:/stock/simulator?tab=monthly-dividend");
    String resolvedSort = monthlyDividendViewSupport.resolveRowSort(sort);
    appendQueryParam(redirectUrl, "sort", resolvedSort);
    appendQueryParam(
        redirectUrl,
        "direction",
        monthlyDividendViewSupport.resolveRowDirection(resolvedSort, direction));
    appendQueryParam(redirectUrl, "result", result);
    appendQueryParam(redirectUrl, "savedCount", savedCount);
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

  private List<MonthlyDividendSnapshotUpsertRequest> parseBulkInput(String bulkInput, UUID userId) {
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

  private String[] splitBulkColumns(String line) {
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
      @RequestParam(required = false) LocalDate payoutPayDate,
      @RequestParam(required = false) String result) {
    if (isNotAuthenticated()) {
      return getLoginRedirectUrl(request);
    }

    String adminTab = resolveAdminTab(tab);
    model.addAttribute("adminTab", adminTab);

    if (DIVIDEND_TAB_MONTHLY_REFERENCE.equals(adminTab)) {
      populateMonthlyDividendReferenceModel(
          model, symbol, profileSort, profileDirection, payoutRecordDate, payoutPayDate);
      model.addAttribute("monthlyDividendReferenceResult", result != null ? result : "");
    }

    return "stock/admin";
  }
}
