package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 차트에 쓰는 일별 종가 한 점.
 *
 * <p>조회 대상 종목은 호출자가 이미 알고 있으므로 종목 id 를 행마다 싣지 않는다 &mdash; 실측 2026-09-01(삼성전자 전 구간 1,593 행): id 를 빼면
 * 응답이 154.2 KB 에서 3 분의 1 남짓으로 줄어든다. 같은 값을 1,593 번 보내는 셈이었다.
 */
public record StockPriceHistoryPoint(LocalDate tradeDate, BigDecimal closePrice) {}
