package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.web.dto.response.TradeProfitPeriodSummary;

/**
 * 2026-11-02 사용자 결정 두 가지 - 시계열의 배당은 배당락일에 세고, 시작일을 준 기간 요약은 시작 전날 마감을 기초로 쓴다(그날은 쪼갬 표에 남기지 않는다).
 */
class PeriodOpeningAndExDateTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static Dividend dividend(String recordDate, String payDate) {
    Dividend dividend = new Dividend();
    dividend.setRecordDate(recordDate == null ? null : Instant.parse(recordDate + "T00:00:00Z"));
    dividend.setPayDate(Instant.parse(payDate + "T00:00:00Z"));
    return dividend;
  }

  @Test
  void 배당은_기준일_하루_전_평일에_세고_기준일이_없으면_지급일() {
    // 월말 기준일(10-31 토) 종목: 배당락 10-30(금), 지급 11-04.
    assertThat(TradeProfitService.dividendDay(dividend("2026-10-31", "2026-11-04"), KST))
        .isEqualTo(LocalDate.parse("2026-10-30"));
    // 기준일 월요일이면 앞 금요일.
    assertThat(TradeProfitService.dividendDay(dividend("2026-11-02", "2026-11-04"), KST))
        .isEqualTo(LocalDate.parse("2026-10-30"));
    assertThat(TradeProfitService.dividendDay(dividend("2026-11-04", "2026-11-04"), KST))
        .as("기준일 = 지급일이면 원장 가져오기가 채운 값 - 모르는 것으로 보고 지급일")
        .isEqualTo(LocalDate.parse("2026-11-04"));
    assertThat(TradeProfitService.dividendDay(dividend(null, "2026-11-04"), KST))
        .isEqualTo(LocalDate.parse("2026-11-04"));
  }

  private static TradeProfitPeriodSummary row(String label, String from, String to) {
    return new TradeProfitPeriodSummary(
        "MONTH", label, LocalDate.parse(from), LocalDate.parse(to), false, null);
  }

  @Test
  void 시작_전날만_든_구간은_빼고_같은_구간이면_시작을_시작일로() {
    var rows =
        TradeProfitService.withoutBucketsBefore(
            List.of(
                row("2026-11", "2026-11-01", "2026-11-30"),
                row("2026-10", "2026-10-31", "2026-10-31")),
            LocalDate.parse("2026-11-01"));
    assertThat(rows).extracting(TradeProfitPeriodSummary::label).containsExactly("2026-11");
    assertThat(rows.get(0).fromDate())
        .as("시작일 이후 구간은 그대로")
        .isEqualTo(LocalDate.parse("2026-11-01"));

    var midMonth =
        TradeProfitService.withoutBucketsBefore(
            List.of(row("2026-10", "2026-10-01", "2026-10-31")), LocalDate.parse("2026-10-02"));
    assertThat(midMonth.get(0).fromDate()).isEqualTo(LocalDate.parse("2026-10-02"));
    assertThat(midMonth.get(0).complete()).as("2 일 시작인데 기초 1 일이 온전한 달로 세지면 안 된다").isFalse();
  }
}
