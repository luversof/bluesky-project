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
            LocalDate.parse("2026-10-17"));
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

    assertThat(html)
        .contains("data-upcoming-daylist=\"4\"")
        .contains("data-daycard-month")
        .contains("data-upcoming-overdue=\"1\"")
        .contains("data-upcoming-undated=\"1\"")
        .contains("data-upcoming-stale-quantity=\"7\"")
        .contains("data-ledger-unavailable")
        .contains("data-short-history-count=\"1\"")
        .contains("2026-09-02 ~ 2026-09-17")
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
}
