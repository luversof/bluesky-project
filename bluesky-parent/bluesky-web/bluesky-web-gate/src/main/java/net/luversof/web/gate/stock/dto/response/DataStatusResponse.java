package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 데이터 최신 시점. 관리 화면이 "언제까지의 데이터인지"를 서버 기준으로 보여주기 위한 값이다.
 *
 * <p>브라우저 로컬에 저장하던 '마지막 갱신 클릭 시각'과 달리, 다른 브라우저에서 보거나 갱신이 실패했을 때도 실제 채워진 범위를 알 수 있다.
 *
 * <p>가격의 마지막 일자만으로는 그 날 데이터가 진짜인지 알 수 없다. 거래가 없던 시점에 수집하면 KIS 가 직전 종가를 거래량 0 으로 실어 보내는데, 그 행이 들어가면
 * 화면의 "평가 기준" 일자가 실제보다 앞당겨진다(실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일). 그래서 직전
 * 거래일과의 동일 종목 수와 거래량 0 종목 수를 함께 받는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataStatusResponse(
    Instant tradeLastDate,
    long tradeCount,
    Instant dividendLastDate,
    long dividendCount,
    LocalDate priceHistoryLastDate,
    long stockItemCount,
    LocalDate priceHistoryPreviousDate,
    long priceHistoryItemCount,
    long priceHistorySameCloseCount,
    long priceHistorySameAllCount,
    long priceHistoryZeroVolumeCount,
    long priceHistoryRowCount,
    long priceHistoryZeroVolumeRowCount,
    long priceHistoryZeroVolumeChangedCloseCount,
    /**
     * 위 개수에 해당하는 행(최대 5건). 개수만으로는 수집 오류인지 액면분할 같은 정상 조정인지 알 수 없다.
     *
     * <p>실측 2026-08-23 의 유일한 행은 쌍방울 2025-05-08 로 종가가 13,450 에서 2,690 으로 정확히 1/5 이 됐다 &mdash;
     * 액면분할이다. 이 한 줄이 보이지 않으면 "거래량 0 행을 평가에서 빼도 되는가" 를 판단할 수 없다.
     */
    List<ZeroVolumeChangedCloseRow> priceHistoryZeroVolumeChangedCloseRows,
    /** 월배당 지급 이력의 가장 늦은 지급일. 이 참조 데이터도 사람이 가져와야 한다. */
    LocalDate monthlyDividendPayoutLastDate,
    /**
     * 제 주기를 넘겨 밀린 종목. 한 달 치가 비면 예상 월배당의 기준·평균 창·다가올 지급일이 함께 어긋나는데, 관리 화면은 매매·배당·시세의 최신 시점만 보여줘 알 수
     * 없었다(실측 2026-08-23: 월중 4종목이 34일 경과 / 자기 최대 33일).
     */
    List<MonthlyDividendPayoutOverdueRow> monthlyDividendPayoutOverdueRows,
    /**
     * 하루 만에 가격제한폭(±30%)을 넘어 움직인 시세 행의 개수. 거래로는 생길 수 없으므로 분할·병합이거나 수집 오류이고, 어느 쪽이든 가격 이력이 소급 조정되지
     * 않았다는 뜻이라 그 날 이전 구간의 평가액이 배율만큼 어긋난다.
     */
    long priceHistoryPriceLimitBreachCount,
    /** 위 개수에 해당하는 행(최대 5건). */
    List<PriceLimitBreachRow> priceHistoryPriceLimitBreachRows) {

  /** 지급 이력이 제 주기를 넘겨 밀린 종목. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MonthlyDividendPayoutOverdueRow(
      String stockItemName, LocalDate lastPayDate, int elapsedDays, int widestGapDays) {}

  /** 하루 만에 가격제한폭을 넘어 움직인 행. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PriceLimitBreachRow(
      String stockItemName,
      LocalDate tradeDate,
      LocalDate previousTradeDate,
      BigDecimal previousClosePrice,
      BigDecimal closePrice,
      double changePercent,
      int gapDays) {

    /**
     * 직전 종가 대비 배율.
     *
     * <p>제한폭을 넘었다는 것만으로는 원인을 가릴 수 없다 - 액면분할·병합일 수도 있고, 정리매매나 하한가처럼 제한폭이 적용되지 않거나 제한폭 그 자체인 정상 거래일
     * 수도 있다(실측 2026-08-24: 5행 중 쌍방울 2025-05-08 만 정확히 1/5 이고, 나머지 4행은 -30.0% ~ -67.1% 로 어떤 정수비에도 붙지
     * 않는다).
     */
    public BigDecimal closeRatio() {
      if (previousClosePrice == null
          || closePrice == null
          || previousClosePrice.compareTo(BigDecimal.ZERO) == 0) {
        return null;
      }
      return closePrice.divide(previousClosePrice, 4, java.math.RoundingMode.HALF_UP);
    }

    /** 분할·병합으로 설명되는 배율이면 그 비. 아니면 null - 그 경우는 시장에서 일어날 수 있는 변동이다. */
    public String splitRatioLabel() {
      return splitRatioLabelOf(closeRatio());
    }
  }

  /** 거래량 0 인데 종가가 바뀐 행. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ZeroVolumeChangedCloseRow(
      String stockItemName,
      LocalDate tradeDate,
      BigDecimal previousClosePrice,
      BigDecimal closePrice) {

    /**
     * 직전 종가 대비 배율. 두 숫자만 보면 데이터가 깨진 것처럼 보이지만, 배율이 깔끔한 정수비면 액면분할·병합이다.
     *
     * <p>실측 2026-08-23: 이 행은 하나뿐이고 쌍방울 2025-05-08 의 13,450 &rarr; 2,690 인데 배율이 정확히 1/5 이다(5:1
     * 액면분할). 배율을 함께 적지 않으면 사용자가 시세 수집이 깨진 것으로 오해한다.
     */
    public BigDecimal closeRatio() {
      if (previousClosePrice == null
          || closePrice == null
          || previousClosePrice.compareTo(BigDecimal.ZERO) == 0) {
        return null;
      }
      return closePrice.divide(previousClosePrice, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 액면분할·병합으로 설명되는 배율이면 그 비(예: {@code 1:5})를 돌려준다. 아니면 null.
     *
     * <p>1/2 ~ 1/20, 2 ~ 20 배까지만 본다. 이 범위 밖의 배율은 분할로 설명하기 어려우므로 단정하지 않는다.
     */
    public String splitRatioLabel() {
      return splitRatioLabelOf(closeRatio());
    }
  }

  /**
   * 배율이 액면분할·병합으로 설명되면 그 비(예: {@code 1:5})를, 아니면 null.
   *
   * <p>1/2 ~ 1/20, 2 ~ 20 배까지만 본다. 이 범위 밖은 분할로 설명하기 어려우므로 단정하지 않는다.
   *
   * <p>이 판정을 쓰는 자리가 둘이다 - 거래량 0 인데 종가가 바뀐 행과, 하루 변동이 제한폭을 넘은 행. 같은 식을 두 벌 두면 갈라지므로 한 곳에 둔다.
   */
  static String splitRatioLabelOf(BigDecimal ratio) {
    if (ratio == null) {
      return null;
    }
    for (int factor = 2; factor <= 20; factor++) {
      BigDecimal divisor = BigDecimal.valueOf(factor);
      if (ratio
              .subtract(BigDecimal.ONE.divide(divisor, 4, java.math.RoundingMode.HALF_UP))
              .abs()
              .compareTo(new BigDecimal("0.0005"))
          <= 0) {
        return "1:" + factor;
      }
      if (ratio.subtract(divisor).abs().compareTo(new BigDecimal("0.0005")) <= 0) {
        return factor + ":1";
      }
    }
    return null;
  }

  /**
   * 마지막 시세 일자가 '거래가 없던 날'로 의심되는지.
   *
   * <p>그 날 행이 있는 종목이 전부 거래량 0 이고 종가도 직전 거래일과 같다면, 그 날은 확정 종가가 없는 날이다. 한 종목만 그런 것은 거래가 없었을 뿐일 수 있으므로
   * 전 종목이 그럴 때만 의심한다.
   */
  /**
   * 거래량 0 행이 전체에서 차지하는 비율(%). 관리 화면의 데이터 품질 표시용.
   *
   * <p>실측 2026-08-22: 57,459 행 중 1,352 행(2.35%)이 거래량 0 이고, 그중 종가가 직전 행과 다른 것은 1 행뿐이었다. 즉 이 행들은 값을
   * 갖고 있지 않으므로 종가로 쓰지 않아도 평가액이 달라지지 않는다.
   */
  public double priceHistoryZeroVolumeRatioPercent() {
    return priceHistoryRowCount > 0
        ? priceHistoryZeroVolumeRowCount * 100.0 / priceHistoryRowCount
        : 0.0;
  }

  public boolean priceHistoryLastDateLooksUntraded() {
    return priceHistoryItemCount > 0
        && priceHistoryZeroVolumeCount == priceHistoryItemCount
        && priceHistorySameCloseCount == priceHistoryItemCount;
  }
}
