package net.luversof.web.gate.stock.controller;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
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
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutImportRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutSourceImportService;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 배당 화면과 <b>월배당 기준(프로필·지급이력) 관리</b> 폼.
 *
 * <p>{@code StockViewController} 안에 있을 때는 관리 화면·시뮬레이터·상세와 뒤섞여 있어, 한 폼을 고치려면 무관한 코드를 지나다녀야 했다. 이 갈래는
 * 저장·삭제·가져오기가 전부 같은 화면(관리 화면의 월배당 기준 탭)으로 되돌아가는 한 덩어리다.
 *
 * <p>조회·검증·기본 폼 조립은 {@link net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport} 에
 * 있다 &mdash; 관리 화면도 같은 것을 쓰기 때문이다. 여기 남은 것은 <b>받은 값을 다듬고, 부르고, 어디로 되돌릴지 정하는</b> 일뿐이다.
 *
 * <p>동작은 그대로다 &mdash; 옮기기만 했다.
 */
@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendViewController {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(StockDividendViewController.class);

  private static final int MONTHLY_DIVIDEND_AVERAGE_WINDOW = 12;

  @Autowired private MonthlyDividendProfileClient monthlyDividendProfileClient;

  @Autowired private MonthlyDividendPayoutClient monthlyDividendPayoutClient;

  @Autowired private MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser;

  @Autowired
  private MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService;

  @Autowired private MonthlyDividendViewSupport monthlyDividendViewSupport;

  @Autowired
  private net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
      monthlyDividendReferenceSupport;

  public void setMonthlyDividendReferenceSupport(
      net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
          monthlyDividendReferenceSupport) {
    this.monthlyDividendReferenceSupport = monthlyDividendReferenceSupport;
  }

  public void setMonthlyDividendViewSupport(MonthlyDividendViewSupport monthlyDividendViewSupport) {
    this.monthlyDividendViewSupport = monthlyDividendViewSupport;
  }

  public void setMonthlyDividendProfileClient(
      MonthlyDividendProfileClient monthlyDividendProfileClient) {
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
  }

  public void setMonthlyDividendPayoutClient(
      MonthlyDividendPayoutClient monthlyDividendPayoutClient) {
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
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
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String dividendTab = resolveDividendTab(tab);

    if (net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
        .DIVIDEND_TAB_MONTHLY_REFERENCE
        .equals(dividendTab)) {
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
    List<MonthlyDividendSnapshotResponse> rows =
        monthlyDividendReferenceSupport.loadMonthlyDividendRows(userId);
    if (rows == null) {
      rows = List.of();
    }

    Map<String, String> payoutWindowBySymbol = new LinkedHashMap<>();
    for (MonthlyDividendProfileResponse profile :
        monthlyDividendReferenceSupport.loadMonthlyDividendProfiles()) {
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
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
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(row.stockItemSymbol());
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
            rows, monthlyDividendReferenceSupport.loadCurrentHoldings(userId).quantities());
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
        String symbol =
            monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(
                payout.stockItemSymbol());
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
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(row.stockItemSymbol());
      long count = symbol != null ? payoutCountBySymbol.getOrDefault(symbol, 0L) : 0L;
      if (count > 0 && count < MONTHLY_DIVIDEND_AVERAGE_WINDOW) {
        labels.add(row.stockItemName() + "(" + count + ")");
      }
    }
    return labels;
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile")
  public String saveMonthlyDividendProfile(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @ModelAttribute MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
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
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(
              monthlyDividendProfileForm.getSymbol()));
    } catch (Exception ex) {
      log.warn("월배당 프로필 저장 실패: symbol={}", monthlyDividendProfileForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendProfileForm.getSymbol(),
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.profile.save.failed")),
          monthlyDividendProfileForm,
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(
              monthlyDividendProfileForm.getSymbol()));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/profile/delete")
  public String deleteMonthlyDividendProfile(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam String symbol) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String normalizedSymbol =
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(symbol);
    try {
      monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(normalizedSymbol);
      monthlyDividendProfileClient.deleteProfile(normalizedSymbol);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, normalizedSymbol, "profile-deleted");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          ex.getMessage(),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(normalizedSymbol));
    } catch (Exception ex) {
      log.warn("월배당 프로필 삭제 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.profile.delete.failed")),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(normalizedSymbol));
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
    if (StockViewSupport.isNotAuthenticated()) {
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
                  StockViewSupport.failureMessage(
                      ex, msg("stock.monthly.reference.error.profile.order.save.failed")),
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
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
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
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
              monthlyDividendPayoutForm.getSymbol()),
          monthlyDividendPayoutForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 저장 실패: symbol={}", monthlyDividendPayoutForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutForm.getSymbol(),
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.payout.save.failed")),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
              monthlyDividendPayoutForm.getSymbol()),
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
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    try {
      normalizeMonthlyDividendPayoutImportRequest(monthlyDividendPayoutImportForm);
      monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(
          monthlyDividendPayoutImportForm.getSymbol());

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
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
              monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(
              monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendPayoutImportForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 일괄 저장 실패: symbol={}", monthlyDividendPayoutImportForm.getSymbol(), ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          monthlyDividendPayoutImportForm.getSymbol(),
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.payout.bulk.save.failed")),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
              monthlyDividendPayoutImportForm.getSymbol()),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(
              monthlyDividendPayoutImportForm.getSymbol()),
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
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String normalizedSymbol =
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(symbol);
    MonthlyDividendProfileResponse profile = findMonthlyDividendProfile(normalizedSymbol);
    try {
      monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(normalizedSymbol);
      if (profile == null) {
        throw new IllegalArgumentException(msg("stock.monthly.reference.error.profile.missing"));
      }
      if (!StringUtils.hasText(profile.sourceUrl())) {
        throw new IllegalArgumentException(msg("stock.monthly.reference.error.source.url.missing"));
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
              ? monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(profile)
              : monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
                  normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutImportForm(
              normalizedSymbol));
    } catch (Exception ex) {
      log.warn("저장된 출처 URL 가져오기 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.source.import.failed")),
          profile != null
              ? monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(profile)
              : monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(
                  normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(normalizedSymbol),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutImportForm(
              normalizedSymbol));
    }
  }

  @BlueskyPreAuthorize
  @PostMapping("/dividend/monthly-reference/payout/import/source/bulk")
  public String importMonthlyDividendPayoutsFromSourceBulk(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @RequestParam String payoutWindow) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
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
        monthlyDividendReferenceSupport.loadMonthlyDividendProfiles().stream()
            .filter(MonthlyDividendProfileResponse::active)
            .filter(
                profile ->
                    normalizedWindow.equals(StockViewSupport.safeString(profile.payoutWindow())))
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
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      try {
        List<MonthlyDividendPayoutUpsertRequest> importRequests =
            monthlyDividendPayoutSourceImportService.fetchImportRequests(
                symbol, profile.sourceUrl());
        saveMonthlyDividendPayoutRequests(importRequests);
        successCount++;
      } catch (Exception ex) {
        failedSymbols.add(symbol);
        log.warn(
            "monthly dividend source bulk import failed: window={}, symbol={}, sourceUrl={}",
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
        new StringBuilder("redirect:/stock/admin?tab=")
            .append(
                net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
                    .DIVIDEND_TAB_MONTHLY_REFERENCE);
    StockViewSupport.appendQueryParam(redirectUrl, "profileSort", profileSort);
    StockViewSupport.appendQueryParam(redirectUrl, "profileDirection", profileDirection);
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
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String normalizedSymbol =
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(symbol);
    MonthlyDividendPayoutUpsertRequest payoutForm =
        monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutForm(normalizedSymbol);
    payoutForm.setRecordDate(recordDate);
    payoutForm.setPayDate(payDate);

    try {
      monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(normalizedSymbol);
      if (recordDate == null) {
        throw new IllegalArgumentException(
            msg("stock.monthly.reference.error.record.date.required"));
      }
      if (payDate == null) {
        throw new IllegalArgumentException(msg("stock.monthly.reference.error.pay.date.required"));
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
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          payoutForm);
    } catch (Exception ex) {
      log.warn("월배당 지급 이력 삭제 실패: symbol={}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request,
          model,
          normalizedSymbol,
          StockViewSupport.failureMessage(
              ex, msg("stock.monthly.reference.error.payout.delete.failed")),
          monthlyDividendReferenceSupport.buildDefaultMonthlyDividendProfileForm(normalizedSymbol),
          payoutForm);
    }
  }

  private String resolveDividendTab(String tab) {
    if (net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
        .DIVIDEND_TAB_MONTHLY_REFERENCE
        .equalsIgnoreCase(tab)) {
      return net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
          .DIVIDEND_TAB_MONTHLY_REFERENCE;
    }
    if ("calendar".equalsIgnoreCase(tab)) {
      return "calendar";
    }
    return "history";
  }

  private MonthlyDividendProfileResponse findMonthlyDividendProfile(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    return monthlyDividendReferenceSupport.loadMonthlyDividendProfiles().stream()
        .filter(
            row ->
                normalizedSymbol.equalsIgnoreCase(
                    StockViewSupport.safeString(row.stockItemSymbol())))
        .findFirst()
        .orElse(null);
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
        new StringBuilder("redirect:/stock/admin?tab=")
            .append(
                net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
                    .DIVIDEND_TAB_MONTHLY_REFERENCE);
    StockViewSupport.appendQueryParam(redirectUrl, "symbol", symbol);
    StockViewSupport.appendQueryParam(redirectUrl, "profileSort", resolvedProfileSort);
    StockViewSupport.appendQueryParam(redirectUrl, "profileDirection", resolvedProfileDirection);
    StockViewSupport.appendQueryParam(redirectUrl, "payoutRecordDate", payoutRecordDate);
    StockViewSupport.appendQueryParam(redirectUrl, "payoutPayDate", payoutPayDate);
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
        monthlyDividendReferenceSupport.buildDefaultMonthlyDividendPayoutImportForm(symbol));
  }

  private String renderMonthlyDividendReferenceError(
      HttpServletRequest request,
      Model model,
      String symbol,
      String errorMessage,
      MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm,
      MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm,
      MonthlyDividendPayoutImportRequest monthlyDividendPayoutImportForm) {
    model.addAttribute(
        "adminTab",
        net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport
            .DIVIDEND_TAB_MONTHLY_REFERENCE);
    model.addAttribute("monthlyDividendProfileForm", monthlyDividendProfileForm);
    model.addAttribute("monthlyDividendPayoutForm", monthlyDividendPayoutForm);
    model.addAttribute("monthlyDividendPayoutImportForm", monthlyDividendPayoutImportForm);
    model.addAttribute("monthlyDividendReferenceErrorMessage", errorMessage);
    model.addAttribute("monthlyDividendReferenceResult", "");
    monthlyDividendReferenceSupport.populateMonthlyDividendReferenceModel(
        model,
        symbol,
        request.getParameter("profileSort"),
        request.getParameter("profileDirection"),
        monthlyDividendPayoutForm != null ? monthlyDividendPayoutForm.getRecordDate() : null,
        monthlyDividendPayoutForm != null ? monthlyDividendPayoutForm.getPayDate() : null);
    return "stock/admin";
  }

  private void normalizeMonthlyDividendProfileRequest(MonthlyDividendProfileUpsertRequest request) {
    request.setSymbol(
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(request.getSymbol()));
    request.setSourceUrl(trimToNull(request.getSourceUrl()));
    request.setNote(trimToNull(request.getNote()));
  }

  private void validateMonthlyDividendProfileRequest(MonthlyDividendProfileUpsertRequest request) {
    monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(request.getSymbol());
  }

  private void normalizeMonthlyDividendProfileReorderRequest(
      MonthlyDividendProfileReorderRequest request) {
    if (request == null || request.getSymbols() == null) {
      return;
    }

    request.setSymbols(
        request.getSymbols().stream()
            .filter(StringUtils::hasText)
            .map(monthlyDividendReferenceSupport::normalizeMonthlyDividendSymbol)
            .toList());
  }

  private void validateMonthlyDividendProfileReorderRequest(
      MonthlyDividendProfileReorderRequest request) {
    if (request == null || request.getSymbols() == null || request.getSymbols().isEmpty()) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.order.empty"));
    }

    request.getSymbols().forEach(monthlyDividendReferenceSupport::validateMonthlyDividendSymbol);
    if (request.getSymbols().size() != request.getSymbols().stream().distinct().count()) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.order.duplicate"));
    }
  }

  private void normalizeMonthlyDividendPayoutRequest(MonthlyDividendPayoutUpsertRequest request) {
    request.setSymbol(
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(request.getSymbol()));
  }

  private void normalizeMonthlyDividendPayoutImportRequest(
      MonthlyDividendPayoutImportRequest request) {
    request.setSymbol(
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(request.getSymbol()));
    request.setBulkInput(
        StringUtils.hasText(request.getBulkInput()) ? request.getBulkInput().trim() : null);
  }

  private void validateMonthlyDividendPayoutRequest(MonthlyDividendPayoutUpsertRequest request) {
    monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(request.getSymbol());
    if (request.getRecordDate() == null) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.record.date.required"));
    }
    if (request.getPayDate() == null) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.pay.date.required"));
    }
    if (request.getPayDate().isBefore(request.getRecordDate())) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.pay.date.before.record"));
    }
    if (request.getDistributionRatePct() != null
        && request.getDistributionRatePct().compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.distribution.rate.negative"));
    }
    StockViewSupport.requireNonNegative(
        request.getDividendAmountPerShare(),
        msg("stock.monthly.reference.error.dividend.per.share.negative"));
    StockViewSupport.requireNonNegative(
        request.getTaxableBasePerShare(),
        msg("stock.monthly.reference.error.taxable.base.negative"));
    // 과세표준은 분배금 중 과세 대상 몫이라 분배금을 넘을 수 없다. api-stock 도 같은 검증을 하지만,
    // 여기서 먼저 걸러야 사용자가 다른 항목과 같은 형식의 안내를 본다(서버까지 가면 일반 오류가 뜬다).
    if (request.getTaxableBasePerShare() != null
        && request.getDividendAmountPerShare() != null
        && request.getTaxableBasePerShare().compareTo(request.getDividendAmountPerShare()) > 0) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.taxable.base.exceeds.dividend"));
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
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

  /**
   * 화면에 원 단위로 찍히는 값. 소계는 행 표시값의 합이어야 사용자가 열을 더한 값과 맞는다.
   *
   * <p>실측 2026-08-23 월배당 8 종목: 정확한 합의 소수부가 버려지는 자리가 행과 소계에서 달라 <b>2 원</b> 차이가 났다. 지금은 양쪽 다 반올림해 같은
   * 값이 된다.
   */
  static long displayWon(BigDecimal amount) {
    return StockFormatUtil.displayWon(amount);
  }

  private static BigDecimal latestMonthlyDividend(MonthlyDividendSnapshotResponse row) {
    BigDecimal perShare =
        row.latestMonthlyDividendPerShare() != null
            ? row.latestMonthlyDividendPerShare()
            : BigDecimal.ZERO;
    long quantity = row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L;
    return perShare.multiply(BigDecimal.valueOf(quantity));
  }
}
