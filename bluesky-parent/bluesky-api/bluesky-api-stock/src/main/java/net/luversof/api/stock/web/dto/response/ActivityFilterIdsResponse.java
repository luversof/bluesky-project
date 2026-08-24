package net.luversof.api.stock.web.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 필터 목록에 쓰는 '해당 기간에 등장한' 계좌·종목 id.
 *
 * <p>게이트가 전체 거래/배당 목록을 통째로 내려받아 id 를 뽑던 것을 대체한다(실측: 거래 250건 82KB 를 받아 id 만 쓰고 버렸다).
 */
public record ActivityFilterIdsResponse(
    List<UUID> tradeAccountIds,
    List<UUID> tradeStockItemIds,
    List<UUID> dividendAccountIds,
    List<UUID> dividendStockItemIds) {}
