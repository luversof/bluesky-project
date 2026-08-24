package net.luversof.web.gate.stock.dto.response;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 필터 목록용 계좌·종목 id 집계 응답. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityFilterIdsResponse(
    List<UUID> tradeAccountIds,
    List<UUID> tradeStockItemIds,
    List<UUID> dividendAccountIds,
    List<UUID> dividendStockItemIds) {}
