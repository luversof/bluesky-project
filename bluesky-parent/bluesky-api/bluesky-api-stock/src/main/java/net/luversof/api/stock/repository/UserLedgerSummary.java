package net.luversof.api.stock.repository;

import java.time.Instant;

/**
 * 사용자 원장의 "마지막 일자 + 건수" 한 쌍.
 *
 * <p>둘을 따로 물으면 같은 조인을 두 번 훑는다(실측: {@code /api/dataStatus} 한 요청이 SQL 5건 · DB 왕복 7회). 관리 화면의 데이터 최신
 * 시점 표시에만 쓰는 값이라 한 번에 읽는다.
 *
 * <p>{@code MAX} 는 NULL 을 무시하므로 예전 쿼리의 {@code IS NOT NULL} 조건과 결과가 같고, {@code COUNT(*)} 는 조건 없이 세던
 * 예전 쿼리와 같다.
 */
public record UserLedgerSummary(Instant lastDate, long totalCount) {}
