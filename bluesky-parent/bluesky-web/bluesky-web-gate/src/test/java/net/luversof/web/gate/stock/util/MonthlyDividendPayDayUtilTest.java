package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 종목별 대표 지급일.
 *
 * <p>배당 캘린더가 종목을 달력의 어느 칸에 놓을지가 여기서 정해진다. 규칙을 바꾸면 화면의 날짜가 조용히 움직인다.
 */
class MonthlyDividendPayDayUtilTest {

  private static MonthlyDividendPayDayUtil.Payout pay(String symbol, String date) {
    return new MonthlyDividendPayDayUtil.Payout(symbol, LocalDate.parse(date));
  }

  /** 가장 자주 나온 날을 쓴다 - 최신 1건만 보면 어쩌다 늦게 준 달에 통째로 끌려간다. */
  @Test
  void 최빈일을_쓴다() {
    var result =
        MonthlyDividendPayDayUtil.byStockItem(
            List.of(
                pay("A", "2026-06-17"),
                pay("A", "2026-07-17"),
                pay("A", "2026-08-20"),
                pay("A", "2026-09-17")));

    assertThat(result.get("A").day()).isEqualTo(17);
    assertThat(result.get("A").sampleCount()).isEqualTo(4);
  }

  /** 같은 횟수면 최근에 나온 쪽 - 일정이 옮겨간 경우를 따라가야 한다. */
  @Test
  void 동률이면_최근_쪽을_쓴다() {
    var result =
        MonthlyDividendPayDayUtil.byStockItem(
            List.of(pay("A", "2026-06-17"), pay("A", "2026-09-20")));

    assertThat(result.get("A").day()).isEqualTo(20);
  }

  /** 실제 지급일은 한 날에 모여 있지 않다. 그 폭을 알려 줘야 화면이 "그 날 들어온다"고 말하지 않는다. */
  @Test
  void 관측된_폭을_함께_돌려준다() {
    var payDay =
        MonthlyDividendPayDayUtil.byStockItem(
                List.of(
                    pay("A", "2026-06-17"),
                    pay("A", "2026-07-19"),
                    pay("A", "2026-08-20"),
                    pay("A", "2026-09-17")))
            .get("A");

    assertThat(payDay.earliest()).isEqualTo(17);
    assertThat(payDay.latest()).isEqualTo(20);
    assertThat(payDay.spread()).isTrue();
  }

  @Test
  void 한_날에만_있으면_폭이_없다() {
    var payDay =
        MonthlyDividendPayDayUtil.byStockItem(
                List.of(pay("A", "2026-08-17"), pay("A", "2026-09-17")))
            .get("A");

    assertThat(payDay.spread()).isFalse();
  }

  /** 이력이 없는 종목은 결과에 담기지 않는다 - 0 이나 1 일로 채우면 없는 일정을 지어내는 셈이다. */
  @Test
  void 이력이_없으면_담지_않는다() {
    var result =
        MonthlyDividendPayDayUtil.byStockItem(
            List.of(pay("A", "2026-09-17"), new MonthlyDividendPayDayUtil.Payout("B", null)));

    assertThat(result).containsOnlyKeys("A");
  }

  @Test
  void 종목코드는_다듬어_맞춘다() {
    var result = MonthlyDividendPayDayUtil.byStockItem(List.of(pay(" a1 ", "2026-09-17")));

    assertThat(result).containsOnlyKeys("A1");
    assertThat(MonthlyDividendPayDayUtil.normalizeSymbol("  ")).isNull();
    assertThat(MonthlyDividendPayDayUtil.normalizeSymbol(null)).isNull();
  }

  @Test
  void 빈_입력에도_죽지_않는다() {
    assertThat(MonthlyDividendPayDayUtil.byStockItem(null)).isEmpty();
    assertThat(MonthlyDividendPayDayUtil.byStockItem(List.of())).isEmpty();
  }

  /** 31 일에 주던 종목도 2 월에는 31 일이 없다 - 마지막 날로 당기지 않으면 달력에서 사라진다. */
  @Test
  void 그_달에_없는_날은_마지막_날로_당긴다() {
    assertThat(MonthlyDividendPayDayUtil.dayInMonth(31, YearMonth.of(2027, 2))).isEqualTo(28);
    assertThat(MonthlyDividendPayDayUtil.dayInMonth(31, YearMonth.of(2028, 2))).isEqualTo(29);
    assertThat(MonthlyDividendPayDayUtil.dayInMonth(17, YearMonth.of(2027, 2))).isEqualTo(17);
    assertThat(MonthlyDividendPayDayUtil.dayInMonth(0, YearMonth.of(2027, 2))).isZero();
  }

  @Test
  void 종목마다_따로_센다() {
    Map<String, MonthlyDividendPayDayUtil.PayDay> result =
        MonthlyDividendPayDayUtil.byStockItem(
            List.of(
                pay("A", "2026-08-17"),
                pay("A", "2026-09-17"),
                pay("B", "2026-08-02"),
                pay("B", "2026-09-02")));

    assertThat(result.get("A").day()).isEqualTo(17);
    assertThat(result.get("B").day()).isEqualTo(2);
  }
}
