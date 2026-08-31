package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitPeriodSummary;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/**
 * 조회 기간을 <b>달 또는 해</b>로 쪼갠 성과를 내는지 본다.
 *
 * <p>연 단위만 있으면 짧은 구간이 한 줄로 끝나 아무것도 말해 주지 않는다 &mdash; 실측 2026-08-31 삼성전자 '올해': 연도별 <b>1 행</b>. 반대로
 * 17 년치를 달로 쪼개면 200 줄이 되어 읽히지 않는다. 그래서 조회 기간 길이로 단위를 고른다.
 *
 * <p>각 구간의 기초는 <b>직전 구간의 마지막 지점</b>이다({@code summarizeByYear} 와 같은 규칙). 자기 첫 지점을 기초로 쓰면 그 달/해 첫날의
 * 손익이 통째로 빠진다.
 */
class PeriodBreakdownTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static TradeProfitTimeSeriesPoint point(LocalDate date, String value, String cost) {
    BigDecimal holdingsValue = new BigDecimal(value);
    BigDecimal holdingsCost = new BigDecimal(cost);
    return new TradeProfitTimeSeriesPoint(
        date.atStartOfDay(KST).toInstant(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        0L,
        holdingsValue,
        holdingsCost,
        holdingsValue.subtract(holdingsCost),
        BigDecimal.ZERO,
        date);
  }

  /** 6~8 월 각 달의 마지막 날. 원가는 1,000 그대로라 손익은 평가액 변동이 전부다. */
  private static List<TradeProfitTimeSeriesPoint> threeMonths() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 6, 30), "1000", "1000"));
    series.add(point(LocalDate.of(2026, 7, 31), "1300", "1000"));
    series.add(point(LocalDate.of(2026, 8, 31), "1100", "1000"));
    return series;
  }

  @Test
  void 달로_쪼개면_달마다_한_줄씩_낸다() {
    List<TradeProfitPeriodSummary> rows =
        TradeProfitService.summarizeByPeriod(threeMonths(), KST, false, "MONTH");

    assertThat(rows)
        .extracting(TradeProfitPeriodSummary::label)
        .containsExactly("2026-08", "2026-07", "2026-06");
    assertThat(rows).allSatisfy(row -> assertThat(row.unit()).isEqualTo("MONTH"));
  }

  /**
   * 각 달의 기초는 <b>직전 달의 마지막 값</b>이다.
   *
   * <p>7 월은 1,000 &rarr; 1,300 이라 +300, 8 월은 1,300 &rarr; 1,100 이라 &minus;200 이어야 한다. 자기 첫 지점을 기초로
   * 쓰면 둘 다 0 이 되어 표가 통째로 무의미해진다.
   */
  @Test
  void 각_구간의_기초는_직전_구간의_마지막_값이다() {
    List<TradeProfitPeriodSummary> rows =
        TradeProfitService.summarizeByPeriod(threeMonths(), KST, false, "MONTH");

    assertThat(rows.get(0).summary().periodProfit()).isEqualByComparingTo("-200");
    assertThat(rows.get(1).summary().periodProfit()).isEqualByComparingTo("300");
  }

  /**
   * 조회 기간이 달을 가로지르면 그 달을 온전히 덮지 못한다.
   *
   * <p>밝히지 않으면 "8월 &minus;3%" 를 그 달 전체 성과로 오해한다. 위 표본은 6 월 30 일 하루뿐이라 6 월은 온전하지 않고, 7 월도 31 일 하루만
   * 있으므로 온전하지 않다.
   */
  @Test
  void 온전히_덮지_못한_구간을_밝힌다() {
    List<TradeProfitPeriodSummary> rows =
        TradeProfitService.summarizeByPeriod(threeMonths(), KST, false, "MONTH");

    assertThat(rows).allSatisfy(row -> assertThat(row.complete()).isFalse());

    // 7 월을 1 일부터 31 일까지 덮으면 온전하다.
    List<TradeProfitTimeSeriesPoint> whole = new ArrayList<>();
    whole.add(point(LocalDate.of(2026, 7, 1), "1000", "1000"));
    whole.add(point(LocalDate.of(2026, 7, 31), "1300", "1000"));
    assertThat(TradeProfitService.summarizeByPeriod(whole, KST, false, "MONTH"))
        .singleElement()
        .satisfies(row -> assertThat(row.complete()).isTrue());
  }

  @Test
  void 해로_쪼개면_해마다_한_줄씩_낸다() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2024, 12, 31), "1000", "1000"));
    series.add(point(LocalDate.of(2025, 12, 31), "1400", "1000"));

    List<TradeProfitPeriodSummary> rows =
        TradeProfitService.summarizeByPeriod(series, KST, false, "YEAR");

    assertThat(rows).extracting(TradeProfitPeriodSummary::label).containsExactly("2025", "2024");
    assertThat(rows.get(0).summary().periodProfit()).isEqualByComparingTo("400");
  }

  /** 짧은 구간은 달로, 긴 구간은 해로. 3 년(36 개월)이 경계다. */
  @Test
  void 조회_기간_길이로_단위를_고른다() {
    List<TradeProfitTimeSeriesPoint> shortSpan = new ArrayList<>();
    shortSpan.add(point(LocalDate.of(2026, 1, 1), "1000", "1000"));
    shortSpan.add(point(LocalDate.of(2026, 8, 31), "1100", "1000"));
    assertThat(TradeProfitService.resolveBreakdownUnit(shortSpan, KST, "AUTO")).isEqualTo("MONTH");

    List<TradeProfitTimeSeriesPoint> longSpan = new ArrayList<>();
    longSpan.add(point(LocalDate.of(2020, 1, 1), "1000", "1000"));
    longSpan.add(point(LocalDate.of(2026, 8, 31), "1100", "1000"));
    assertThat(TradeProfitService.resolveBreakdownUnit(longSpan, KST, "AUTO")).isEqualTo("YEAR");
  }

  /** 직접 고른 단위는 그대로 따른다. 자동 판정이 사용자의 선택을 덮으면 안 된다. */
  @Test
  void 직접_고른_단위가_자동_판정보다_우선한다() {
    List<TradeProfitTimeSeriesPoint> longSpan = new ArrayList<>();
    longSpan.add(point(LocalDate.of(2020, 1, 1), "1000", "1000"));
    longSpan.add(point(LocalDate.of(2026, 8, 31), "1100", "1000"));

    assertThat(TradeProfitService.resolveBreakdownUnit(longSpan, KST, "MONTH")).isEqualTo("MONTH");
    assertThat(TradeProfitService.resolveBreakdownUnit(longSpan, KST, "YEAR")).isEqualTo("YEAR");
  }

  /** 보유도 거래도 없던 구간은 표에서 뺀다. 0 만 늘어선 줄은 읽는 사람을 방해한다. */
  @Test
  void 아무것도_없던_구간은_줄을_내지_않는다() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 6, 30), "0", "0"));
    series.add(point(LocalDate.of(2026, 7, 31), "0", "0"));
    series.add(point(LocalDate.of(2026, 8, 31), "1100", "1000"));

    assertThat(TradeProfitService.summarizeByPeriod(series, KST, false, "MONTH"))
        .extracting(TradeProfitPeriodSummary::label)
        .containsExactly("2026-08");
  }
}
