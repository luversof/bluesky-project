package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 기간 요약이 <b>기간 손익률</b>과 <b>기간 중 평가액 고점/저점</b>을 함께 내는지 본다.
 *
 * <p>화면에는 비율이 둘 있었다 &mdash; 투자 수익률(TWR, 입출금 제거)과 자산 증가율(평가액 기준). 그런데 헤드라인인 기간 손익은 <b>금액만</b> 있어서
 * "이게 많이 번 건지" 를 알 수 없었고, 옆의 두 비율 중 어느 것이 그 금액의 비율인지도 알 수 없었다. 셋은 <b>분모가 다른 별개의 값</b>이다.
 *
 * <p>실측 2026-08-27(실데이터):
 *
 * <pre>
 *   기간      자산 증가율   투자 수익률(TWR)   기간 손익률
 *   이번달      -1.19%        -1.18%          -1.19%
 *   올해       76.66%        92.88%          94.93%
 *   최근 1년   164.50%       192.92%         196.65%
 *   최근 3년   211.06%       193.48%         192.38%
 *   전체        계산 불가    1497.66%         190.18%
 * </pre>
 *
 * <p>'전체' 기간은 기초 평가액이 0 이라 증가율을 낼 수 없는데, 손익률은 나온다 &mdash; 분모가 <b>기초 평가액 + 기간 중 순유입 원금</b>이기 때문이다.
 *
 * <p>고점/저점은 차트 주석에만 있었다. 평가액이 0 인 날(보유가 하나도 없던 날)을 세면 저점이 늘 0 원이 되어 아무것도 말해 주지 않는다 &mdash; 실측 '전체'
 * 기간 6,170 일 중 1,772 일이 그렇다.
 */
class PeriodProfitRateAndExtremesTest {

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

  /** 기초 1,000(원가 1,000) → 중간에 고점 1,500·저점 800 → 기말 1,200. 유입 원금 없음. */
  private static List<TradeProfitTimeSeriesPoint> series() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 3, 2), "1000", "1000"));
    series.add(point(LocalDate.of(2026, 3, 3), "1500", "1000"));
    series.add(point(LocalDate.of(2026, 3, 4), "800", "1000"));
    series.add(point(LocalDate.of(2026, 3, 5), "1200", "1000"));
    return series;
  }

  @Test
  void 기간_손익률은_넣어_둔_돈으로_나눈다() {
    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series(), KST, false);

    // 기간 손익 +200, 분모 = 기초 평가액 1,000 + 유입 원금 0
    assertThat(summary.periodProfit()).isEqualByComparingTo("200");
    assertThat(summary.periodProfitRatePct())
        .as("기간 손익의 비율을 내지 않으면 헤드라인 금액이 많은 건지 알 수 없다")
        .isNotNull()
        .isCloseTo(20.0d, within(0.001d));
  }

  /**
   * 기초 평가액이 0 이어서 자산 증가율을 못 내는 구간에서도 손익률은 나온다.
   *
   * <p>실측 '전체' 기간이 그렇다 &mdash; 증가율은 계산 불가인데 손익률은 190.18% 다. 분모에 유입 원금이 들어가기 때문이다. 여기서 손익률까지 없으면 가장
   * 많이 보는 기간에 비율이 하나(TWR)만 남는다.
   */
  @Test
  void 기초가_0_이어도_유입_원금이_분모가_된다() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 3, 2), "0", "0"));
    series.add(point(LocalDate.of(2026, 3, 3), "1000", "1000"));
    series.add(point(LocalDate.of(2026, 3, 4), "1400", "1000"));

    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series, KST, false);

    assertThat(summary.growthRatePct()).as("기초가 0 이면 증가율은 내지 않는다").isNull();
    assertThat(summary.periodProfitRatePct())
        .as("증가율이 없는 구간에 손익률까지 없으면 비율이 TWR 하나만 남는다")
        .isNotNull()
        .isCloseTo(40.0d, within(0.001d));
  }

  @Test
  void 기간_중_고점과_저점을_날짜와_함께_낸다() {
    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series(), KST, false);

    assertThat(summary.peakValue()).isEqualByComparingTo("1500");
    assertThat(summary.peakValueDate()).isEqualTo(LocalDate.of(2026, 3, 3));
    assertThat(summary.troughValue()).isEqualByComparingTo("800");
    assertThat(summary.troughValueDate()).isEqualTo(LocalDate.of(2026, 3, 4));
  }

  /**
   * 보유가 없어 평가액이 0 이던 날은 저점 후보가 아니다.
   *
   * <p>세면 저점이 늘 0 원이 되어 "지금 대비" 를 낼 수도 없다. 실측 '전체' 기간에서 6,170 일 중 1,772 일이 평가액 0 이라, 예전 차트는 늘 "최저
   * 0원" 을 그렸다.
   */
  @Test
  void 평가액이_0_이던_날은_저점으로_세지_않는다() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 3, 2), "0", "0"));
    series.add(point(LocalDate.of(2026, 3, 3), "900", "900"));
    series.add(point(LocalDate.of(2026, 3, 4), "0", "0"));
    series.add(point(LocalDate.of(2026, 3, 5), "1300", "900"));

    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series, KST, false);

    assertThat(summary.troughValue())
        .as("평가액 0 인 날을 세면 저점이 늘 0 원이 되어 아무것도 말해 주지 않는다")
        .isEqualByComparingTo("900");
    assertThat(summary.troughValueDate()).isEqualTo(LocalDate.of(2026, 3, 3));
    assertThat(summary.peakValue()).isEqualByComparingTo("1300");
  }

  /** 보유가 한 번도 없었으면 고점/저점 자체가 없다. 0 을 내면 화면이 "최저 0원" 을 그린다. */
  @Test
  void 보유가_한_번도_없으면_고점과_저점이_없다() {
    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    series.add(point(LocalDate.of(2026, 3, 2), "0", "0"));
    series.add(point(LocalDate.of(2026, 3, 3), "0", "0"));

    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series, KST, false);

    assertThat(summary.peakValue()).isNull();
    assertThat(summary.troughValue()).isNull();
    assertThat(summary.peakValueDate()).isNull();
    assertThat(summary.troughValueDate()).isNull();
  }
}
