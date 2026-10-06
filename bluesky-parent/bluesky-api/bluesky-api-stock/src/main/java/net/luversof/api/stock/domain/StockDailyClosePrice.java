package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 일별 종가만 담는 조회 전용 투영.
 *
 * <p>손익 시뮬레이션은 종목/일자/종가 세 값만 쓰는데, 엔티티로 읽으면 시가·고가·저가·거래량·id·갱신일까지 9개 컬럼을 전부 매핑한다. 조회 구간이 전체 보유 이력이라
 * 행 수가 커서 그 차이가 응답 시간을 지배한다.
 */
public record StockDailyClosePrice(
    UUID stockItemId, LocalDate tradeDate, BigDecimal closePrice, java.time.Instant updatedDate) {

  /**
   * 갱신 시각까지(최근 종가 조회만 채운다, 2026-10-02). 시세 갱신이 장중이면 그 날 "종가" 는 장중 값이라 화면이 "종가" 라고 부르면 안 된다 &mdash;
   * 실측: 10:16 에 받은 행이 장 마감 뒤까지 남아 종일 "평가 기준 2026-10-02 종가" 로 보였다.
   */
  @org.springframework.data.annotation.PersistenceCreator
  public StockDailyClosePrice {}

  /** 갱신 시각 없이(일별 종가 구간 조회 · 시험). */
  public StockDailyClosePrice(UUID stockItemId, LocalDate tradeDate, BigDecimal closePrice) {
    this(stockItemId, tradeDate, closePrice, null);
  }
}
