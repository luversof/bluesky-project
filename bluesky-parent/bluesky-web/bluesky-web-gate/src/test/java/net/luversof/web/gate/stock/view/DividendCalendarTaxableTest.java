package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 배당 캘린더의 예상 과세표준.
 *
 * <p>칸에 종목별 과세표준을 찍으므로 합계는 <b>보이는 값</b>을 더한 것이어야 한다 &mdash; 원값을 더한 뒤 한 번 반올림하면 화면의 숫자를 손으로 더한 값과
 * 달라진다(배당 합계에서 이미 겪은 문제다).
 *
 * <p>같은 값을 월배당 시뮬레이터도 적는다. 두 화면이 같은 자료로 다른 수를 적으면 어느 쪽이 맞는지 화면으로는 알 수 없으므로, 합산 규칙이 갈라지지 않는지 여기서
 * 못박는다.
 */
class DividendCalendarTaxableTest {

  /** 원 미만이 남는 값들 - 각각 반올림한 합과, 원값 합을 한 번 반올림한 값이 다르다. */
  private static final List<String> TAXABLE =
      List.of("10470.6", "50693.5", "2070.7", "61007.6", "100923.6");

  private static DividendCalendarView.Entry entry(String name, String taxable) {
    BigDecimal value = new BigDecimal(taxable);
    return new DividendCalendarView.Entry(
        name, name, value, value, value, null, 17, 17, 17, 12, false);
  }

  @Test
  void 칸의_과세표준_합계는_보이는_값을_더한_것이다() {
    List<DividendCalendarView.Entry> entries =
        List.of(
            entry("가", TAXABLE.get(0)),
            entry("나", TAXABLE.get(1)),
            entry("다", TAXABLE.get(2)),
            entry("라", TAXABLE.get(3)),
            entry("마", TAXABLE.get(4)));
    YearMonth month = YearMonth.of(2026, 9);
    var weeks =
        DividendCalendarGridUtil.build(
            month,
            LocalDate.parse("2026-09-14"),
            DividendCalendarGridUtil.groupByDay(entries, month));
    var day =
        weeks.stream()
            .flatMap(week -> week.days().stream())
            .filter(d -> d.inMonth() && d.date().getDayOfMonth() == 17)
            .findFirst()
            .orElseThrow();

    long shownSum =
        TAXABLE.stream().mapToLong(v -> StockFormatUtil.displayWon(new BigDecimal(v))).sum();
    long roundedOnce =
        StockFormatUtil.displayWon(
            TAXABLE.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add));

    assertThat(shownSum).as("두 방식이 같은 자료를 골랐다 - 이 검사가 아무것도 지키지 못한다").isNotEqualTo(roundedOnce);
    assertThat(day.taxableTotal()).isEqualByComparingTo(BigDecimal.valueOf(shownSum));
  }

  /**
   * 과세표준이 0 인 종목도 칸에 적는다(2026-09-14 요구) - 다만 합계에는 0 으로 들어가야 한다.
   *
   * <p>표기 자체는 {@link DividendCalendarZeroTaxableTest} 가 지킨다. 여기서 지키는 건 합계다 - 0 을 적기 시작했다고 합계가 달라지면
   * 칸 숫자와 합계가 어긋난다.
   */
  @Test
  void 과세표준이_없으면_합계에_영향이_없다() {
    YearMonth month = YearMonth.of(2026, 9);
    var weeks =
        DividendCalendarGridUtil.build(
            month,
            null,
            DividendCalendarGridUtil.groupByDay(
                List.of(entry("있음", "1000"), entry("없음", "0")), month));
    var day =
        weeks.stream()
            .flatMap(week -> week.days().stream())
            .filter(d -> d.hasEntries())
            .findFirst()
            .orElseThrow();

    assertThat(day.taxableTotal()).isEqualByComparingTo("1000");
  }

  /** 월배당 시뮬레이터의 합산 규칙과 갈라지면 안 된다. */
  @Test
  void 시뮬레이터와_같은_규칙으로_더한다() {
    var rows = TAXABLE.stream().map(DividendCalendarTaxableTest::snapshot).toList();

    var summary =
        new net.luversof.web.gate.stock.service.MonthlyDividendCalculator()
            .buildSimulatorSummary(rows);

    long shownSum =
        TAXABLE.stream().mapToLong(v -> StockFormatUtil.displayWon(new BigDecimal(v))).sum();
    assertThat(summary.totalExpectedTaxableBaseAmount())
        .as("두 화면이 같은 자료로 다른 수를 적으면 어느 쪽이 맞는지 화면으로는 알 수 없다")
        .isEqualByComparingTo(BigDecimal.valueOf(shownSum));
  }

  /** 과세표준만 채운 최소 스냅샷. 나머지 칸은 이 검사와 상관이 없다. */
  private static net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse snapshot(
      String taxable) {
    return new net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse(
        null,
        null,
        null,
        "SYM" + taxable,
        null,
        null,
        BigDecimal.ZERO,
        null,
        null,
        1,
        BigDecimal.ZERO,
        null,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        null,
        null,
        null,
        new BigDecimal(taxable),
        null,
        BigDecimal.ZERO,
        null);
  }
}
