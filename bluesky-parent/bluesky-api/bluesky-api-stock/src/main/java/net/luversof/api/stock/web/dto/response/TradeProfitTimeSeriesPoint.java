package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 손익 시계열의 한 지점.
 *
 * <p>{@code date} 는 응답에 나가지 않는 내부 값이다. 시뮬레이션은 이미 '거래일'을 {@link LocalDate} 로 들고 있는데, 그것을 {@code
 * timestamp} 로 바꿔 담고 나면 요약·연도별·다운샘플이 저마다 다시 {@code atZone(zone).toLocalDate()} 로 되돌려 왔다. 되돌리는 비용만 이
 * 엔드포인트의 8%였다(실측). 원래 값을 그대로 들고 다니면 그 변환이 통째로 사라지고, 직렬화 결과는 그대로다.
 */
public record TradeProfitTimeSeriesPoint(
    Instant timestamp,
    BigDecimal cumulativeRealizedProfit,
    BigDecimal dailyRealizedProfit,
    long tradeCount,
    long buyCount,
    long tradeVolume,
    BigDecimal totalHoldingsValue,
    BigDecimal totalHoldingsCost,
    BigDecimal cumulativeTotalProfit,
    BigDecimal cumulativeDividend,
    @JsonIgnore LocalDate date,
    @JsonIgnore BigDecimal cumulativeNetCashFlow,
    @JsonIgnore BigDecimal cumulativeGrossInflow) {

  /**
   * 누적 순현금흐름(산 돈 + 수수료 - 판 돈 순수취액)을 모르는 지점. 시간가중 수익률은 이때 원가 변동 + 실현손익으로 대신 잰다.
   *
   * <p>{@code cumulativeNetCashFlow} 는 응답에 나가지 않는 내부 값이다 - 시간가중 수익률(요약)이 원가 기준과 무관하게 현금으로 하루 손익을
   * 재려고 들고 다닌다. 실현손익은 증권사 기록값이라 장부 원가와 다를 수 있어, 원가 변동 + 실현손익으로 재면 그 차가 판 날의 손익으로 튄다(실측 2026-10-01
   * PLUS 고배당주 2025-10-22 하루 -19.23%, 현금 기준 +1.47%).
   */
  public TradeProfitTimeSeriesPoint(
      Instant timestamp,
      BigDecimal cumulativeRealizedProfit,
      BigDecimal dailyRealizedProfit,
      long tradeCount,
      long buyCount,
      long tradeVolume,
      BigDecimal totalHoldingsValue,
      BigDecimal totalHoldingsCost,
      BigDecimal cumulativeTotalProfit,
      BigDecimal cumulativeDividend,
      LocalDate date) {
    this(
        timestamp,
        cumulativeRealizedProfit,
        dailyRealizedProfit,
        tradeCount,
        buyCount,
        tradeVolume,
        totalHoldingsValue,
        totalHoldingsCost,
        cumulativeTotalProfit,
        cumulativeDividend,
        date,
        null,
        null);
  }

  /**
   * 누적 총 매수액(산 돈 + 수수료, 판 돈은 빼지 않는다)을 모르는 지점. 순현금흐름만으로는 보유가 없던 날 사고판 돈을 못 가른다 - 같은 날 사고팔아 번 날은
   * 순현금흐름이 음수라 그날 운용한 돈(분모)이 0 으로 읽혔다(실측 2026-10-02 미코 · 웰크론 · 제일바이오 · 코미팜 · CJ씨푸드 수익률 0%, 실제
   * 0.5~1.25%).
   */
  public TradeProfitTimeSeriesPoint(
      Instant timestamp,
      BigDecimal cumulativeRealizedProfit,
      BigDecimal dailyRealizedProfit,
      long tradeCount,
      long buyCount,
      long tradeVolume,
      BigDecimal totalHoldingsValue,
      BigDecimal totalHoldingsCost,
      BigDecimal cumulativeTotalProfit,
      BigDecimal cumulativeDividend,
      LocalDate date,
      BigDecimal cumulativeNetCashFlow) {
    this(
        timestamp,
        cumulativeRealizedProfit,
        dailyRealizedProfit,
        tradeCount,
        buyCount,
        tradeVolume,
        totalHoldingsValue,
        totalHoldingsCost,
        cumulativeTotalProfit,
        cumulativeDividend,
        date,
        cumulativeNetCashFlow,
        null);
  }
}
