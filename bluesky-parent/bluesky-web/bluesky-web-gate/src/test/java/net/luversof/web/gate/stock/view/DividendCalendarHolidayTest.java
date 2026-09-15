package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.util.KoreanHolidays;

/**
 * 배당 캘린더의 공휴일 표시.
 *
 * <p>공휴일 표는 손으로 채운 것이라 <b>수록 범위가 있다</b>. 범위 밖 해는 이름이 전부 null 이라 화면이 "그 해엔 공휴일이 없다" 처럼 보인다 &mdash;
 * 자료가 없는 것과 공휴일이 없는 것은 다르므로 그 사실을 밝혀야 한다.
 *
 * <p>공휴일이라고 종목을 옮기지는 않는다. 지급일을 영업일로 보정해 봤더니 최빈일 그대로 두는 것보다 오히려 빗나갔다(기록: 다가올 배당). 달력은 "그 날이 쉬는 날"
 * 이라는 사실만 말한다.
 */
class DividendCalendarHolidayTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String render(YearMonth month) {
    DividendCalendarView calendar =
        new DividendCalendarView(
            month,
            DividendCalendarGridUtil.build(month, LocalDate.parse("2026-09-14"), Map.of()),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=" + month.minusMonths(1));
    model.put("nextHref", "/stock/dividend?tab=calendar&month=" + month.plusMonths(1));
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", false);
    model.put("avgLabel", "평균");
    model.put("latestLabel", "최근");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  /** 2026-10 은 개천절 · 대체공휴일 · 한글날 셋이 든 달이다. */
  @Test
  void 공휴일_이름을_그_날에_적는다() {
    String html = render(YearMonth.of(2026, 10));

    assertThat(html).contains("data-calendar-day=\"2026-10-03\"");
    assertThat(html).contains("개천절");
    assertThat(html).contains("대체공휴일(개천절)");
    assertThat(html).contains("한글날");
  }

  /** 이름만 있고 어느 칸인지 안 붙으면 탐침도 사람도 어느 날인지 모른다. */
  @Test
  void 공휴일인_칸을_표시한다() {
    String html = render(YearMonth.of(2026, 10));

    assertThat(html)
        .as("칸에 공휴일 표시가 없으면 색만 남아 색을 못 가르는 사람에게 뜻이 없다")
        .contains("data-holiday=\"개천절\"");
  }

  /** 공휴일이 없는 달에는 아무 이름도 적지 않는다 - 늘 무언가 적히면 검사가 헛돈다. */
  @Test
  void 공휴일이_없는_달에는_적지_않는다() {
    String html = render(YearMonth.of(2026, 11));

    assertThat(html).doesNotContain("data-holiday=\"");
  }

  /**
   * 표에 없는 해는 "공휴일 없음" 이 아니라 "자료 없음" 이다.
   *
   * <p>이 검사는 표가 그 해까지 채워지면 자연스럽게 깨진다 &mdash; 그때는 {@code uncoveredYear()} 가 다음 빈 해를 가리키므로 그대로 통과한다.
   */
  @Test
  void 표에_없는_해는_자료가_없다고_밝힌다() {
    String html = render(YearMonth.of(uncoveredYear(), 2));

    assertThat(html).contains("data-calendar-holiday-missing");
    assertThat(html).contains(String.valueOf(uncoveredYear()));
  }

  @Test
  void 수록된_해에는_그_안내를_띄우지_않는다() {
    assertThat(render(YearMonth.of(2026, 11))).doesNotContain("data-calendar-holiday-missing");
  }

  /** 수록된 마지막 해 다음 해. 표가 늘어나면 이 값도 따라 움직인다. */
  private static int uncoveredYear() {
    int year =
        KoreanHolidays.coveredYears().stream().mapToInt(Integer::intValue).max().orElse(2026);
    return year + 1;
  }

  @Test
  void 수록_연도는_끊기지_않는다() {
    List<Integer> years = List.copyOf(KoreanHolidays.coveredYears());

    assertThat(years).as("표가 비면 달력이 통째로 평일이 된다").isNotEmpty();
    for (int i = 1; i < years.size(); i++) {
      assertThat(years.get(i))
          .as("수록 연도 사이에 구멍이 있으면 그 해만 조용히 공휴일이 사라진다")
          .isEqualTo(years.get(i - 1) + 1);
    }
    assertThat(KoreanHolidays.covered(uncoveredYear())).isFalse();
  }
}
