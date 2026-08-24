package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 손익 시계열의 한 지점.
 *
 * <p>{@code date} 는 응답에 나가지 않는 내부 값이다. 시뮬레이션은 이미 '거래일'을 {@link LocalDate} 로 들고 있는데, 그것을 {@code
 * timestamp} 로 바꿔 담고 나면 요약·연도별·다운샘플이 저마다 다시 {@code atZone(zone).toLocalDate()} 로 되돌려 왔다. 되돌리는 비용만 이
 * 엔드포인트의 8%였다(실측). 원래 값을 그대로 들고 다니면 그 변환이 통째로 사라지고, 직렬화 결과는 그대로다.
 */
public record TradeProfitTimeSeriesPoint(
    Instant timestamp,
    BigDecimal cumulativeRealizedProfit,
    BigDecimal dailyRealizedProfit,
    long tradeCount,
    long buyCount,
    long tradeVolume,
    BigDecimal totalHoldingsValue,
    BigDecimal totalHoldingsCost,
    BigDecimal cumulativeTotalProfit,
    BigDecimal cumulativeDividend,
    @JsonIgnore LocalDate date) {}
