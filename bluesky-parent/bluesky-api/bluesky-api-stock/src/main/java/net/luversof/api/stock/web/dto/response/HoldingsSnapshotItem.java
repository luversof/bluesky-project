package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HoldingsSnapshotItem(
    UUID stockItemId,
    String name,
    String symbol,
    BigDecimal quantity,
    BigDecimal avgCost,
    BigDecimal priceAtDate,
    /**
     * {@code priceAtDate} 가 실제로 어느 날 종가인지.
     *
     * <p>요청한 날짜에 시세가 없으면 그 이전 마지막 종가를 쓰기 때문에, 이 값이 요청 날짜보다 이를 수 있다(실측: 2026-08-22 를 물으면 수집이
     * 2026-08-20 에서 멈춰 있어 그 날 종가가 나온다). 화면이 "고른 날짜의 시세"로 오해하지 않도록 함께 돌려준다. 그 종목의 종가를 한 번도 만나지 못했으면
     * {@code null} 이다.
     */
    LocalDate priceDate,
    BigDecimal value,
    BigDecimal unrealizedProfit) {}
