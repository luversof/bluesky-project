package net.luversof.api.stock.domain;

import java.time.LocalDate;

/**
 * 가장 최근 시세 일자가 직전 거래일의 복제인지 판정하는 데 쓰는 집계.
 *
 * <p>실측 2026-08-22: 보유 9종목의 2026-08-20 종가가 2026-08-19 와 <b>전 종목 동일</b>했다(다른 인접 거래일 쌍은 모두 0/9 동일).
 * 서로 다른 종목이 이틀 연속 같은 값으로 마감할 수는 없으므로, 그 날짜의 행은 실제 그 날 시세가 아니다. 그런데도 화면은 "평가 기준 2026-08-20 종가"라고 적어
 * 있지도 않은 최신성을 단언한다.
 *
 * <p>시가/고가/저가/거래량까지 함께 비교한다. 종가만 같은 것은 우연히 일어날 수 있지만 거래량까지 같으면 복제로 볼 근거가 된다.
 */
public record PriceHistoryDuplicateSummary(
    LocalDate tradeDate,
    LocalDate previousTradeDate,
    long itemCount,
    long sameCloseCount,
    long sameAllCount,
    /** 그 날 거래량이 0 인 종목 수. 전 종목이 0 이면 그 날은 장이 열리지 않은 날일 가능성이 크다. */
    long zeroVolumeCount) {}
