package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 종목별 최초 매수 시점(조회 전용 투영).
 *
 * <p>보유 기간의 시작은 <b>처음 산 날</b>이다 &mdash; 매도는 세지 않는다. 자산 현황이 "얼마나 오래 들고 있나"를 적으려면 종목마다 이 날짜가 필요한데, 원장
 * 전체(실측 2026-09-14: 258 행)를 받아 화면에서 min() 하는 대신 DB 집계로 종목 수만큼만 가져온다.
 */
public record StockItemFirstBuy(UUID stockItemId, Instant firstBuyDate) {}
