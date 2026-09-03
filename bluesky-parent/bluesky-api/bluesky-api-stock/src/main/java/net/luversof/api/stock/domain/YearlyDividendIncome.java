package net.luversof.api.stock.domain;

import java.math.BigDecimal;

/**
 * 연도별 배당 집계(조회 전용 투영). 지급일 기준이다.
 *
 * <p>세후는 컬럼이 아니라 계산값이다 &mdash; {@code Dividend.getNetAmount()} 가 {@code 세전 - 세금 - 수수료} 로 낸다. 그 규칙이
 * 두 군데로 갈리지 않게 여기서는 세 값을 그대로 담고, 뺄셈은 쓰는 쪽에서 한다.
 *
 * <p>{@code taxableAmount} 는 <b>시트에 적힌 과세금액</b>이다. 세전에 세율을 곱한 값이 아니다 &mdash; 계좌·종목에 따라 분리과세 혜택이 있어
 * 혜택을 받고 남은 몫만 과세되며, 그 값을 사람이 시트에 적어 둔다(실측 2026-08-24: KB증권 x KODEX 한국부동산리츠인프라 8 건은 과세금액이 세전의 0.75%
 * 수준이고 실효세율이 9.82% 였다).
 */
public record YearlyDividendIncome(
    int year, BigDecimal grossAmount, BigDecimal taxableAmount, BigDecimal tax, BigDecimal fee) {}
