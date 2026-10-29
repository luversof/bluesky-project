package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 배당 화면 위 '다가올 배당' &mdash; 이번 달 남은 날 + 다음 달의 예정 지급. 그리고 없어진 캘린더 탭 주소가 어디로 가는지.
 *
 * <p>2026-09-17 캘린더 탭을 실수령 배당에 통합했다(사용자 결정). 지난 달은 실수령 배당이 달력으로 말하고, 캘린더에만 있던 '앞으로' 가 이 구역으로 왔다(범위:
 * 이번 달 남은 날 + 다음 달, 사용자 선택). 월 · 연 예상 합계 카드는 옮기지 않았다(같은 합계가 월배당 시뮬레이터에 있다).
 */
class UpcomingDividendScheduleTest {

  private static final LocalDate TODAY = LocalDate.parse("2026-09-17");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static final UUID MID = UUID.randomUUID();
  private static final UUID EARLY = UUID.randomUUID();
  private static final UUID FRESH = UUID.randomUUID();
  private static final UUID ANNOUNCED = UUID.randomUUID();

  private static MonthlyDividendSnapshotResponse snapshot(
      UUID item, String symbol, long latestPerShare) {
    BigDecimal one = BigDecimal.ONE;
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        null,
        item,
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-09-02"),
        BigDecimal.valueOf(latestPerShare),
        BigDecimal.valueOf(latestPerShare),
        BigDecimal.ZERO,
        100,
        one,
        one,
        one,
        BigDecimal.valueOf(latestPerShare * 100),
        one,
        one,
        one,
        one,
        BigDecimal.ZERO,
        one,
        one,
        null);
  }

  private static List<MonthlyDividendPayoutResponse> payouts(
      String symbol, int day, String... extraPayDates) {
    List<MonthlyDividendPayoutResponse> rows = new ArrayList<>();
    for (int month = 3; month <= 8; month++) {
      LocalDate pay = LocalDate.of(2026, month, day);
      rows.add(
          new MonthlyDividendPayoutResponse(
              UUID.randomUUID(),
              null,
              symbol,
              symbol,
              pay.minusDays(2),
              pay,
              null,
              BigDecimal.TEN,
              BigDecimal.ZERO,
              null));
    }
    for (String extra : extraPayDates) {
      LocalDate pay = LocalDate.parse(extra);
      rows.add(
          new MonthlyDividendPayoutResponse(
              UUID.randomUUID(),
              null,
              symbol,
              symbol,
              pay.minusDays(2),
              pay,
              null,
              BigDecimal.TEN,
              BigDecimal.ZERO,
              null));
    }
    return rows;
  }

  private static DividendResponse received(UUID item, String payDate) {
    Instant pay = LocalDate.parse(payDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    return new DividendResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        item,
        "name",
        "DIVIDEND",
        1,
        BigDecimal.ONE,
        null,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.TEN,
        pay,
        pay);
  }

  private static StockDividendViewController controller(DividendClient dividendClient) {
    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setMonthlyDividendCalculator(new MonthlyDividendCalculator());
    StockDividendViewController created = new StockDividendViewController();
    created.setMonthlyDividendReferenceSupport(support);
    org.springframework.test.util.ReflectionTestUtils.setField(
        created, "dividendClient", dividendClient);
    return created;
  }

  private static UpcomingDividendScheduleView schedule(DividendClient dividendClient) {
    List<MonthlyDividendPayoutResponse> all = new ArrayList<>();
    all.addAll(payouts("MID1", 17));
    all.addAll(payouts("EARLY1", 2));
    all.addAll(payouts("ANN1", 17, "2026-09-19"));
    return controller(dividendClient)
        .buildUpcomingSchedule(
            UUID.randomUUID(),
            List.of(
                snapshot(MID, "MID1", 300),
                snapshot(EARLY, "EARLY1", 50),
                snapshot(FRESH, "NEW1", 70),
                snapshot(ANNOUNCED, "ANN1", 120)),
            all,
            TODAY);
  }

  private static DividendClient ledger(List<DividendResponse> rows) {
    DividendClient client = org.mockito.Mockito.mock(DividendClient.class);
    org.mockito.Mockito.when(client.findDividends(org.mockito.ArgumentMatchers.any()))
        .thenReturn(rows);
    return client;
  }

  @Test
  void 이번_달_남은_날과_다음_달을_날짜순으로_적는다() {
    UpcomingDividendScheduleView view = schedule(ledger(List.of(received(EARLY, "2026-09-02"))));

    assertThat(view.days())
        .extracting(DividendCalendarView.Day::date)
        .containsExactly(
            LocalDate.parse("2026-09-17"),
            LocalDate.parse("2026-09-19"),
            LocalDate.parse("2026-10-02"),
            // 다음 달 최빈일 17 일은 토요일이라 그 달의 영업일 10/19(월)(2026-10-14 영업일 보정).
            LocalDate.parse("2026-10-19"));
    assertThat(view.days().get(0).today()).as("오늘인 날을 표시한다").isTrue();
    assertThat(view.days().get(1).entries().get(0).actualPayDate())
        .as("이번 달 발표된 지급일(19 일)이 있으면 최빈일(17 일) 대신 그 날이고, 확정이라고 적는다")
        .isTrue();
    assertThat(view.days().get(3).entries())
        .extracting(DividendCalendarView.Entry::symbol)
        .containsExactly("MID1", "ANN1");
    assertThat(view.undated())
        .extracting(DividendCalendarView.Entry::symbol)
        .containsExactly("NEW1");
    assertThat(view.overdue()).as("이번 달 2 일 몫은 원장이 받았다고 말한다").isEmpty();
    assertThat(view.thisMonthTotal()).isEqualByComparingTo(String.valueOf(300 * 100 + 120 * 100));
    assertThat(view.nextMonthTotal())
        .isEqualByComparingTo(String.valueOf(50 * 100 + 300 * 100 + 120 * 100));
  }

  @Test
  void 예정일이_지났는데_받은_기록이_없으면_따로_밝힌다() {
    UpcomingDividendScheduleView view = schedule(ledger(List.of()));
    assertThat(view.overdue())
        .extracting(DividendCalendarView.Entry::symbol)
        .containsExactly("EARLY1");
    assertThat(view.overdue().get(0).payDay()).isEqualTo(2);

    DividendClient failing = org.mockito.Mockito.mock(DividendClient.class);
    org.mockito.Mockito.when(failing.findDividends(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("api-stock down"));
    assertThat(schedule(failing).overdue())
        .as("원장을 못 읽었으면 받았는지 모른다 - 아무것도 빼지 않는다")
        .extracting(DividendCalendarView.Entry::symbol)
        .containsExactly("EARLY1");
  }

  private static String renderSection(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/upcomingDividendSchedule.jte", model, output);
    return output.toString();
  }

  @Test
  void 구역이_예정과_예상치_안내를_함께_보인다() {
    UpcomingDividendScheduleView view = schedule(ledger(List.of()));
    Map<String, Object> model = new HashMap<>();
    model.put("hasRows", true);
    model.put("schedule", view);
    model.put("asOfOldest", LocalDate.parse("2026-09-02"));
    model.put("asOfNewest", LocalDate.parse("2026-09-17"));
    model.put("staleQuantityCount", 7L);
    model.put("currentQuantityTotal", new BigDecimal("1234567"));
    model.put("currentQuantityUnavailable", true);
    model.put("shortHistorySymbols", List.of("SHORT(11)"));
    String html = renderSection(model);

    // 늦는 지급 · 날짜 모름 상자도 이름과 금액 사이 점선 길잡이(2026-10-28 12차 자율, 날짜 카드와 같은 span).
    int overdue = html.indexOf("data-upcoming-overdue");
    int undated = html.indexOf("data-upcoming-undated");
    assertThat(html.substring(overdue, undated)).contains("data-calendar-entry-leader");
    assertThat(html.substring(undated)).contains("data-calendar-entry-leader");

    assertThat(html)
        .contains("data-upcoming-daylist=\"4\"")
        .contains("data-daycard-month")
        .contains("data-upcoming-overdue=\"1\"")
        .contains("data-upcoming-undated=\"1\"")
        .contains("data-upcoming-stale-quantity=\"7\"")
        .contains("data-ledger-unavailable")
        .contains("data-short-history-count=\"1\"")
        .contains(
            "<span class=\"whitespace-nowrap date-whole\">2026-09-02</span> ~ <span class=\"whitespace-nowrap date-whole\">2026-09-17</span>")
        .contains(StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.thisMonthTotal())))
        .contains(MessageUtil.getMessage("stock.dividend.upcoming.desc"));
  }

  @Test
  void 기준_데이터가_없으면_등록하러_가는_길을_준다() {
    Map<String, Object> model = new HashMap<>();
    model.put("hasRows", false);
    String html = renderSection(model);
    assertThat(html)
        .contains("/stock/admin?tab=monthly-reference")
        .doesNotContain("data-upcoming-daylist");
  }

  @Test
  void 옛_캘린더_주소는_그_달_기간의_실수령_배당으로_간다() {
    ZoneId kst = ZoneId.of("Asia/Seoul");
    assertThat(
            StockDividendViewController.calendarTabRedirect(
                "tab=calendar&month=2026-08&accountIdList=A&rangeMode=3", "2026-08", TODAY, kst))
        .as("지난 달: 1 일 ~ 다음 달 1 일(배타), 들고 온 기간 키는 버린다(세 키가 한 묶음)")
        .isEqualTo(
            "/stock/dividend?accountIdList=A&startDate=2026-07-31T15:00:00Z&endDate=2026-08-31T15:00:00Z&rangeMode=1");
    assertThat(
            StockDividendViewController.calendarTabRedirect(
                "tab=calendar&month=2026-09", "2026-09", TODAY, kst))
        .isEqualTo("/stock/dividend?rangeMode=mtd");
    assertThat(
            StockDividendViewController.calendarTabRedirect(
                "tab=calendar&month=2026-11&rangeMode=3", "2026-11", TODAY, kst))
        .as("앞으로 올 달은 실수령 배당에 보일 것이 없다 - 기간을 바꾸지 않는다")
        .isEqualTo("/stock/dividend?rangeMode=3");
    assertThat(
            StockDividendViewController.calendarTabRedirect(
                "tab=calendar&month=bogus", "bogus", TODAY, kst))
        .isEqualTo("/stock/dividend");
    assertThat(
            StockDividendViewController.calendarTabRedirect(
                "tab=calendar&rangeMode=all&locale=en", null, TODAY, kst))
        .as("달을 안 적은 옛 주소는 이번 달 캘린더였다(옛 탭의 기본값 - 대시보드 카드가 이 주소로 열었다)")
        .isEqualTo("/stock/dividend?locale=en&rangeMode=mtd");
    assertThat(StockDividendViewController.calendarTabRedirect(null, null, TODAY, kst))
        .isEqualTo("/stock/dividend?rangeMode=mtd");
  }

  private static String flatten(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
  }

  /** 페이지 · 실수령 배당 · 대시보드 카드가 새 배치를 쓰는지. 템플릿만 있고 배선이 없으면 아무것도 안 보인다. */
  @Test
  void 화면들이_통합된_배치를_쓴다() throws IOException {
    String page = flatten("src/main/jte/stock/dividend.jte");
    int upcoming = page.indexOf("@template.stock.fragments.upcomingDividendSchedule(");
    int history = page.indexOf("hx-get=\"/stock/htmx/dividend/list\"");
    assertThat(upcoming).as("다가올 배당 구역").isGreaterThan(0);
    assertThat(history).as("실수령 배당은 그 아래").isGreaterThan(upcoming);
    assertThat(page).doesNotContain("role=\"tablist\"");

    String controller =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java");
    assertThat(controller)
        .contains(
            "if (\"calendar\".equals(dividendTab)) { return \"redirect:\" + calendarTabRedirect(")
        .contains("populateUpcomingDividendModel(UserUtil.getUserId(), model);");

    String historyTemplate = flatten("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte");
    assertThat(historyTemplate)
        .contains(
            "@if(dividendMonthCalendar != null) @template.stock.fragments.dividendCalendarMonth( calendar = dividendMonthCalendar,");
    String historyController =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java");
    assertThat(historyController)
        .as("달력은 필터를 거친 목록(viewList)으로 만든다")
        .contains(
            "DividendMonthCalendarUtil.calendarMonth( startInstant, endInstant, zone, calendarToday)")
        .contains("DividendMonthCalendarUtil.build( viewList, calendarMonth, calendarToday, zone)");

    String dashboardCard = flatten("src/main/jte/stock/htmx/fragments/upcomingDividends.jte");
    assertThat(dashboardCard).contains("href=\"/stock/dividend\"").doesNotContain("tab=calendar");
  }

  /**
   * 배당 내역 본문은 서로 기대지 않는 세 조회를 동시에 던진다(원장 수량 · 지급 이력 · 이번 달 받은 배당).
   *
   * <p>실측 2026-09-23: 차례로 28 · 34 · 16ms 를 기다려 본문 104ms 중 78ms 였다. 하나라도 다시 순서대로 부르면 그만큼 느려진다. 소스는
   * 공백을 전부 지우고 본다(서식 정리로 줄이 접힌다).
   */
  @Test
  void 본문의_세_조회를_동시에_던진다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
                java.nio.file.Path.of(
                    "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java"),
                java.nio.charset.StandardCharsets.UTF_8)
            .replaceAll("\\s+", "");
    int start = source.indexOf("privatevoidpopulateUpcomingDividendModel(");
    int end = source.indexOf("UpcomingDividendScheduleViewbuildUpcomingSchedule(", start);
    String body = source.substring(start, end);

    int holdings =
        body.indexOf(
            "stockAsync.supply(()->monthlyDividendReferenceSupport.loadCurrentHoldings(userId))");
    int payouts = body.indexOf("stockAsync.supply(this::loadPayoutsQuietly)");
    int received =
        body.indexOf(
            "stockAsync.supply(()->loadReceivedStockItemIdsQuietly(userId,YearMonth.from(today)))");
    int firstJoin = body.indexOf("StockAsyncSupport.join(");
    assertThat(holdings).as("원장 수량을 동시에").isGreaterThan(0);
    assertThat(payouts).as("지급 이력을 동시에").isGreaterThan(0);
    assertThat(received).as("이번 달 받은 배당을 동시에").isGreaterThan(0);
    assertThat(firstJoin)
        .as("셋을 다 던진 뒤에 기다린다 - 하나를 기다리고 다음을 던지면 순서대로와 같다")
        .isGreaterThan(Math.max(holdings, Math.max(payouts, received)));
    assertThat(body)
        .as("미리 읽은 받은 배당을 넘긴다 - 안 넘기면 안에서 또 읽는다")
        .contains("StockAsyncSupport.join(receivedFuture)")
        .doesNotContain("=loadPayoutsQuietly();")
        .doesNotContain("monthlyDividendReferenceSupport.loadCurrentHoldings(userId);");
  }

  /** 최근과 평균이 다른 기준 데이터(최근 latestPerShare, 평균 averagePerShare, 100 주). */
  private static MonthlyDividendSnapshotResponse snapshot(
      UUID item, String symbol, long latestPerShare, long averagePerShare) {
    BigDecimal one = BigDecimal.ONE;
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        null,
        item,
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-09-02"),
        BigDecimal.valueOf(latestPerShare),
        BigDecimal.valueOf(averagePerShare),
        BigDecimal.ZERO,
        100,
        one,
        one,
        one,
        BigDecimal.valueOf(averagePerShare * 100),
        one,
        one,
        one,
        one,
        BigDecimal.ZERO,
        one,
        one,
        null);
  }

  /**
   * 같은 날 합계는 평균과 최근을 함께 낸다(2026-10-13 사용자 지적). 이 칸 합계가 최근만 더해 대시보드의 다가올 배당(평균)과 같은 날 금액이 달랐다 - 10월
   * 17일분 1,601,445 vs 997,459(한 종목이 9월만 주당 60원, 평소 29원). 최근을 앞에 두고 종목도 최근이 큰 순서로 적는다(사용자 선택 "최근을
   * 우선으로" - 처음엔 평균을 앞에 뒀다).
   */
  @Test
  void 같은_날_합계는_최근과_평균을_함께_내고_최근_순으로_적는다() {
    UUID spike = UUID.randomUUID();
    UUID steady = UUID.randomUUID();
    List<MonthlyDividendPayoutResponse> all = new ArrayList<>();
    all.addAll(payouts("SPIKE1", 17));
    all.addAll(payouts("STEADY1", 17));
    UpcomingDividendScheduleView view =
        controller(ledger(List.of()))
            .buildUpcomingSchedule(
                UUID.randomUUID(),
                List.of(snapshot(spike, "SPIKE1", 60, 30), snapshot(steady, "STEADY1", 10, 50)),
                all,
                TODAY);

    DividendCalendarView.Day day = view.days().get(0);
    assertThat(day.date()).isEqualTo(LocalDate.parse("2026-09-17"));
    assertThat(day.averageTotal()).isEqualByComparingTo("8000");
    assertThat(day.latestTotal()).isEqualByComparingTo("7000");
    // 평균 순이면 STEADY1(5,000) 이 먼저지만 최근 순이라 SPIKE1(6,000) 이 먼저 - 대시보드 목록과 같은 순서.
    assertThat(day.entries())
        .extracting(DividendCalendarView.Entry::symbol)
        .containsExactly("SPIKE1", "STEADY1");
    assertThat(view.thisMonthAverageTotal()).isEqualByComparingTo("8000");
    assertThat(view.thisMonthTotal()).isEqualByComparingTo("7000");

    Map<String, Object> model = new HashMap<>();
    model.put("hasRows", true);
    model.put("schedule", view);
    String html = renderSection(model);
    String average = StockFormatUtil.fullKrw(8000L);
    String latest = StockFormatUtil.fullKrw(7000L);
    assertThat(html).contains("data-daycard-average-total").contains("data-daycard-latest-total");
    // 최근이 앞에 온다.
    assertThat(html.indexOf(latest)).isGreaterThan(-1).isLessThan(html.indexOf(average));
    assertThat(html)
        .contains("data-upcoming-this-month-total>" + latest)
        .contains("data-upcoming-this-month-average-total>" + average);
  }

  /** 수량 어긋남 안내도 최근 · 평균을 함께, 최근이 앞(2026-10-13 - 카드와 같은 순서 · 같은 두 기준). */
  @Test
  void 수량_어긋남_안내도_최근과_평균을_함께_적는다() {
    UpcomingDividendScheduleView view = schedule(ledger(List.of()));
    Map<String, Object> model = new HashMap<>();
    model.put("hasRows", true);
    model.put("schedule", view);
    model.put("staleQuantityCount", 2L);
    model.put("currentQuantityTotal", new BigDecimal("4600"));
    model.put("currentQuantityLatestTotal", new BigDecimal("7700"));
    String html = renderSection(model);

    String expected =
        java.text.MessageFormat.format(
            MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity.both"),
            "2",
            StockFormatUtil.fullKrw(7700L),
            StockFormatUtil.fullKrw(4600L));
    assertThat(expected).contains("7,700").contains("4,600");
    assertThat(expected.indexOf("7,700")).isLessThan(expected.indexOf("4,600"));
    assertThat(html).contains("data-upcoming-stale-quantity=\"2\"").contains(expected);
  }
}
