package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.view.DividendCalendarView;

/** 배당 캘린더의 달력 격자. */
class DividendCalendarGridUtilTest {

  private static DividendCalendarView.Entry entry(String name, int payDay, String amount) {
    return new DividendCalendarView.Entry(
        name,
        name,
        new BigDecimal(amount),
        new BigDecimal(amount),
        BigDecimal.ZERO,
        null,
        payDay,
        payDay,
        payDay,
        12,
        false);
  }

  /** 줄은 일요일에서 시작한다. 요일이 밀리면 모든 종목이 엉뚱한 날에 놓인다. */
  @Test
  void 일요일에서_시작한다() {
    var weeks = DividendCalendarGridUtil.build(YearMonth.of(2026, 9), null, Map.of());

    assertThat(weeks.get(0).days().get(0).date().getDayOfWeek())
        .isEqualTo(java.time.DayOfWeek.SUNDAY);
    assertThat(weeks).allSatisfy(week -> assertThat(week.days()).hasSize(7));
  }

  /** 줄 수는 그 달에 필요한 만큼만 - 언제나 6 줄로 두면 통째로 다음 달인 빈 줄이 남는다. */
  @Test
  void 줄_수는_그_달에_맞춘다() {
    // 2026-09 는 화요일 시작 30 일 -> 5 줄, 2026-08 은 토요일 시작 31 일 -> 6 줄
    assertThat(DividendCalendarGridUtil.build(YearMonth.of(2026, 9), null, Map.of())).hasSize(5);
    assertThat(DividendCalendarGridUtil.build(YearMonth.of(2026, 8), null, Map.of())).hasSize(6);
  }

  /** 앞뒤 달 날짜로 칸을 채우되 그 달이 아님을 표시한다. */
  @Test
  void 앞뒤_달_날짜는_이_달이_아니다() {
    var weeks = DividendCalendarGridUtil.build(YearMonth.of(2026, 9), null, Map.of());
    long inMonth =
        weeks.stream()
            .flatMap(week -> week.days().stream())
            .filter(DividendCalendarView.Day::inMonth)
            .count();

    assertThat(inMonth).isEqualTo(30);
    assertThat(weeks.get(0).days().get(0).inMonth()).isFalse();
  }

  @Test
  void 오늘은_그_달에_있을_때만_표시한다() {
    var september =
        DividendCalendarGridUtil.build(
            YearMonth.of(2026, 9), LocalDate.parse("2026-09-14"), Map.of());
    long marked =
        september.stream()
            .flatMap(week -> week.days().stream())
            .filter(DividendCalendarView.Day::today)
            .count();

    assertThat(marked).isEqualTo(1);

    var october =
        DividendCalendarGridUtil.build(
            YearMonth.of(2026, 10), LocalDate.parse("2026-09-14"), Map.of());
    assertThat(october.stream().flatMap(week -> week.days().stream()))
        .as("다른 달을 보고 있는데 오늘이 찍히면 안 된다")
        .noneMatch(DividendCalendarView.Day::today);
  }

  /** 한 칸에 여럿이면 금액이 큰 종목부터 - 칸이 좁아 위에서 몇 줄만 눈에 들어온다. */
  @Test
  void 같은_날은_금액_큰_종목부터() {
    var byDay =
        DividendCalendarGridUtil.groupByDay(
            List.of(entry("작은", 17, "100"), entry("큰", 17, "900"), entry("중간", 17, "500")),
            YearMonth.of(2026, 9));

    assertThat(byDay.get(17))
        .extracting(DividendCalendarView.Entry::name)
        .containsExactly("큰", "중간", "작은");
  }

  /** 31 일 지급 종목이 2 월에 사라지면 안 된다. */
  @Test
  void 그_달에_없는_날은_마지막_날로_당긴다() {
    var byDay =
        DividendCalendarGridUtil.groupByDay(
            List.of(entry("월말종목", 31, "100")), YearMonth.of(2027, 2));

    assertThat(byDay).containsOnlyKeys(28);
  }

  /** 날짜를 모르는 종목(payDay 0)은 어느 칸에도 놓지 않는다. */
  @Test
  void 날짜를_모르면_놓지_않는다() {
    var byDay =
        DividendCalendarGridUtil.groupByDay(List.of(entry("모름", 0, "100")), YearMonth.of(2026, 9));

    assertThat(byDay).isEmpty();
  }

  @Test
  void 칸의_합계는_그_칸의_종목만_더한다() {
    var weeks =
        DividendCalendarGridUtil.build(
            YearMonth.of(2026, 9),
            null,
            DividendCalendarGridUtil.groupByDay(
                List.of(entry("가", 17, "100"), entry("나", 17, "250"), entry("다", 2, "70")),
                YearMonth.of(2026, 9)));
    var day17 =
        weeks.stream()
            .flatMap(week -> week.days().stream())
            .filter(day -> day.inMonth() && day.date().getDayOfMonth() == 17)
            .findFirst()
            .orElseThrow();

    assertThat(day17.latestTotal()).isEqualByComparingTo("350");
    assertThat(day17.averageTotal()).isEqualByComparingTo("350");
    assertThat(day17.hasEntries()).isTrue();
  }

  @Test
  void 빈_입력에도_죽지_않는다() {
    assertThat(DividendCalendarGridUtil.build(null, null, Map.of())).isEmpty();
    assertThat(DividendCalendarGridUtil.groupByDay(null, YearMonth.of(2026, 9))).isEmpty();
    assertThat(DividendCalendarGridUtil.groupByDay(List.of(), null)).isEmpty();
  }
}
