package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;

/**
 * "다가올 배당" 의 예상 지급일은 아직 오지 않은 지급을 건너뛰면 안 된다.
 *
 * <p>대표 지급일은 지급이력의 <b>최빈 일자</b>다(월중 그룹은 17 일). 그런데 실제 지급은 그 날 <b>이후</b>로 밀리는 달이 잦다 &mdash; 실측
 * 2026-09-11, 월중 그룹 11 개월: 17 일 당일이 5 회, +1 일 1 회, +2 일 3 회, +3 일 2 회. 밀리는 이유는 휴일만이 아니다 (2026-02-17
 * 은 설, 2026-07-17 은 금요일인데도 7/20 지급 - 이건 휴일이었다: 제헌절이 2026 년부터 공휴일인데 공휴일 표에 빠져 있었다, 2026-10-20 보탬).
 *
 * <p>그래서 17 일이 지나자마자 다음 달로 넘기면, 내일 들어올 배당을 두고 "한 달 뒤" 라고 말하게 된다.
 */
class UpcomingPayDateTest {

  @Test
  void 대표일_전이면_이번_달을_가리킨다() {
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-09-11"), 17, 20))
        .isEqualTo(LocalDate.parse("2026-09-17"));
  }

  @Test
  void 대표일_당일도_이번_달이다() {
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-09-17"), 17, 20))
        .isEqualTo(LocalDate.parse("2026-09-17"));
  }

  @Test
  void 대표일이_없는_달은_말일로_맞춘다() {
    // 2026-02-28 은 토요일이고 3/1(일) · 3/2(대체공휴일)라 그 달 안에 다음 영업일이 없다 - 앞 영업일 2/27(금)(2026-10-14 영업일 보정).
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-02-01"), 31, 31))
        .isEqualTo(LocalDate.parse("2026-02-27"));
  }

  /** 실측: 2026-08 월중 지급은 8/19 였다. 8/18 에 다음 달을 가리키면 내일 올 배당을 숨긴다. */
  @Test
  void 대표일이_지나도_관측된_마지막_날까지는_이번_달이다() {
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-08-18"), 17, 20))
        .as("8/19 지급을 두고 9/17 을 가리키면 안 된다 - 대표일 8/17 은 대체공휴일이라 영업일 8/18(2026-10-14 영업일 보정)")
        .isEqualTo(LocalDate.parse("2026-08-18"));
  }

  @Test
  void 관측된_마지막_날이_지나면_다음_달이다() {
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-08-21"), 17, 20))
        .isEqualTo(LocalDate.parse("2026-09-17"));
  }

  @Test
  void 관측된_마지막_날이_대표일보다_이르면_대표일을_쓴다() {
    assertThat(StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-09-18"), 17, 15))
        .as("이력이 부실해 last < rep 이어도 하루 만에 다음 달로 튀지 않는다 - 10/17 은 토요일이라 10/19(월)")
        .isEqualTo(LocalDate.parse("2026-10-19"));
  }

  /** 실측한 11 개월 전부에서 '아직 안 온 지급을 건너뛰지 않는다'. */
  @Test
  void 실측한_모든_달에서_지급일까지_이번_달을_가리킨다() {
    String[][] observed = {
      {"2025-10-17"},
      {"2025-11-18"},
      {"2025-12-17"},
      {"2026-01-19"},
      {"2026-02-20"},
      {"2026-03-17"},
      {"2026-04-17"},
      {"2026-05-19"},
      {"2026-06-17"},
      {"2026-07-20"},
      {"2026-08-19"},
    };
    for (String[] row : observed) {
      LocalDate actual = LocalDate.parse(row[0]);
      for (LocalDate day = actual.withDayOfMonth(1); !day.isAfter(actual); day = day.plusDays(1)) {
        LocalDate projected = StockSummaryHtmxController.projectedPayDate(day, 17, 20);
        assertThat(projected.getYear() + "-" + projected.getMonthValue())
            .as(day + " 에는 " + actual + " 지급이 아직 남아 있다")
            .isEqualTo(actual.getYear() + "-" + actual.getMonthValue());
      }
    }
  }

  @Test
  void 지급이_끝난_뒤에는_다음_달을_가리킨다() {
    // 관측된 마지막 날(20) 다음날부터는 이번 달을 더 가리키지 않는다.
    // 7 월 대표일 17 일은 제헌절(2026 년부터 공휴일, 2026-10-20 표에 보탬)이라 다음 영업일 20 일 - 실제 7 월 지급일과 같다.
    LocalDate projected =
        StockSummaryHtmxController.projectedPayDate(LocalDate.parse("2026-06-21"), 17, 20);
    assertThat(projected).isEqualTo(LocalDate.parse("2026-07-20"));
  }

  private MonthlyDividendPayoutResponse payout(String symbol, String payDate) {
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol,
        null,
        LocalDate.parse(payDate),
        null,
        null,
        null,
        null);
  }

