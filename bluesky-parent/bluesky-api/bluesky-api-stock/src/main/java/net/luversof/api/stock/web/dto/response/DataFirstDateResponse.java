package net.luversof.api.stock.web.dto.response;

import java.time.Instant;

/**
 * 사용자의 최초 데이터 일자. 날짜 선택기 하한(minDate) 계산용.
 *
 * <p>화면마다 전체 거래/배당 이력을 내려받아 min() 하던 것을 대체한다. 값이 없으면(거래/배당 없음) 각 필드는 null 이다.
 */
public record DataFirstDateResponse(Instant tradeFirstDate, Instant dividendFirstDate) {}
