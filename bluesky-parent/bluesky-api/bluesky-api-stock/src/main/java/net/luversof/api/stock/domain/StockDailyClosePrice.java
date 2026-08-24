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
public record StockDailyClosePrice(UUID stockItemId, LocalDate tradeDate, BigDecimal closePrice) {}
