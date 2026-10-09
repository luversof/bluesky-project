package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 종목 상세 주가 차트 한 점(2026-10-02, 사용자 요청: 캔들 + 그 시점 평균 단가).
 *
 * <p>가격 넷은 원주가를 분할 · 병합 배율로만 맞춘 값이다(StockPriceChartService). averageCost 는 그 날 보유분의 평균 단가(이동평균법)를
 * 같은 단위로 맞춘 값이고, 보유가 없던 날 · 사용자를 주지 않은 요청이면 null 이다. distribution 은 그 날이 분배락일(지급기준일 전 거래일)이면 주당
 * 분배금(같은 단위), 아니면 null - 원주가 차트에서 매달 분배락 하락이 왜 생겼는지 보이게 한다(월배당 지급 이력이 있는 종목만).
 */
public record StockPriceChartPoint(
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal averageCost,
    BigDecimal distribution) {}
