package net.luversof.api.stock.domain;

/**
 * 보유 기간 원주가(수정 전 종가)가 얼마나 채워졌는지 - 관리 화면 데이터 상태(2026-10-02).
 *
 * <p>평가액은 원주가 x 실제 주식 수다(TradeProfitService.RawValuation). 빈 날은 직전 날 배율로 메우지만, 빈 날이 쌓이면 그 메운 값이 평가에
 * 쓰인다 - 몇 날이 비었는지 보여 준다. 보유 기간 = 종목별 첫 매매일부터, 다 판 종목은 마지막 매매일까지(거래가 있던 날만).
 */
public record RawCloseCoverage(long dayCount, long missingDayCount) {}
