package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 종목별로 실제로 오간 돈 한 건(조회 전용 투영). 들어온 돈은 양수, 나간 돈은 음수.
 *
 * <p>자산 현황 · 종목 상세의 연평균 수익률(XIRR)이 쓴다 &mdash; 최초 매수일 하나로는 나눠 산 종목의 돈이 언제 들어갔는지 알 수 없다(실측
 * 2026-09-17: 삼성전자는 17 번에 나눠 사서, 최초 매수일부터 전액이 들어가 있었다고 보는 복리 환산이 23.6% 인데 실제 날짜로는 31.8%).
 */
public record StockItemCashFlow(UUID stockItemId, Instant flowAt, BigDecimal amount) {}
