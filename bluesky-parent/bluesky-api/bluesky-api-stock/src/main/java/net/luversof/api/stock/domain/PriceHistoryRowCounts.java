package net.luversof.api.stock.domain;

/**
 * 시세 행 수 한 줄. 전체와 그중 거래량 0 인 행을 한 번의 스캔으로 함께 센다.
 *
 * <p>따로 물으면 같은 57,586 행을 두 번 훑는다.
 *
 * <p>{@code lastDateItemCount} 는 그 마지막 일자에 행이 있는 종목 수, {@code noHistoryItemCount} 는 시세 이력이 하나도 없는
 * 종목 수다. 최신 일자 하나만 보여 주면 전 종목이 그 날까지 최신인 것으로 읽힌다 - 실측 2026-09-12: 86 종목 중 9 종목만 2026-09-09 이고 70
 * 종목은 2026-04-01/03 에 멈춰 있으며 4 종목은 이력이 아예 없다.
 */
public record PriceHistoryRowCounts(
    long totalCount, long zeroVolumeCount, long lastDateItemCount, long noHistoryItemCount) {}
