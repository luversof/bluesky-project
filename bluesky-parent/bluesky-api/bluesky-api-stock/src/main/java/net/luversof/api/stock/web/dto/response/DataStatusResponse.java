package net.luversof.api.stock.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 데이터 최신 시점. 관리 화면이 "언제까지의 데이터인지"를 보여주기 위한 값이다.
 *
 * <p>예전에는 브라우저 로컬에 저장한 '마지막 갱신 클릭 시각'만 보여줘서, 다른 브라우저에서 보거나 갱신이 실패했을 때 실제 데이터가 어디까지 채워졌는지 알 수 없었다.
 *
 * <p>가격 이력은 사용자별이 아니라 전 종목 공통이므로 사용자 조건 없이 최신 일자를 구한다.
 *
 * <p>가격의 '마지막 일자'만으로는 그 날 데이터가 진짜인지 알 수 없다. 수집이 자동이 아니라서(스케줄러도 CronJob 도 없다) 같은 값이 다른 날짜로 들어갈 수 있고,
 * 실제로 그런 일이 있었다(실측 2026-08-22: 2026-08-20 행이 보유 9종목 전부 2026-08-19 와 동일 - 다른 인접 거래일 쌍은 0/9). 그 날짜를
 * 화면이 "평가 기준"으로 단언하므로, 직전 거래일과의 동일 종목 수를 함께 돌려줘 관리 화면이 의심스러운 상태를 드러낼 수 있게 한다.
 */
public record DataStatusResponse(
    Instant tradeLastDate,
    long tradeCount,
    Instant dividendLastDate,
    long dividendCount,
    LocalDate priceHistoryLastDate,
    long stockItemCount,
    /** 마지막 시세 일자 직전에 시세가 있던 날(종목마다 다를 수 있어 그중 가장 늦은 날). */
    LocalDate priceHistoryPreviousDate,
    /** 마지막 시세 일자에 행이 있는 종목 수. */
    long priceHistoryItemCount,
    /** 그중 종가가 직전 거래일과 같은 종목 수. */
    long priceHistorySameCloseCount,
    /** 그중 시가/고가/저가/종가/거래량이 모두 직전 거래일과 같은 종목 수. 거래량까지 같으면 복제로 볼 근거가 된다. */
    long priceHistorySameAllCount,
    /** 마지막 시세 일자의 거래량이 0 인 종목 수. 전부 0 이면 장이 열리지 않은 날일 가능성이 크다. */
    long priceHistoryZeroVolumeCount,
    /** 전 구간 시세 행 수. */
    long priceHistoryRowCount,
    /** 그중 거래량이 0 인 행 수. 거래가 없던 시점에 수집된 행이다. */
    long priceHistoryZeroVolumeRowCount,
    /** 그중 종가가 직전 행과 다른 행 수. 거래가 없으면 종가가 바뀔 수 없으므로 0 에 가까워야 한다. */
    long priceHistoryZeroVolumeChangedCloseCount,
    /**
     * 위 개수에 해당하는 행(최대 5건).
     *
     * <p>개수만으로는 그것이 수집 오류인지 액면분할 같은 정상 조정인지 알 수 없고, 이 앱에는 시세 이력을 읽는 다른 경로가 없어 확인할 방법 자체가 없었다(실측
     * 2026-08-23: 관리 화면에 "1" 이라고만 떴다). 어느 종목의 어느 날인지 함께 돌려준다.
     */
    List<ZeroVolumeChangedCloseRow> priceHistoryZeroVolumeChangedCloseRows,
    /** 월배당 지급 이력의 가장 늦은 지급일. 이 참조 데이터도 사람이 가져와야 한다. */
    LocalDate monthlyDividendPayoutLastDate,
    /**
     * 제 주기를 넘겨 밀린 종목(최대 5건).
     *
     * <p>한 달 치가 비면 세 곳이 조용히 어긋난다 &mdash; 예상 월배당의 '최신 1개월' 기준, 12개월 평균의 창, 그리고 "다가올 배당" 카드의 예상
     * 지급일(지급일을 이력에서 산출하므로 빠진 달을 건너뛴 날짜가 나온다). 관리 화면은 매매·배당·시세의 최신 시점만 보여줘 이 참조가 밀린 것을 알 수 없었다.
     *
     * <p>임계값은 종목 자신의 과거 최대 간격이다. 그러면 월중(17일)·월말(익월 2일)처럼 주기가 다른 종목과 상장 초기의 불규칙을 그대로 흡수한다 (실측
     * 2026-08-23: 월중 4종목이 34일 경과인데 각자 과거 최대 33일이라 걸리고, 월말 4종목은 19일 경과 / 최대 34~94일이라 걸리지 않는다).
     */
    List<MonthlyDividendPayoutOverdueRow> monthlyDividendPayoutOverdueRows,
    /**
     * 하루 만에 가격제한폭(±30%)을 넘어 움직인 시세 행의 개수.
     *
     * <p>한국 시장의 일일 제한폭이 ±30% 이므로 거래로는 생길 수 없는 변동이다. 액면분할·병합·감자이거나 수집 오류이고, 어느 쪽이든 가격 이력이 소급 조정되지
     * 않았다는 뜻이라 그 날 이전 구간의 평가액이 배율만큼 통째로 어긋난다.
     *
     * <p>기존 점검({@code priceHistoryZeroVolumeChangedCloseCount})은 거래량 0 인 행만 본다. 분할은 보통 거래가 재개되며
     * 거래량이 붙어 그 그물에 걸리지 않는다 - 지금까지 걸린 유일한 행(쌍방울 2025-05-08, 정확히 1/5)은 거래량이 우연히 0 이었을 뿐이다.
     */
    long priceHistoryPriceLimitBreachCount,
    /** 위 개수에 해당하는 행(최대 5건). 며칠 만의 변동인지 보여야 거래정지 뒤 첫 거래와 가를 수 있다. */
    List<PriceLimitBreachRow> priceHistoryPriceLimitBreachRows) {

  /** 지급 이력이 제 주기를 넘겨 밀린 종목. */
  public record MonthlyDividendPayoutOverdueRow(
      String stockItemName, LocalDate lastPayDate, int elapsedDays, int widestGapDays) {}

  /**
   * 하루 만에 가격제한폭을 넘어 움직인 행. 종목 이름과 직전 거래일까지 붙인다.
   *
   * <p>{@code ratioPercent} 는 직전 종가 대비 변동률이다. 분할이면 -80%(1/5), -50%(1/2) 처럼 딱 떨어진다.
   */
  public record PriceLimitBreachRow(
      String stockItemName,
      LocalDate tradeDate,
      LocalDate previousTradeDate,
      java.math.BigDecimal previousClosePrice,
      java.math.BigDecimal closePrice,
      double changePercent,
      int gapDays) {}

  /** 거래량 0 인데 종가가 바뀐 행. 종목 이름까지 붙여 화면이 바로 읽을 수 있게 한다. */
  public record ZeroVolumeChangedCloseRow(
      String stockItemName,
      LocalDate tradeDate,
      java.math.BigDecimal previousClosePrice,
      java.math.BigDecimal closePrice) {}
}
