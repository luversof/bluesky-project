package net.luversof.web.gate.stock.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 배당 메타(최초 기준일 + 배당 보유 종목 ID). 배당이 없으면 firstBasisDate 는 null.
 *
 * <p>합계는 여기서 받지 않는다 &mdash; 이 응답은 필터와 무관한 전체 메타라 계좌·기간을 걸어도 값이 그대로다. 필터가 반영된 세후 배당 합계는 {@code
 * DividendClient#findDividendTotal} 을 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendMetaResponse(Instant firstBasisDate, List<UUID> stockItemIds) {}
