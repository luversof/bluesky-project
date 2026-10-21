package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil;
import net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.RecordedPayout;
import net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil.SettlementRule;

/**
 * 다가올 배당의 예상 지급일 = 기준일 규칙 + 종목별 결제 거래일 수(2026-10-20 자율 개선).
 *
 * <p>실측(지급 이력 209 건, 그 전 이력만으로 맞히기): 최빈 일자 + 영업일 보정 119 건(57%), 이 규칙 209 건(100%). 아래 기준일 · 지급일은 실제
 * 이력의 날짜다(월중 9 종목 대부분 · 월말 12 종목).
 */
class UpcomingPayDateRuleTest {

  private static final List<RecordedPayout> MID =
      List.of(
          rec("MIDX", "2026-07-15", "2026-07-20"), // 17 일 제헌절을 건너뛴 2 거래일
          rec("MIDX", "2026-08-14", "2026-08-19"), // 15 일 토 · 광복절 -> 14 일, 17 일 대체공휴일 건너뜀
          rec("MIDX", "2026-09-15", "2026-09-17"),
          rec("MIDX", "2026-10-15", "2026-10-19"));

  private static final List<RecordedPayout> END =
      List.of(
          rec("ENDX", "2025-12-30", "2026-01-05"), // 31 일 연말 휴장 · 1 일 신정
          rec("ENDX", "2026-07-31", "2026-08-04"),
          rec("ENDX", "2026-08-31", "2026-09-02"),
          rec("ENDX", "2026-09-30", "2026-10-02"));

  private static RecordedPayout rec(String symbol, String record, String pay) {
    return new RecordedPayout(symbol, LocalDate.parse(record), LocalDate.parse(pay));
  }

  @Test
  void 실제_이력에서_규칙과_거래일_수를_읽는다() {
    List<RecordedPayout> all = new ArrayList<>(MID);
    all.addAll(END);
    // 기준일이 규칙(15 일 · 마지막 거래일)과 안 맞는 종목은 규칙을 주지 않는다 - 예전 추정으로.
    all.add(rec("ODDX", "2026-08-10", "2026-08-12"));
    all.add(rec("ODDX", "2026-09-10", "2026-09-14"));
    Map<String, SettlementRule> rules = MonthlyDividendPayDayUtil.settlementRules(all);

    assertThat(rules.get("MIDX")).isEqualTo(new SettlementRule("MID_MONTH", 2, 4));
    assertThat(rules.get("ENDX")).isEqualTo(new SettlementRule("MONTH_END", 2, 4));
    assertThat(rules).doesNotContainKey("ODDX");
  }

  @Test
  void 이력의_지급일을_규칙으로_그대로_다시_낸다() {
    List<RecordedPayout> all = new ArrayList<>(MID);
    all.addAll(END);
    Map<String, SettlementRule> rules = MonthlyDividendPayDayUtil.settlementRules(all);
    for (RecordedPayout payout : MID) {
      assertThat(
              MonthlyDividendPayDayUtil.projectedPayDate(
                  YearMonth.from(payout.payDate()), rules.get("MIDX")))
          .as(payout.toString())
          .isEqualTo(payout.payDate());
    }
    for (RecordedPayout payout : END) {
      assertThat(
              MonthlyDividendPayDayUtil.projectedPayDate(
                  YearMonth.from(payout.payDate()), rules.get("ENDX")))
          .as(payout.toString())
          .isEqualTo(payout.payDate());
    }
    // 앞으로: 11 월 월중 = 13 일(금) 기준 -> 17 일(화), 11 월 초 월말 지급 = 10-30(금) 기준 -> 11-03(화).
    assertThat(
            MonthlyDividendPayDayUtil.projectedPayDate(
                YearMonth.parse("2026-11"), rules.get("MIDX")))
        .isEqualTo(LocalDate.parse("2026-11-17"));
    assertThat(
            MonthlyDividendPayDayUtil.projectedPayDate(
                YearMonth.parse("2026-11"), rules.get("ENDX")))
        .isEqualTo(LocalDate.parse("2026-11-03"));
    // 2027 년 1 월: 2026-12-30(수) 기준(31 일 휴장) -> 31 휴장 · 1/1 신정 건너 1/4(월) · 1/5(화).
    assertThat(
            MonthlyDividendPayDayUtil.projectedPayDate(
                YearMonth.parse("2027-01"), rules.get("ENDX")))
        .isEqualTo(LocalDate.parse("2027-01-05"));
  }

