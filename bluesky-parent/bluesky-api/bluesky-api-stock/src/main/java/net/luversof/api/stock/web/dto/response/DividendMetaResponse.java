package net.luversof.api.stock.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 배당 메타 정보(최초 기준일 + 배당 보유 종목 ID 목록).
 *
 * <p>게이트가 필터 UI 구성용으로 전 기간 배당 이력을 통째로 내려받던 것을 대체한다. 배당이 없으면 firstBasisDate 는 null, stockItemIds 는 빈
 * 목록이다.
 *
 * <p>여기에는 <b>합계를 두지 않는다.</b> 이 응답은 필터와 무관한 사용자 전체 메타라, 예전에 있던 {@code totalNetAmount} 는 계좌·기간을 걸어도 늘
 * 전체 값이었다. 요약 화면이 그것을 '누적 확정 수익'으로 쓰는 바람에 필터를 건 실현손익과 어긋났다(실측: KB증권 위탁에서 10,113,820 이어야 할 자리에
 * 61,646,257). 필터가 반영된 합계는 {@code GET /api/dividend/total} 이 손익 조회와 같은 파라미터로 낸다.
 */
public record DividendMetaResponse(Instant firstBasisDate, List<UUID> stockItemIds) {}
