package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

/**
 * 배당 캘린더의 과세표준 0.
 *
 * <p>2026-09-14 까지 과세표준이 0 이면 아예 적지 않았다. 그런데 안 적는 것과 0 만 적는 것은 둘 다 읽는 사람을 속인다 &mdash; 안 적으면 "이 종목엔
 * 과표가 없다" 를 알 길이 없고, 0 만 적으면 "세금이 없다(비과세)" 로 읽힌다.
 *
 * <p>실측 2026-09-14: 저장값이 0 인 2 종목(PLUS 고배당주위클리고정커버드콜 · TIGER 코리아배당다우존스위클리커버드콜)은 지급이력 기준으로는 각각 25.1%
 * · 31.3% 가 과세였다. 그 0 은 비과세가 아니라 <b>기준 데이터 미등록</b>이다. 그래서 0 은 적되, 이력이 반박하면 그 사실을 함께 적는다.
 */
class DividendCalendarZeroTaxableTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  private static final YearMonth MONTH = YearMonth.of(2026, 9);

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    // 금액 표기가 로케일마다 다르다("0원" vs "KRW 0") - 검사가 머신 기본값에 끌려다니면 안 된다.
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  /** 저장 과세표준 {@code taxable}, 지급이력 비중 {@code historyPct}(없으면 null). */
  private static DividendCalendarView.Entry entry(String name, String taxable, String historyPct) {
    return new DividendCalendarView.Entry(
        name,
        name,
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        new BigDecimal(taxable),
        historyPct == null ? null : new BigDecimal(historyPct),
        17,
        17,
        17,
        12,
        false);
  }

  private static String render(List<DividendCalendarView.Entry> entries) {
    DividendCalendarView calendar =
        new DividendCalendarView(
            MONTH,
            DividendCalendarGridUtil.build(
                MONTH,
                LocalDate.parse("2026-09-14"),
                DividendCalendarGridUtil.groupByDay(entries, MONTH)),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=2026-08");
    model.put("nextHref", "/stock/dividend?tab=calendar&month=2026-10");
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", true);
    model.put("avgLabel", "평균");
    model.put("latestLabel", "최근");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  private static int count(String haystack, String needle) {
    int total = 0;
    int at = haystack.indexOf(needle);
    while (at >= 0) {
      total++;
      at = haystack.indexOf(needle, at + needle.length());
    }
    return total;
  }

  /** 과표 줄은 종목 수만큼 나온다 - 0 이라고 빠지면 이 검사가 깨진다. */
  @Test
  void 과표가_0_이어도_모든_종목에_적는다() {
    String html =
        render(
            List.of(
                entry("과세있음", "5000", null), entry("미등록", "0", "30.10"), entry("비과세", "0", null)));

    assertThat(count(html, "data-calendar-entry=")).isEqualTo(3);
    assertThat(count(html, "data-calendar-taxable"))
        .as("과표 0 을 빼면 사용자는 그 종목에 과표가 없다는 것을 알 길이 없다")
        .isEqualTo(3);
    assertThat(count(html, "0원")).isGreaterThanOrEqualTo(2);
  }

  /** 저장 0 + 이력 과세 = 미등록. 0 만 적고 말면 없는 비과세를 알리는 셈이다. */
  @Test
  void 이력이_반박하는_0_은_미등록으로_표시한다() {
    String html =
        render(
            List.of(
                entry("과세있음", "5000", null), entry("미등록", "0", "30.10"), entry("비과세", "0", null)));

    assertThat(count(html, "data-taxable-unknown=\"true\""))
        .as("저장 0 이고 이력이 과세를 말하는 것은 한 종목뿐이다")
        .isEqualTo(1);
    assertThat(count(html, "data-taxable-unknown=\"false\"")).isEqualTo(2);
    // 근거를 title 에만 달면 마우스로만 닿는다(기록: dash-reason-must-reach-at).
    assertThat(count(html, "30.1%")).as("title 과 sr-only 둘 다 있어야 한다").isEqualTo(2);
  }

  /** 이력이 없거나 이력도 0 이면 그냥 0 이다 - 있지도 않은 경고를 달면 안 된다. */
  @Test
  void 근거가_없으면_경고하지_않는다() {
    String html = render(List.of(entry("이력없음", "0", null), entry("이력도0", "0", "0.00")));

    assertThat(count(html, "data-taxable-unknown=\"true\"")).isZero();
    assertThat(count(html, "data-calendar-taxable")).isEqualTo(2);
    assertThat(html).doesNotContain("text-warning");
  }

  /** 과표가 있으면 이력이 뭐라 하든 평범하게 적는다 - 미등록이 아니다. */
  @Test
  void 과표가_있으면_평범하게_적는다() {
    String html = render(List.of(entry("정상", "5000", "30.10")));

    assertThat(html).contains("5,000원");
    assertThat(count(html, "data-taxable-unknown=\"true\"")).isZero();
  }
}