  /** 대시보드 다가올 배당 카드의 시기별 다음 지급일도 같은 규칙(2026-10-20). 지급일 당일은 그 날을, 지나면 다음 달을. */
  @Test
  void 대시보드_시기별_다음_지급일() {
    List<RecordedPayout> all = new ArrayList<>(MID);
    all.addAll(END);
    // SOL 코리아고배당처럼 결제 1 거래일인 월중 종목(실제: 10-15 기준 -> 10-16 지급).
    all.add(rec("MIDY", "2026-08-14", "2026-08-18"));
    all.add(rec("MIDY", "2026-09-15", "2026-09-16"));
    all.add(rec("MIDY", "2026-10-15", "2026-10-16"));
    Map<String, SettlementRule> rules = MonthlyDividendPayDayUtil.settlementRules(all);
    Map<String, String> windows =
        Map.of(
            "MIDX", "MID_MONTH", "MIDY", "MID_MONTH", "ENDX", "MONTH_END", "NORULE", "MONTH_END");
    java.util.Set<String> held = java.util.Set.of("MIDX", "MIDY", "ENDX");

    // 10-16 엔 MIDY 가 그날, MIDX 는 10-19 - 가장 이른 날과 그 달 범위.
    var oct16 =
        StockSummaryHtmxController.ruledNextPayDates(
            LocalDate.parse("2026-10-16"), "MID_MONTH", rules, windows, held);
    assertThat(oct16.first()).isEqualTo(LocalDate.parse("2026-10-16"));
    assertThat(oct16.lastSameMonth()).isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(oct16.symbols()).containsExactlyInAnyOrder("MIDX", "MIDY");
    // 10-17 엔 MIDY 가 이번 달 몫을 이미 받았다(다음은 11-16) - 카드 합계 · 목록에서 뺀다(검토 2026-10-20).
    var oct17 =
        StockSummaryHtmxController.ruledNextPayDates(
            LocalDate.parse("2026-10-17"), "MID_MONTH", rules, windows, held);
    assertThat(oct17.first()).isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(oct17.symbols()).containsExactly("MIDX");
    // 10-17 ~ 10-19 엔 MIDX 의 10-19 가 남았다 - 시기 전체 최빈값 하나로 냈다면 MIDY 는 틀리고, 더 늦은 종목이 있으면 그 날 이미 다음 달을
    // 가리켰다.
    assertThat(
            StockSummaryHtmxController.ruledNextPayDates(
                    LocalDate.parse("2026-10-19"), "MID_MONTH", rules, windows, held)
                .first())
        .isEqualTo(LocalDate.parse("2026-10-19"));
    var oct20 =
        StockSummaryHtmxController.ruledNextPayDates(
            LocalDate.parse("2026-10-20"), "MID_MONTH", rules, windows, held);
    assertThat(oct20.first()).isEqualTo(LocalDate.parse("2026-11-16"));
    assertThat(oct20.lastSameMonth()).isEqualTo(LocalDate.parse("2026-11-17"));
    assertThat(
            StockSummaryHtmxController.ruledNextPayDates(
                    LocalDate.parse("2026-10-20"), "MONTH_END", rules, windows, held)
                .first())
        .isEqualTo(LocalDate.parse("2026-11-03"));
    // 들고 있는 종목 중 하나라도 규칙이 없으면 그 시기는 통째로 예전 추정.
    assertThat(
            StockSummaryHtmxController.ruledNextPayDates(
                LocalDate.parse("2026-10-20"),
                "MONTH_END",
                rules,
                windows,
                java.util.Set.of("ENDX", "NORULE")))
        .isNull();
  }

  private static MonthlyDividendPayoutResponse payout(String symbol, String record, String pay) {
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        null,
        symbol,
        symbol,
        LocalDate.parse(record),
        LocalDate.parse(pay),
        null,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        null);
  }

  private static MonthlyDividendSnapshotResponse snapshot(UUID item, String symbol) {
    BigDecimal one = BigDecimal.ONE;
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        null,
        item,
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-10-15"),
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        100,
        one,
        one,
        one,
        BigDecimal.valueOf(1000),
        one,
        one,
        one,
        one,
        BigDecimal.ZERO,
        one,
        one,
        null);
  }

  @Test
  void 다가올_배당이_규칙의_날짜에_놓인다() {
    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setMonthlyDividendCalculator(new MonthlyDividendCalculator());
    StockDividendViewController controller = new StockDividendViewController();
    controller.setMonthlyDividendReferenceSupport(support);
    DividendClient client = org.mockito.Mockito.mock(DividendClient.class);
    org.mockito.Mockito.when(client.findDividends(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    org.springframework.test.util.ReflectionTestUtils.setField(
        controller, "dividendClient", client);

    List<MonthlyDividendPayoutResponse> payouts = new ArrayList<>();
    for (RecordedPayout each : MID) {
      payouts.add(payout("MIDX", each.recordDate().toString(), each.payDate().toString()));
    }
    for (RecordedPayout each : END) {
      payouts.add(payout("ENDX", each.recordDate().toString(), each.payDate().toString()));
    }
    UpcomingDividendScheduleView view =
        controller.buildUpcomingSchedule(
            UUID.randomUUID(),
            List.of(snapshot(UUID.randomUUID(), "MIDX"), snapshot(UUID.randomUUID(), "ENDX")),
            payouts,
            LocalDate.parse("2026-10-20"),
            java.util.Set.of());

    // 10 월 몫: 월중 10-19 는 발표된 지급일(이력), 월말 10-02 도 이력 - 둘 다 지났다. 11 월 몫은 규칙: 11-03(월말) · 11-17(월중).
    // 예전 추정이었다면 월말은 최빈 일자(2 일 · 4 일 · 5 일 중) · 월중은 17 · 19 · 20 일 중 하나에 놓였다.
    assertThat(view.days())
        .extracting(DividendCalendarView.Day::date)
        .containsExactly(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-17"));
  }
}
