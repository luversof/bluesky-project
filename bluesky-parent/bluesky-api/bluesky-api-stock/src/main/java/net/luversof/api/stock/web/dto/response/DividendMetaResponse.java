package net.luversof.api.stock.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 배당 메타 정보(최초 기준일 + 배당 보유 종목 ID 목록).
 *
 * <p>게이트가 필터 UI 구성용으로 전 기간 배당 이력을 통째로 내려받던 것을 대체한다. 배당이 없으면 firstBasisDate 는 null, stockItemIds 는 빈
 * 목록이다.
 */
public record DividendMetaResponse(Instant firstBasisDate, List<UUID> stockItemIds) {}
