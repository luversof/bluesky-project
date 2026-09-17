package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil;

/**
 * 지나간 달은 지급일을 <b>추정하지 않는다</b>.
 *
 * <p>달력은 지급이력의 <b>최빈일</b>에 종목을 놓는다. 앞으로 올 달에는 그것 말고 방법이 없지만, 이미 지나간 달은 그 달의 지급이력이 곧 답이다.
 *
 * <p>실측 2026-09-15(사용자 지적): 2026-08 달력이 8 종목 전부를 최빈일 2 일·17 일에 놓았는데 실제 지급은 4 일·19 일이었다. 하필 2 일은
 * <b>일요일</b>, 17 일은 <b>대체공휴일(광복절)</b> &mdash; 지급이 있을 수 없는 날에 배당이 찍혀 있었다. 2026-07 도 4 종목이 17 일(실제 20
 * 일)로 어긋났다. 고친 뒤 두 달 모두 실제 지급일과 일치한다.
 *
 * <p>날짜가 확정이면 "날짜는 추정" 안내도 거짓이 된다 &mdash; 그것도 함께 지킨다.
 */
class DividendCalendarActualPayDayTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  private static final YearMonth AUGUST = YearMonth.of(2026, 8);

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

  private static MonthlyDividendPayDayUtil.Payout payout(String symbol, String date) {
    return new MonthlyDividendPayDayUtil.Payout(symbol, LocalDate.parse(date));
  }

  /** 그 달 이력이 있으면 그 날이 답이다. 다른 달 이력은 섞이지 않는다. */
  @Test
  void 그_달의_실제_지급일을_돌려준다() {
    List<MonthlyDividendPayDayUtil.Payout> payouts =
        List.of(
            payout("AAA", "2026-08-04"),
            payout("AAA", "2026-07-02"),
            payout("BBB", "2026-08-19"),
            payout("CCC", "2026-07-20"));

    Map<String, Integer> august = MonthlyDividendPayDayUtil.actualDayByStockItem(payouts, AUGUST);

    assertThat(august).containsEntry("AAA", 4).containsEntry("BBB", 19);
    assertThat(august).as("7 월 지급이 8 월 지도에 섞이면 안 된다").doesNotContainKey("CCC");
  }

  /** 한 달에 두 번 지급된 종목은 가장 늦은 날에 놓는다(칸 하나에 한 번만). */
  @Test
  void 한_달에_두_번이면_늦은_날을_쓴다() {
    Map<String, Integer> august =
        MonthlyDividendPayDayUtil.actualDayByStockItem(
            List.of(payout("AAA", "2026-08-04"), payout("AAA", "2026-08-19")), AUGUST);

    assertThat(august).containsEntry("AAA", 19);
  }

  @Test
  void 이력이_없으면_비어_있다() {
    assertThat(MonthlyDividendPayDayUtil.actualDayByStockItem(List.of(), AUGUST)).isEmpty();
    assertThat(MonthlyDividendPayDayUtil.actualDayByStockItem(null, AUGUST)).isEmpty();
  }

  private static DividendCalendarView.Entry entry(String name, int day, boolean actual) {
    return new DividendCalendarView.Entry(
        name,
        name,
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        BigDecimal.ZERO,
        null,
        day,
        2,
        19,
        12,
        actual);
  }

  private static String render(YearMonth month, List<DividendCalendarView.Entry> entries) {
    DividendCalendarView calendar =
        new DividendCalendarView(
            month,
            DividendCalendarGridUtil.build(
                month, null, DividendCalendarGridUtil.groupByDay(entries, month)),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=2026-07");
    model.put("nextHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", false);
    model.put("avgLabel", "평균");
    model.put("latestLabel", "최근");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  /** 확정된 달에 "날짜는 추정" 이라고 적으면 화면이 거짓말을 한다. */
  @Test
  void 확정된_달은_추정이라고_말하지_않는다() {
    String html = render(AUGUST, List.of(entry("가", 4, true), entry("나", 19, true)));

    assertThat(html).contains("data-estimated=\"false\"");
    assertThat(html).contains("data-calendar-day=\"2026-08-04\"");
    assertThat(html).contains("data-calendar-day=\"2026-08-19\"");
    // 폭 안내("실제 지급은 2~19 일에 있었습니다")는 추정일 때만 할 말이다.
    assertThat(html).as("확정인데 지급일 폭을 말하면 추정처럼 읽힌다").doesNotContain("2~19");
  }

  /** 추정이 하나라도 있으면 추정 안내를 유지한다. */
  @Test
  void 추정이_섞이면_추정이라고_말한다() {
    String html = render(AUGUST, List.of(entry("가", 4, true), entry("나", 17, false)));

    assertThat(html).contains("data-estimated=\"true\"");
    assertThat(html).as("추정인 종목은 지급일 폭을 함께 적는다").contains("2~19");
  }

  /**
   * 컨트롤러가 그 값을 <b>실제로 쓰는가</b>.
   *
   * <p>유틸과 렌더만 지키면 정작 결함이 있던 자리를 안 본다 &mdash; 실제로 변이 실험에서 컨트롤러를 옛 코드로 되돌렸는데 위 검사들이 전부 통과했다. 배선은 여기서
   * 지킨다.
   */
  @Test
  void 컨트롤러가_실제_지급일을_먼저_쓴다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller"
                    + "/StockDividendViewController.java"),
            StandardCharsets.UTF_8);

    assertThat(source)
        .as("그 달 실제 지급일 지도를 안 만들면 지나간 달이 다시 추정된다")
        .contains("MonthlyDividendPayDayUtil.actualDayByStockItem(");
    assertThat(source)
        .as("실제 지급일이 있으면 그것을 먼저 써야 한다")
        .contains("announced != null ? announced : payDay.day()");
    assertThat(source)
        .as("확정 여부를 Entry 에 실어야 화면이 '추정' 이라고 거짓말하지 않는다")
        .contains(
            "upcomingEntry(row, symbol, ratio, payDay, date.getDayOfMonth(), announced != null)");
  }

  /**
   * 확정된 달 안내가 <b>금액은 아직 예상치</b>라는 말을 빠뜨리지 않는가.
   *
   * <p>날짜만 실제로 바꾸고 금액은 그대로 두었더니 '실제 날짜 + 지어낸 금액' 이 되어 오히려 더 그럴듯해졌다 &mdash; 실측 2026-09-15: 2026-08
   * 달력 합계 3,956,660 원 대 실제 수령 3,096,324 원(+27.8%), 종목별 최대 +132.5%. 게다가 그 달 배당의 37.9% 인 삼성전자는 월배당
   * 종목이 아니라 달력에 아예 없다.
   *
   * <p>금액까지 원장 기준으로 바꿀지는 화면의 뜻을 바꾸는 결정이라 사용자 판단으로 남겨 두고, 그때까지 화면이 <b>스스로 한계를 밝히게</b> 한다.
   */
  @Test
  void 확정된_달_안내가_금액_한계를_밝힌다() throws IOException {
    java.util.Properties ko = new java.util.Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8)) {
      ko.load(reader);
    }
    java.util.Properties en = new java.util.Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8)) {
      en.load(reader);
    }

    String settledKo = ko.getProperty("stock.dividend.calendar.settled");
    String settledEn = en.getProperty("stock.dividend.calendar.settled");
    assertThat(settledKo).as("확정 안내 문구가 없다").isNotBlank();
    assertThat(settledEn).isNotBlank();

    assertThat(settledKo).as("금액이 예상치라는 말이 빠지면 화면이 실적표처럼 읽힌다").contains("금액").contains("예상");
    assertThat(settledEn.toLowerCase(java.util.Locale.ROOT)).contains("amount").contains("project");
  }
}
