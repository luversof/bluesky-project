package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 시간가중 수익률(TWR)에서 그날 들어온 돈은 그날 처음부터 운용된 것으로 본다.
 *
 * <p>예전에는 하루 수익률의 분모가 전날 평가액뿐이라, 작은 보유 위에 크게 산 날 체결가와 종가의 차가 전체 수익률로 읽혔다. 실측 2026-10-01 KODEX
 * 한국부동산리츠인프라: 1/5 보유 2,050,200 위에 25,865,180 을 사고 그날 종가 기준 -318,074 &rarr; 하루 -15.51%, 1월 수익률
 * -16.90% (가격은 한 달 1% 안쪽). 포트폴리오 '전체' 투자 수익률도 1561% 로 부풀어 있었다(고친 뒤 804%).
 */
class TimeWeightedInflowDayTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private TradeProfitTimeSeriesPoint point(
      LocalDate date, String value, String cost, String realized) {
    BigDecimal holdingsValue = new BigDecimal(value);
    BigDecimal holdingsCost = new BigDecimal(cost);
    BigDecimal cumulativeRealized = new BigDecimal(realized);
    return new TradeProfitTimeSeriesPoint(
        date.atStartOfDay(KST).toInstant(),
        cumulativeRealized,
        BigDecimal.ZERO,
        0L,
        0L,
        0L,
        holdingsValue,
        holdingsCost,
        holdingsValue.subtract(holdingsCost).add(cumulativeRealized),
        BigDecimal.ZERO,
        date);
  }

  /** 실측 그대로: 1/2 보유 2,050,200(원가 2,036,745), 1/5 원가 27,901,925 · 평가 27,597,306. */
  @Test
  void 큰_매수일의_손익은_그날_들어온_돈까지_합친_분모로_나눈다() {
    TradeProfitTimeSeriesSummary summary =
        TradeProfitService.summarizeSeries(
            List.of(
                point(LocalDate.of(2026, 1, 2), "2050200", "2036745", "0"),
                point(LocalDate.of(2026, 1, 5), "27597306", "27901925", "0")),
            KST,
            false);

    // -318,074 / (2,050,200 + 25,865,180) = -1.1394% (예전 -318,074 / 2,050,200 = -15.51%)
    assertEquals(-1.1394d, summary.timeWeightedReturnPct(), 0.0001d);
  }

  /** 판 날은 판 돈이 그날 끝까지 운용됐다 - 분모는 전날 평가액 그대로다(나간 돈을 분모에 더하거나 빼면 틀린다). */
  @Test
  void 매도일의_분모는_전날_평가액이다() {
    TradeProfitTimeSeriesSummary summary =
        TradeProfitService.summarizeSeries(
            List.of(
                point(LocalDate.of(2026, 1, 2), "1000", "1000", "0"),
                // 절반(원가 500)을 520 에 팔아 실현 +20, 남은 절반이 520 으로 평가
                point(LocalDate.of(2026, 1, 5), "520", "500", "20")),
            KST,
            false);

    // 손익 = 520 - 1000 - (-500) + 20 = 40 -> 40 / 1000 = 4%
    assertEquals(4.0d, summary.timeWeightedReturnPct(), 0.0001d);
  }

  private TradeProfitTimeSeriesPoint withCash(TradeProfitTimeSeriesPoint p, String cumulativeCash) {
    return new TradeProfitTimeSeriesPoint(
        p.timestamp(),
        p.cumulativeRealizedProfit(),
        p.dailyRealizedProfit(),
        p.tradeCount(),
        p.buyCount(),
        p.tradeVolume(),
        p.totalHoldingsValue(),
        p.totalHoldingsCost(),
        p.cumulativeTotalProfit(),
        p.cumulativeDividend(),
        p.date(),
        new BigDecimal(cumulativeCash));
  }

  /**
   * 실현손익은 증권사 기록값이라 장부 원가와 다를 수 있다. 현금흐름을 알면 하루 손익을 현금으로 잰다 - 원가 기준이 어긋나도 판 날이 튀지 않는다. 실측
   * 2026-10-01 PLUS 고배당주 2025-10-22: 장부 원가 5,966,415 인 403 주를 증권사는 실현 +405,412 로 기록 -> 하루 -19.23%,
   * 현금 기준 +1.47%.
   */
  @Test
  void 현금흐름을_알면_실현손익의_원가_기준과_무관하다() {
    // 평가 1,000(장부 원가 800)을 전량 1,050 에 팔았다. 증권사는 원가를 1,000 으로 봐 실현 +50.
    var before = point(LocalDate.of(2026, 1, 2), "1000", "800", "0");
    var after = point(LocalDate.of(2026, 1, 5), "0", "0", "50");

    TradeProfitTimeSeriesSummary costBased =
        TradeProfitService.summarizeSeries(List.of(before, after), KST, false);
    // 원가 변동 + 실현손익: 0 - 1000 + 800 + 50 = -150 -> -15% (판 날이 튄다)
    assertEquals(-15.0d, costBased.timeWeightedReturnPct(), 0.0001d);

    TradeProfitTimeSeriesSummary cashBased =
        TradeProfitService.summarizeSeries(
            List.of(withCash(before, "800"), withCash(after, "-250")), KST, false);
    // 현금: 0 - 1000 - (-1050) = +50 -> +5%
    assertEquals(5.0d, cashBased.timeWeightedReturnPct(), 0.0001d);
  }

  /** 현금 기준에서도 산 날은 그날 산 돈까지 분모에 들어간다. */
  @Test
  void 현금_기준_매수일도_분모에_산_돈을_더한다() {
    TradeProfitTimeSeriesSummary summary =
        TradeProfitService.summarizeSeries(
            List.of(
                withCash(point(LocalDate.of(2026, 1, 2), "2050200", "2036745", "0"), "2036745"),
                withCash(point(LocalDate.of(2026, 1, 5), "27597306", "27901925", "0"), "27901925")),
            KST,
            false);
    assertEquals(-1.1394d, summary.timeWeightedReturnPct(), 0.0001d);
  }

  /**
   * 보유가 없던 다음 날의 첫 매수도 센다 - 분모는 그날 들어간 돈. 실측 2026-10-02 하이브 꼴: 공모가 135,000 x 2 주(270,000)를 사서 상장일
   * 종가 255,420 x 2 = 510,840, 이튿날 다 팔아 345,000. 예전에는 첫날을 건너뛰어 -32.5%(상장일 종가부터), 지금은 +27.8%(실제).
   */
  @Test
  void 첫_매수일의_손익도_센다() {
    TradeProfitTimeSeriesSummary summary =
        TradeProfitService.summarizeSeries(
            List.of(
                withCash(point(LocalDate.of(2020, 10, 14), "0", "0", "0"), "0"),
                withCash(point(LocalDate.of(2020, 10, 15), "510840", "270000", "0"), "270000"),
                withCash(point(LocalDate.of(2020, 10, 16), "0", "0", "75000"), "-75000")),
            KST,
            false);
    // 첫날 (510,840 - 270,000) / 270,000 = +89.2%, 이튿날 (0 - 510,840 + 345,000) / 510,840 = -32.46% ->
    // 곱 +27.78%
    assertEquals(27.7778d, summary.timeWeightedReturnPct(), 0.001d);
  }

  private TradeProfitTimeSeriesPoint withGross(
      TradeProfitTimeSeriesPoint p, String cumulativeCash, String cumulativeGross) {
    return new TradeProfitTimeSeriesPoint(
        p.timestamp(),
        p.cumulativeRealizedProfit(),
        p.dailyRealizedProfit(),
        p.tradeCount(),
        p.buyCount(),
        p.tradeVolume(),
        p.totalHoldingsValue(),
        p.totalHoldingsCost(),
        p.cumulativeTotalProfit(),
        p.cumulativeDividend(),
        p.date(),
        new BigDecimal(cumulativeCash),
        new BigDecimal(cumulativeGross));
  }

  /**
   * 같은 날 사고팔면 그날 운용한 돈은 산 돈이다. 순현금흐름(산 돈 - 판 돈)은 번 날 음수라 분모가 0 으로 읽혀 그날을 통째로 건너뛰었다(실측 2026-10-02 미코
   * · 웰크론 · 제일바이오 · 코미팜 · CJ씨푸드 수익률 0%, 실제 0.5~1.25%).
   */
  @Test
  void 같은_날_사고판_손익은_산_돈으로_나눈다() {
    // 1,000 에 사서 같은 날 1,050 에 팔았다 - 저녁에는 보유 0, 순현금흐름 -50, 총 매수 1,000
    var series = List.of(withGross(point(LocalDate.of(2026, 1, 2), "0", "0", "50"), "-50", "1000"));
    assertEquals(
        5.0d,
        TradeProfitService.summarizeSeries(series, KST, true).timeWeightedReturnPct(),
        0.0001d);

    // 보유가 있던 날의 사고팔기는 예전 그대로(분모 = 전날 평가액 + 순유입)
    var held =
        List.of(
            withGross(point(LocalDate.of(2026, 1, 2), "1000", "1000", "0"), "1000", "1000"),
            withGross(point(LocalDate.of(2026, 1, 5), "1000", "1000", "50"), "950", "2000"));
    // 손익 = 1000 - 1000 - (-50) = 50, 분모 = 1000 + max(-50, 0) = 1000 -> 5%
    assertEquals(
        5.0d,
        TradeProfitService.summarizeSeries(held, KST, false).timeWeightedReturnPct(),
        0.0001d);
  }

  /**
   * 종목 하나의 시계열은 매수일이 첫 점이다(앞에 0 인 날이 없다). 이력 맨 앞부터(zeroOpening)면 그 앞을 0 으로 본다 - 실측 2026-10-02 하이브는
   * 이 꼴이라 위 고침 뒤에도 -33.3% 였다(원주가 평가 516,000 = 상장일 종가 258,000 x 2).
   */
  @Test
  void 매수일이_첫_점이어도_이력_맨_앞이면_센다() {
    var series =
        List.of(
            withCash(point(LocalDate.of(2020, 10, 15), "516000", "270000", "0"), "270000"),
            withCash(point(LocalDate.of(2020, 12, 15), "0", "0", "75000"), "-75000"));
    assertEquals(
        27.7778d,
        TradeProfitService.summarizeSeries(series, KST, true).timeWeightedReturnPct(),
        0.001d);
    // 이력 중간에서 자른 구간이면 첫 점은 기초다 - 예전과 같다(첫날을 세지 않는다).
    assertEquals(
        -33.1395d,
        TradeProfitService.summarizeSeries(series, KST, false).timeWeightedReturnPct(),
        0.001d);
  }
}
