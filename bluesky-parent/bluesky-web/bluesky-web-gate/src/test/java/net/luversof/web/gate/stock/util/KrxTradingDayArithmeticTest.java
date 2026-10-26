package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * 거래일 셈 - 지급일(기준일 + 2 거래일) · 기준일 → 지급일 지연 · "지난 가장 최근 기준일". 마감(minusTradingDays) 과 기준일 규칙은
 * view/RecordDateBuyByTest 가 보지만 이 셋은 바로 보는 시험이 없어 2026-10-22 에 더했다. 주말 · 한글날(2026-10-09 금) · 연말
 * 휴장(12 월 마지막 평일) · 신정을 건넌다.
 */
class KrxTradingDayArithmeticTest {

  private static LocalDate d(String iso) {
    return LocalDate.parse(iso);
  }

  @Test
  void 거래일_더하기는_주말과_휴일을_건너뛴다() {
    assertThat(KrxRecordDateUtil.plusTradingDays(d("2026-10-15"), 2))
        .as("목 + 2 = 월(주말 건넘)")
        .isEqualTo(d("2026-10-19"));
    assertThat(KrxRecordDateUtil.plusTradingDays(d("2026-10-08"), 1))
        .as("한글날(금) 건넘")
        .isEqualTo(d("2026-10-12"));
    assertThat(KrxRecordDateUtil.plusTradingDays(d("2026-12-30"), 1))
        .as("12-31 연말 휴장 · 01-01 신정 건넘")
        .isEqualTo(d("2027-01-04"));
    assertThat(KrxRecordDateUtil.plusTradingDays(d("2026-10-15"), 0))
        .as("0 이면 그날")
        .isEqualTo(d("2026-10-15"));
  }

  @Test
  void 거래일_수는_다음_날부터_끝날까지() {
    assertThat(KrxRecordDateUtil.tradingDaysBetween(d("2026-10-15"), d("2026-10-19"))).isEqualTo(2);
    assertThat(KrxRecordDateUtil.tradingDaysBetween(d("2026-10-08"), d("2026-10-12")))
        .as("한글날 빼고")
        .isEqualTo(1);
    assertThat(KrxRecordDateUtil.tradingDaysBetween(d("2026-10-15"), d("2026-10-15"))).isZero();
    assertThat(KrxRecordDateUtil.tradingDaysBetween(d("2026-10-19"), d("2026-10-15")))
        .as("끝이 앞이면 0")
        .isZero();
  }

  @Test
  void 지난_가장_최근_기준일() {
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(d("2026-10-14"), "MID_MONTH"))
        .as("이번 달 기준일(10-15) 전이면 앞 달 것")
        .isEqualTo(d("2026-09-15"));
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(d("2026-10-15"), "MID_MONTH"))
        .as("기준일 당일은 포함")
        .isEqualTo(d("2026-10-15"));
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(d("2026-08-20"), "MID_MONTH"))
        .as("08-15 는 토요일 · 광복절 - 앞 거래일로 당긴다")
        .isEqualTo(d("2026-08-14"));
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(d("2026-10-22"), "MONTH_END"))
        .isEqualTo(d("2026-09-30"));
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(d("2026-10-22"), "OTHER")).isNull();
    assertThat(KrxRecordDateUtil.latestRecordDateOnOrBefore(null, "MID_MONTH")).isNull();
  }
}