  @Test
  void 관측된_마지막_날을_지급이력에서_뽑는다() {
    var payouts =
        List.of(
            payout("A", "2026-06-17"),
            payout("A", "2026-07-20"),
            payout("A", "2026-08-19"),
            payout("B", "2026-07-30"));
    var window = Map.of("A", "MID_MONTH", "B", "MONTH_END");

    assertThat(StockSummaryHtmxController.latestPayDay(payouts, window, "MID_MONTH", 15))
        .as("17·20·19 중 가장 늦은 20 이어야 유예가 실제 지급을 덮는다")
        .isEqualTo(20);
    assertThat(StockSummaryHtmxController.latestPayDay(payouts, window, "MONTH_END", 31))
        .isEqualTo(30);
  }

  @Test
  void 이력이_없으면_기본값을_쓴다() {
    assertThat(StockSummaryHtmxController.latestPayDay(List.of(), Map.of(), "MID_MONTH", 15))
        .isEqualTo(15);
  }

  @Test
  void 다른_시기의_지급일은_섞이지_않는다() {
    var payouts = List.of(payout("A", "2026-06-17"), payout("B", "2026-06-30"));
    var window = Map.of("A", "MID_MONTH", "B", "MONTH_END");

    assertThat(StockSummaryHtmxController.latestPayDay(payouts, window, "MID_MONTH", 15))
        .as("월말 지급일(30)이 섞이면 월중 유예가 열흘 넘게 늘어난다")
        .isEqualTo(17);
  }

  /** 추정일은 그 달 안의 영업일로 - 주말 · 공휴일이면 다음 영업일, 그 달에 없으면 앞 영업일(2026-10-14). */
  @Test
  void 추정일은_그_달의_영업일로_옮긴다() {
    assertThat(
            net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.businessDayInMonth(
                LocalDate.parse("2026-10-17")))
        .as("토요일 -> 월요일")
        .isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(
            net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.businessDayInMonth(
                LocalDate.parse("2026-10-09")))
        .as("한글날(금) -> 10/12(월)")
        .isEqualTo(LocalDate.parse("2026-10-12"));
    assertThat(
            net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.businessDayInMonth(
                LocalDate.parse("2026-08-17")))
        .as("대체공휴일(광복절) -> 8/18 - 2026-08 실측에서 17 일 추정이 지급이 있을 수 없는 날이었다")
        .isEqualTo(LocalDate.parse("2026-08-18"));
    assertThat(
            net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.businessDayInMonth(
                LocalDate.parse("2026-10-30")))
        .as("영업일은 그대로")
        .isEqualTo(LocalDate.parse("2026-10-30"));
    assertThat(
            net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.businessDayInMonth(
                LocalDate.parse("2026-10-31")))
        .as("달 끝 토요일은 다음 달로 넘기지 않고 앞 영업일")
        .isEqualTo(LocalDate.parse("2026-10-30"));
  }
}
