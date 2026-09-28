package net.luversof.web.gate.stock.controller;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutSourceImportService;

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

  /** 종목별 시세 갱신은 관리 쪽 창구를 쓴다. */
  @Autowired private StockAdminClient stockAdminClient;

  @Autowired private MonthlyDividendPayoutClient monthlyDividendPayoutClient;

  /** 지나간 달의 <b>실제 수령액</b>은 기준 데이터가 아니라 원장에 있다. */
  @Autowired private net.luversof.web.gate.stock.httpexchange.DividendClient dividendClient;

  /** 본문의 서로 기대지 않는 api-stock 조회를 동시에 던지는 실행기(StockViewController 와 같은 것). */
  @Autowired private net.luversof.web.gate.stock.support.StockAsyncSupport stockAsync;

  @Autowired private MonthlyDividendPayoutImportParser monthlyDividendPayoutImportParser;

  @Autowired
  private net.luversof.web.gate.stock.service.MonthlyDividendLinkRegisterService
      monthlyDividendLinkRegisterService;

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
      @RequestParam(required = false) LocalDate payoutPayDate,
      @RequestParam(required = false) String month) {
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

    // 배당 캘린더 탭은 실수령 배당에 통합됐다(사용자 결정 2026-09-17). 옛 주소(북마크 · 공유 링크)는 실수령 배당으로
    // 보내되, 달을 적었으면 그 달을 기간으로 싣는다 - 기간이 달력 한 달이면 실수령 배당이 그 달 달력을 그린다.
    if ("calendar".equals(dividendTab)) {
      return "redirect:"
          + calendarTabRedirect(
              request.getQueryString(), month, LocalDate.now(), java.time.ZoneId.systemDefault());
    }

    populateUpcomingDividendModel(UserUtil.getUserId(), model);
    return "stock/dividend";
  }

  /**
   * 옛 캘린더 탭 주소를 실수령 배당 주소로 바꾼다.
   *
   * <p>{@code tab} · {@code month} 는 버리고 나머지 조건(필터 · 로케일)은 조각째 그대로 옮긴다({@link
   * net.luversof.web.gate.stock.util.StockTabLinkUtil} 과 같은 규칙 &mdash; 다시 묶으면 인코딩과 중복 키 순서가 바뀐다).
   *
   * <p>달을 기간으로 바꾼다 &mdash; 이번 달은 '이번달' 프리셋({@code rangeMode=mtd}), 지난 달은 1 일 0 시 ~ 다음 달 1 일 0 시(배타,
   * {@code rangeMode=1}). 달을 안 적은 주소는 이번 달이다 &mdash; 옛 탭이 그때 이번 달을 그렸고, 대시보드 '다가올 배당' 카드가 바로 그 주소
   * ({@code ?tab=calendar})로 이 탭을 열었다. 기간은 세 키가 한 묶음이라 달을 실을 때는 들고 온 기간 키를 버린다. 앞으로 올 달은 실수령 배당에 보일
   * 것이 없어 기간을 싣지 않는다(다음 달 예정은 화면 위 '다가올 배당' 이 말한다). 못 읽는 달도 기간 없이 보낸다 &mdash; 탭이 없어졌으니 여기서 400 을 낼
   * 까닭이 없다.
   */
  static String calendarTabRedirect(
      String currentQuery, String monthParam, LocalDate today, java.time.ZoneId zone) {
    YearMonth thisMonth = YearMonth.from(today);
    YearMonth month = thisMonth;
    if (StringUtils.hasText(monthParam)) {
      try {
        month = YearMonth.parse(monthParam.trim());
      } catch (DateTimeParseException ex) {
        month = null;
      }
    }
    boolean withRange = month != null && !month.isAfter(thisMonth);
    java.util.Set<String> dropped = new java.util.HashSet<>(java.util.List.of("tab", "month"));
    if (withRange) {
      dropped.addAll(java.util.List.of("startDate", "endDate", "rangeMode"));
    }

    StringBuilder query = new StringBuilder();
    if (currentQuery != null && !currentQuery.isBlank()) {
      for (String part : currentQuery.split("&")) {
        int eq = part.indexOf('=');
        String name = eq < 0 ? part : part.substring(0, eq);
        if (part.isEmpty() || dropped.contains(name)) {
          continue;
        }
        query.append(query.length() == 0 ? "" : "&").append(part);
      }
    }
    if (withRange) {
      if (month.equals(thisMonth)) {
        query.append(query.length() == 0 ? "" : "&").append("rangeMode=mtd");
      } else {
        query
            .append(query.length() == 0 ? "" : "&")
            .append("startDate=")
            .append(month.atDay(1).atStartOfDay(zone).toInstant())
            .append("&endDate=")
            .append(month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant())
            .append("&rangeMode=1");
      }
    }
    return query.length() == 0 ? "/stock/dividend" : "/stock/dividend?" + query;
  }

  /**
   * '다가올 배당' 구역의 모델. 옛 캘린더 탭 위에 있던 예상치 안내(기준일 · 수량 어긋남 · 원장 조회 실패 · 짧은 이력)는 이제 이 구역의 예정 금액을 설명한다
   * &mdash; 금액이 같은 스냅샷에서 나오므로 같은 안내가 따라온다. 월 · 연 예상 합계 카드는 옮기지 않았다(사용자 결정: 같은 합계가 월배당 시뮬레이터 요약에
   * 있다).
   */
  private void populateUpcomingDividendModel(UUID userId, Model model) {
    List<MonthlyDividendSnapshotResponse> rows =
        monthlyDividendReferenceSupport.loadMonthlyDividendRows(userId);
    if (rows == null) {
      rows = List.of();
    }
    model.addAttribute("upcomingHasRows", !rows.isEmpty());
    if (rows.isEmpty()) {
      return;
    }

    // 서로 기대지 않는 세 조회(원장 수량 · 지급 이력 · 이번 달 받은 배당)를 동시에 던진다. 실측 2026-09-23: 차례로 28 · 34 · 16ms 를
    // 기다려 배당 내역 본문 104ms 중 78ms 였다. 스냅샷은 비었을 때 일찍 끝내려고 먼저 읽는다.
    LocalDate today = LocalDate.now();
    var holdingsFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.loadCurrentHoldings(userId));
    var payoutsFuture = stockAsync.supply(this::loadPayoutsQuietly);
    var receivedFuture =
        stockAsync.supply(() -> loadReceivedStockItemIdsQuietly(userId, YearMonth.from(today)));

    // 스냅샷 수량은 사람이 갱신한 시점의 값이라 원장과 어긋난다(실측 2026-08-23: 8 종목 중 7 종목, 1.66% 낮다).
    // 요약 카드 · 월배당 시뮬레이터와 같은 계산 · 같은 문구를 쓴다.
    var currentHoldings =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(holdingsFuture);
    var quantityBasis =
        net.luversof.web.gate.stock.service.MonthlyDividendCalculator.currentQuantitySummary(
            rows, currentHoldings.quantities());
    model.addAttribute("upcomingStaleQuantityCount", quantityBasis.staleCount());
    // 조회가 실패해도 빈 맵이면 어긋난 줄이 0 이 되어 안내가 사라진다 - 안내가 없는 화면은 "수량이 원장과 같다" 로 읽힌다.
    model.addAttribute("upcomingCurrentQuantityUnavailable", currentHoldings.unavailable());
    model.addAttribute("upcomingCurrentQuantityTotal", quantityBasis.totalAtCurrentQuantity());
    // 스냅샷은 종목마다 시점이 다르다 - 가장 이른 것과 늦은 것을 함께 적는다.
    model.addAttribute(
        "upcomingAsOfOldest",
        rows.stream()
            .map(MonthlyDividendSnapshotResponse::asOfDate)
            .filter(java.util.Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null));
    model.addAttribute(
        "upcomingAsOfNewest",
        rows.stream()
            .map(MonthlyDividendSnapshotResponse::asOfDate)
            .filter(java.util.Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null));

    // "평균" 기준은 최근 12 건의 주당 배당을 평균한다(건수 기준). 이력이 12 건에 못 미치는 종목은 그만큼 짧은 평균이다
    // (실측 2026-08-23: 8 종목 중 2 종목이 10 건). 지급이력은 한 번만 읽어 이 안내와 예정일 추정 두 곳에 쓴다.
    List<MonthlyDividendPayoutResponse> payouts =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(payoutsFuture);
    model.addAttribute("shortHistorySymbols", shortHistoryLabels(rows, payouts));
    model.addAttribute(
        "upcomingSchedule",
        buildUpcomingSchedule(
            userId,
            rows,
            payouts,
            today,
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(receivedFuture)));
  }

  /**
   * 이번 달 남은 날 + 다음 달의 예정 지급.
   *
   * <p>날짜는 그 달 지급 이력이 이미 있으면(운용사가 발표한 지급일) 그 날, 없으면 종목별 최빈 지급일이다({@link
   * MonthlyDividendPayDayUtil}). 이력이 없는 종목은 날짜를 지어내지 않고 따로 적는다. 금액은 월배당 기준 데이터(최근 · 평균 주당 배당 x 지금
   * 스냅샷 수량)다.
   *
   * <p>이번 달에 원장이 이미 받았다고 말하는 종목은 이번 달 예정에서 뺀다 &mdash; 받은 돈은 아래 실수령 배당이 말한다. 원장을 못 읽었으면(null) 아무것도
   * 빼지 않는다. 이번 달 예정일이 지났는데 받은 기록이 없는 종목은 조용히 버리지 않고 {@code overdue} 로 밝힌다.
   *
   * <p>오늘과 두 달은 <b>서버 존</b> 기준이다 &mdash; 이 화면은 타임존 파라미터를 받지 않는다(대시보드의 다가올 배당도 같은 규칙).
   */
  net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView buildUpcomingSchedule(
      UUID userId,
      List<MonthlyDividendSnapshotResponse> rows,
      List<MonthlyDividendPayoutResponse> payouts,
      LocalDate today) {
    return buildUpcomingSchedule(
        userId,
        rows,
        payouts,
        today,
        loadReceivedStockItemIdsQuietly(userId, YearMonth.from(today)));
  }

  /** 이번 달 받은 종목을 미리 읽어 둔 경우(본문이 다른 조회와 동시에 던진다). null 이면 원장을 못 읽은 것이다. */
  net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView buildUpcomingSchedule(
      UUID userId,
      List<MonthlyDividendSnapshotResponse> rows,
      List<MonthlyDividendPayoutResponse> payouts,
      LocalDate today,
      java.util.Set<UUID> receivedThisMonth) {
    List<MonthlyDividendPayDayUtil.Payout> payDayInput = new ArrayList<>();
    for (MonthlyDividendPayoutResponse payout : payouts) {
      if (payout != null) {
        payDayInput.add(
            new MonthlyDividendPayDayUtil.Payout(payout.stockItemSymbol(), payout.payDate()));
      }
    }
    Map<String, MonthlyDividendPayDayUtil.PayDay> payDayBySymbol =
        MonthlyDividendPayDayUtil.byStockItem(payDayInput);
    YearMonth thisMonth = YearMonth.from(today);
    YearMonth nextMonth = thisMonth.plusMonths(1);
    Map<String, Integer> announcedThisMonth =
        MonthlyDividendPayDayUtil.actualDayByStockItem(payDayInput, thisMonth);
    Map<String, Integer> announcedNextMonth =
        MonthlyDividendPayDayUtil.actualDayByStockItem(payDayInput, nextMonth);
    // 저장된 과세표준이 0 인 종목이 비과세인지 미등록인지는 지급이력이 말해 준다.
    Map<String, BigDecimal> historyTaxableRatioBySymbol =
        monthlyDividendReferenceSupport.referenceTaxableRatioBySymbol(payouts);

    java.util.TreeMap<LocalDate, List<DividendCalendarView.Entry>> byDate =
        new java.util.TreeMap<>();
    List<DividendCalendarView.Entry> undated = new ArrayList<>();
    List<DividendCalendarView.Entry> overdue = new ArrayList<>();
    for (MonthlyDividendSnapshotResponse row : rows) {
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(row.stockItemSymbol());
      MonthlyDividendPayDayUtil.PayDay payDay = symbol != null ? payDayBySymbol.get(symbol) : null;
      BigDecimal ratio = symbol != null ? historyTaxableRatioBySymbol.get(symbol) : null;
      if (payDay == null) {
        undated.add(upcomingEntry(row, symbol, ratio, null, 0, false));
        continue;
      }

      boolean received =
          receivedThisMonth != null
              && row.stockItemId() != null
              && receivedThisMonth.contains(row.stockItemId());
      if (!received) {
        Integer announced = symbol != null ? announcedThisMonth.get(symbol) : null;
        LocalDate date =
            thisMonth.atDay(
                MonthlyDividendPayDayUtil.dayInMonth(
                    announced != null ? announced : payDay.day(), thisMonth));
        DividendCalendarView.Entry entry =
            upcomingEntry(row, symbol, ratio, payDay, date.getDayOfMonth(), announced != null);
        if (date.isBefore(today)) {
          overdue.add(entry);
        } else {
          byDate.computeIfAbsent(date, key -> new ArrayList<>()).add(entry);
        }
      }

      Integer announcedNext = symbol != null ? announcedNextMonth.get(symbol) : null;
      LocalDate nextDate =
          nextMonth.atDay(
              MonthlyDividendPayDayUtil.dayInMonth(
                  announcedNext != null ? announcedNext : payDay.day(), nextMonth));
      byDate
          .computeIfAbsent(nextDate, key -> new ArrayList<>())
          .add(
              upcomingEntry(
                  row, symbol, ratio, payDay, nextDate.getDayOfMonth(), announcedNext != null));
    }

    // 같은 날에 여럿이면 금액이 큰 종목부터(달력 칸 · 목록과 같은 순서).
    Comparator<DividendCalendarView.Entry> byLatestDesc =
        Comparator.comparing(
                (DividendCalendarView.Entry entry) ->
                    entry.latest() != null ? entry.latest() : BigDecimal.ZERO)
            .reversed();
    List<DividendCalendarView.Day> days = new ArrayList<>();
    byDate.forEach(
        (date, entries) -> {
          entries.sort(byLatestDesc);
          days.add(new DividendCalendarView.Day(date, true, date.equals(today), entries));
        });
    return new net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView(
        today, days, undated, overdue);
  }

  private static DividendCalendarView.Entry upcomingEntry(
      MonthlyDividendSnapshotResponse row,
      String symbol,
      BigDecimal historyTaxableRatio,
      MonthlyDividendPayDayUtil.PayDay payDay,
      int day,
      boolean announced) {
    return new DividendCalendarView.Entry(
        symbol,
        row.stockItemName(),
        row.expectedMonthlyDividend(),
        latestMonthlyDividend(row),
        row.expectedTaxableBaseAmount(),
        historyTaxableRatio,
        day,
        payDay != null ? payDay.earliest() : 0,
        payDay != null ? payDay.latest() : 0,
        payDay != null ? payDay.sampleCount() : 0,
        announced);
  }

  /**
   * 그 달에 원장이 <b>이미 받았다</b>고 말하는 종목 id. 못 읽으면 {@code null} &mdash; 없는 것(빈 집합)과는 다르다.
   *
   * <p>'다가올 배당' 이 이번 달 예정에서 받은 종목을 빼는 데만 쓴다 &mdash; 받은 금액은 아래 실수령 배당(달력 포함)이 원장 행으로 말한다. 한 종목이 한 달에
   * 여러 줄일 수 있어(실측 2026-09: 계좌 셋에 나뉜 두 종목이 각 3 줄) 줄이 아니라 종목으로 본다.
   *
   * <p>기간은 그 달 전체다. {@code endDate} 는 배타적이라 다음 달 1 일을 준다. 존은 이 화면의 다른 날짜와 같은 서버 존이다.
   */
  private java.util.Set<UUID> loadReceivedStockItemIdsQuietly(UUID userId, YearMonth month) {
    if (userId == null || month == null) {
      return null;
    }
    java.time.ZoneId zone = java.time.ZoneId.systemDefault();
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    params.add("startDate", month.atDay(1).atStartOfDay(zone).toInstant().toString());
    params.add("endDate", month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toString());

    List<net.luversof.web.gate.stock.dto.response.DividendResponse> rows;
    try {
      rows = dividendClient.findDividends(params);
    } catch (RuntimeException ex) {
      // 원장을 못 읽었다고 예정이 못 뜰 이유는 없다 - 아무것도 빼지 않는다(지급이력 읽기와 같은 규칙).
      log.warn("배당 원장을 읽지 못했다 - 이번 달 예정에서 받은 종목을 빼지 않는다", ex);
      return null;
    }
    java.util.Set<UUID> received = new java.util.HashSet<>();
    if (rows != null) {
      for (net.luversof.web.gate.stock.dto.response.DividendResponse row : rows) {
        if (row != null && row.stockItemId() != null && row.payDate() != null) {
          received.add(row.stockItemId());
        }
      }
    }
    return received;
  }

  /** 지급이력. 이걸 못 읽는다고 화면이 못 뜰 이유는 없다. */
  private List<MonthlyDividendPayoutResponse> loadPayoutsQuietly() {
    try {
      List<MonthlyDividendPayoutResponse> payouts =
          monthlyDividendPayoutClient.findPayouts(new LinkedMultiValueMap<>());
      return payouts == null ? List.of() : payouts;
    } catch (RuntimeException ex) {
      log.warn("월배당 지급 이력을 읽지 못했다", ex);
      return List.of();
    }
  }

  /** 이력이 12건에 못 미치는 종목의 "이름(건수)" 목록. 없으면 빈 목록. */
  private List<String> shortHistoryLabels(
      List<MonthlyDividendSnapshotResponse> rows, List<MonthlyDividendPayoutResponse> payouts) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<String, Long> payoutCountBySymbol = new LinkedHashMap<>();
    for (MonthlyDividendPayoutResponse payout : payouts) {
      if (payout == null) {
        continue;
      }
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(payout.stockItemSymbol());
      if (symbol != null) {
        payoutCountBySymbol.merge(symbol, 1L, Long::sum);
      }
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
  /**
   * 고른 종목 시세만 다시 받는다(사용자 요청 2026-09-22).
   *
   * <p>전체 갱신은 53 종목에 18~22 초가 걸려 게이트의 읽기 제한(10 초)에 매번 끊겼다(실측). 한 종목이면 1 초 안쪽이라 그 자리에서 끝난다. 실패하면 까닭을
   * 그대로 화면에 띄운다 &mdash; 원격 사정을 "입력값을 확인하세요" 로 덮지 않는다.
   */
  @PostMapping("/dividend/monthly-reference/profile/refresh-price")
  public String refreshMonthlyDividendProfilePrice(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam String symbol) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    String normalizedSymbol =
        monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(symbol);
    UUID userId = UserUtil.getUserId();
    try {
      monthlyDividendReferenceSupport.validateMonthlyDividendSymbol(normalizedSymbol);
      stockAdminClient.priceHistoryUpdateOne(normalizedSymbol, userId);
      return buildMonthlyDividendReferenceRedirect(
          request, redirectAttributes, normalizedSymbol, "price-refreshed");
    } catch (IllegalArgumentException ex) {
      return renderMonthlyDividendReferenceError(
          request, model, normalizedSymbol, ex.getMessage(), null, null);
    } catch (Exception ex) {
      // 원격 호출 실패는 반드시 남긴다 - 조용히 삼키면 화면만 "안 됨" 이 되고 까닭을 못 찾는다.
      log.warn("종목 시세 갱신 실패: {}", normalizedSymbol, ex);
      return renderMonthlyDividendReferenceError(
          request, model, normalizedSymbol, ex.getMessage(), null, null);
    }
  }

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

      net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser.LenientParseResult
          imported =
              monthlyDividendPayoutSourceImportService.fetchImport(
                  normalizedSymbol, profile.sourceUrl());
      saveMonthlyDividendPayoutRequests(imported.requests());
      // 출처 표의 잘못된 행은 건너뛰었다 - 조용히 삼키지 않고 무엇을 왜 건너뛰었는지 밝힌다(사용자 결정 2026-09-17).
      if (!imported.skipped().isEmpty()) {
        redirectAttributes.addFlashAttribute(
            "monthlyDividendReferenceWarningMessage",
            msg(
                "stock.monthly.reference.import.skipped.rows",
                imported.skipped().size(),
                net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser.joinReasons(
                    imported.skipped())));
      }
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
  /**
   * 운용사 상세 링크만으로 등록한다(사용자 요청 2026-09-21). 여러 줄이면 줄마다 하나씩.
   *
   * <p>되는 줄만 등록하고 실패는 사유와 함께 알린다(사용자 결정). 미리보기 없이 바로 등록하고 결과를 요약한다.
   */
  @PostMapping("/dividend/monthly-reference/profile/import/links")
  public String registerMonthlyDividendProfilesFromLinks(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @RequestParam(required = false) String links) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    try {
      var result = monthlyDividendLinkRegisterService.registerLinks(links);
      String summary =
          MessageFormat.format(
              MessageUtil.getMessage("stock.monthly.reference.link.result.summary"),
              result.successCount(),
              result.newStockItemCount(),
              result.payoutCount(),
              result.failureCount());
      List<String> notes = new ArrayList<>();
      if (result.skippedCount() > 0) {
        notes.add(
            MessageFormat.format(
                MessageUtil.getMessage("stock.monthly.reference.link.result.skipped"),
                result.skippedCount()));
      }
      result.results().stream()
          .filter(one -> !one.succeeded())
          .forEach(
              one ->
                  notes.add(
                      MessageFormat.format(
                          MessageUtil.getMessage("stock.monthly.reference.link.result.failure"),
                          one.sourceUrl(),
                          one.failureReason())));

      redirectAttributes.addFlashAttribute("monthlyDividendReferenceResultMessage", summary);
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceResultIsError", result.successCount() == 0);
      if (!notes.isEmpty()) {
        redirectAttributes.addFlashAttribute(
            "monthlyDividendReferenceWarningMessage", String.join(" / ", notes));
      }
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceErrorMessage",
          StringUtils.hasText(ex.getMessage())
              ? ex.getMessage()
              : MessageUtil.getMessage("stock.monthly.reference.error.link.empty"));
    }

    return buildMonthlyDividendReferenceBulkRedirect(request);
  }

  @BlueskyPreAuthorize
  /** 등록된 월배당 프로필의 총보수 · 상장일을 운용사에서 다시 읽는다(사용자 승인 2026-09-28). 바뀐 것만 저장하고, 못 읽은 값은 지우지 않는다. */
  @PostMapping("/dividend/monthly-reference/profile/facts/refresh")
  public String refreshMonthlyDividendProfileFacts(
      HttpServletRequest request, RedirectAttributes redirectAttributes) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    var result = monthlyDividendLinkRegisterService.refreshFacts();
    redirectAttributes.addFlashAttribute(
        "monthlyDividendReferenceResultMessage",
        MessageFormat.format(
            MessageUtil.getMessage("stock.monthly.reference.facts.refresh.summary"),
            result.updatedCount(),
            result.unchangedCount(),
            result.failures().size()));
    redirectAttributes.addFlashAttribute(
        "monthlyDividendReferenceResultIsError",
        result.updatedCount() == 0 && result.unchangedCount() == 0 && !result.failures().isEmpty());
    if (!result.failures().isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceWarningMessage", String.join(" / ", result.failures()));
    }
    return buildMonthlyDividendReferenceBulkRedirect(request);
  }

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
    // 가져오기는 됐지만 출처 표의 잘못된 행을 건너뛴 종목(종목 + 사유).
    List<String> skippedBySymbol = new ArrayList<>();
    for (MonthlyDividendProfileResponse profile : targets) {
      String symbol =
          monthlyDividendReferenceSupport.normalizeMonthlyDividendSymbol(profile.stockItemSymbol());
      try {
        net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser.LenientParseResult
            imported =
                monthlyDividendPayoutSourceImportService.fetchImport(symbol, profile.sourceUrl());
        saveMonthlyDividendPayoutRequests(imported.requests());
        successCount++;
        if (!imported.skipped().isEmpty()) {
          skippedBySymbol.add(
              symbol
                  + " "
                  + net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser.joinReasons(
                      imported.skipped()));
        }
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
    if (!skippedBySymbol.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "monthlyDividendReferenceWarningMessage",
          msg("stock.monthly.reference.import.skipped.bulk", String.join(" ; ", skippedBySymbol)));
    }

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

  private static BigDecimal latestMonthlyDividend(MonthlyDividendSnapshotResponse row) {
    BigDecimal perShare =
        row.latestMonthlyDividendPerShare() != null
            ? row.latestMonthlyDividendPerShare()
            : BigDecimal.ZERO;
    long quantity = row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L;
    return perShare.multiply(BigDecimal.valueOf(quantity));
  }
}
