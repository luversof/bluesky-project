package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HoldingsSnapshotItem(
    UUID stockItemId,
    String name,
    String symbol,
    BigDecimal quantity,
    BigDecimal avgCost,
    BigDecimal priceAtDate,
    /**
     * {@code priceAtDate} 가 실제로 어느 날 종가인지. 고른 날짜에 시세가 없으면 그 이전 마지막 종가를 쓰므로 요청 날짜보다 이를 수 있다.
     *
     * <p>실측: 2026-08-22 를 고르면 수집이 2026-08-20 에서 멈춰 있어 그 날 종가가 나온다. 표에 "그 날 시세"로 보이면 안 되므로 화면이 이 값을
     * 함께 밝힌다.
     */
    LocalDate priceDate,
    BigDecimal value,
    BigDecimal unrealizedProfit) {}
