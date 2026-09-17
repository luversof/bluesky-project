package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;

/**
 * 실수령 배당의 '달력 보기' &mdash; 기간이 달력 한 달이면 그 달의 (필터된) 실제 지급을 받은 날에 놓는다.
 *
 * <p>2026-09-17 캘린더 탭을 실수령 배당에 통합했다(사용자 결정). 옛 탭에서 지나간 달이 지키던 약속은 여기로 옮겨 왔다 &mdash; 금액은 원장 값(세전 ·
 * 실수령 · 과세표준, 실측 2026-09-15: 예상치는 실제보다 +27.8%), 월배당 기준 데이터가 없는 종목도 받은 날에 놓는다(실측: 2026-08 삼성전자
 * 1,886,082 원 = 그 달의 37.9%), 한 건도 없는 지난 달은 "받은 게 없다" 고 말한다(실측: 그런 달이 41 개).
 */
class DividendMonthCalendarUtilTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

  private static Instant kst(String date) {
    return LocalDate.parse(date).atStartOfDay(KST).toInstant();
  }

  @Test
  void 달력_한_달인_기간만_달로_본다() {
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-08-01"), kst("2026-09-01"), KST, TODAY))
        .isEqualTo(YearMonth.of(2026, 8));
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-09-01"), kst("2026-09-18"), KST, TODAY))
        .as("이번달 프리셋(1 일 ~ 오늘)도 그 달이다")
        .isEqualTo(YearMonth.of(2026, 9));

    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-09-01"), kst("2026-09-15"), KST, TODAY))
        .as("이번 달이라도 오늘 전에 끝나면 한 달이 아니다")
        .isNull();
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-07-01"), kst("2026-09-01"), KST, TODAY))
        .isNull();
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-08-02"), kst("2026-09-01"), KST, TODAY))
        .isNull();
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                kst("2026-08-01").plusSeconds(60), kst("2026-09-01"), KST, TODAY))
        .isNull();
    assertThat(
            DividendMonthCalendarUtil.calendarMonth(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                KST,
                TODAY))
        .as("UTC 자정은 KST 로 9 시다 - 존을 섞으면 한 달이 아니다")
        .isNull();
    assertThat(DividendMonthCalendarUtil.calendarMonth(null, kst("2026-09-01"), KST, TODAY))
        .isNull();
  }

  private static final UUID RISE = UUID.randomUUID();

  private static final UUID SAMSUNG = UUID.randomUUID();

  private static DividendView row(
      UUID item, String name, String payDate, long gross, long net, long taxable) {
    return new DividendView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "account",
        item,
        name,
        10,
        BigDecimal.ONE,
        BigDecimal.valueOf(gross),
        BigDecimal.valueOf(gross - net),
        BigDecimal.valueOf(taxable),
        null,
        BigDecimal.valueOf(net),
        null,
        kst(payDate),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static List<DividendCalendarView.Entry> entries(DividendCalendarView view) {
    List<DividendCalendarView.Entry> all = new ArrayList<>();
    for (DividendCalendarView.Day day : view.payDays()) {
      all.addAll(day.entries());
    }
    return all;
  }

  @Test
  void 받은_날마다_원장_값으로_놓고_기준_데이터_없는_종목도_놓는다() {
    DividendCalendarView view =
        DividendMonthCalendarUtil.build(
            List.of(
                row(RISE, "RISE 200", "2026-08-04", 1341850, 1337470, 28550),
                row(RISE, "RISE 200", "2026-08-19", 100, 90, 10),
                row(SAMSUNG, "SAMSUNG", "2026-08-28", 1886082, 1595632, 1886082),
                row(SAMSUNG, "SAMSUNG", "2026-07-28", 999, 999, 999)),
            YearMonth.of(2026, 8),
            TODAY,
            KST);

    assertThat(view.payDays()).extracting(d -> d.date().getDayOfMonth()).containsExactly(4, 19, 28);
    DividendCalendarView.Entry samsung = view.payDays().get(2).entries().get(0);
    assertThat(samsung.name()).isEqualTo("SAMSUNG");
    assertThat(samsung.latest()).as("큰 숫자는 세전").isEqualByComparingTo("1886082");
    assertThat(samsung.average()).as("둘째 줄은 실수령").isEqualByComparingTo("1595632");
    assertThat(samsung.taxable()).isEqualByComparingTo("1886082");
    assertThat(entries(view))
        .allMatch(DividendCalendarView.Entry::actualAmount)
        .allMatch(DividendCalendarView.Entry::actualPayDate);
    assertThat(entries(view)).as("다른 달 행은 넣지 않는다").hasSize(3);
    assertThat(view.actualMonthTotal())
        .isEqualByComparingTo(String.valueOf(1341850 + 100 + 1886082));
    assertThat(view.isSettledEmpty()).isFalse();
  }

  @Test
  void 받은_게_없는_지난_달은_그렇다고_말하고_이번_달은_그리지_않는다() {
    DividendCalendarView past =
        DividendMonthCalendarUtil.build(List.of(), YearMonth.of(2026, 6), TODAY, KST);
    assertThat(past).isNotNull();
    assertThat(past.isSettledEmpty()).isTrue();

    assertThat(DividendMonthCalendarUtil.build(List.of(), YearMonth.of(2026, 9), TODAY, KST))
        .as("이번 달 예정은 위 '다가올 배당' 이 말한다 - 빈 격자는 고장처럼 보인다")
        .isNull();

    DividendCalendarView current =
        DividendMonthCalendarUtil.build(
            List.of(row(RISE, "RISE 200", "2026-09-02", 1377840, 1372540, 34446)),
            YearMonth.of(2026, 9),
            TODAY,
            KST);
    assertThat(current).isNotNull();
    assertThat(current.isSettledEmpty()).isFalse();
  }

  @Test
  void 달력은_실제_지급이라고_말하고_합계는_행을_더한_값이다() {
    DividendCalendarView view =
        DividendMonthCalendarUtil.build(
            List.of(
                row(RISE, "RISE 200", "2026-08-04", 1341850, 1337470, 28550),
                row(SAMSUNG, "SAMSUNG", "2026-08-28", 1886082, 1595632, 1886082)),
            YearMonth.of(2026, 8),
            TODAY,
            KST);
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", view);
    model.put("actualGrossLabel", "GROSS");
    model.put("actualNetLabel", "NET");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/dividendCalendarMonth.jte", model, output);
    String html = output.toString();

    assertThat(html).contains(MessageUtil.getMessage("stock.dividend.calendar.actual"));
    assertThat(html).contains("data-calendar-actual-month-total").contains("3,227,932");
    assertThat(html)
        .as("달 이동은 기간 선택기가 한다")
        .doesNotContain("data-calendar-prev")
        .doesNotContain("data-calendar-next");
  }
}
