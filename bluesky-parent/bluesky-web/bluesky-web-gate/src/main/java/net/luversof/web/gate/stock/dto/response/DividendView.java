package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendView(
    UUID id,
    UUID accountId,
    String accountName,
    UUID stockItemId,
    String stockItemName,
    Integer quantity,
    BigDecimal amountPerShare,
    BigDecimal grossAmount,
    BigDecimal tax,
    BigDecimal taxableAmount,
    /**
     * 주당 과세표준.
     *
     * <p>api-stock 이 이 값을 {@code taxPerShare} 라는 이름으로 내려주지만 <b>주당 세금이 아니다</b>. 실측 2026-08-24: 값이 있는
     * 배당 177 건 중 {@code 값 x 수량 = 세금} 인 것은 <b>0 건</b>이고, 과세표준과 세금이 모두 있는 80 건 중 {@code 값 x 수량 =
     * 과세표준} 인 것이 <b>72 건</b>이다. 남은 8 건(KODEX 한국부동산리츠인프라: 과세표준 2,233 = 29 x 77, 실제 수량은 10,256)은 분리과세
     * 혜택 뒤 남은 몫을 과세표준으로 적은 것이라 이 식에 안 맞는 것이 맞다. 처음엔 다른 수량으로 계산된 오류로 보고 원장 점검 규칙을 두었다가 오탐이라
     * 걷어냈다(api-stock LedgerIntegrityService 의 "원장(구글 시트)의 값을 그대로 믿는다" 절).
     *
     * <p>게이트는 이미 {@code 과세표준이 비어 있으면 이 값 x 수량} 으로 채우고 있었다 &mdash; 즉 코드는 과세표준으로 쓰면서 이름만 세금이었다. 이
     * 쪽에서는 이름을 바로잡아 들고 간다.
     */
    BigDecimal taxableBasePerShare,
    BigDecimal netAmount,
    Instant recordDate,
    Instant payDate,
    BigDecimal referencePrice,
    BigDecimal averageCostBasis,
    BigDecimal principalCost,
    BigDecimal principalMarketValue,
    BigDecimal yieldOnCostPct,
    BigDecimal yieldOnMarketPct) {}
