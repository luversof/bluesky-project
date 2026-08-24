package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendResponse(
    UUID id,
    UUID accountId,
    UUID stockItemId,
    String stockItemName,
    String type,
    Integer quantity,
    BigDecimal amountPerShare,
    /**
     * 주당 <b>과세표준</b>. 이름과 달리 주당 세금이 아니다.
     *
     * <p>실측 2026-08-24: 값이 있는 배당 177 건 중 {@code 값 x 수량 = 세금} 인 것은 <b>0 건</b>이고, 과세표준·세금이 모두 있는 80 건
     * 중 {@code 값 x 수량 = 과세표준} 인 것이 <b>72 건</b>이다. 남은 8 건은 기록된 과세표준 쪽이 다른 수량으로 계산된 것으로, 원장 점검의
     * {@code DIVIDEND_TAXABLE_COMPUTED_WITH_OTHER_QUANTITY} 가 이미 잡는다.
     *
     * <p>이름을 바꾸면 저장 컬럼과 이 응답을 쓰는 쪽이 함께 흔들리므로 여기서는 뜻만 적는다. 게이트 화면 모델은 {@code taxableBasePerShare} 로
     * 받아 이름을 바로잡는다.
     */
    BigDecimal taxPerShare,
    BigDecimal grossAmount,
    BigDecimal fee,
    BigDecimal tax,
    BigDecimal taxableAmount,
    BigDecimal netAmount,
    Instant recordDate,
    Instant payDate) {}
