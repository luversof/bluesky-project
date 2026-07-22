package net.luversof.web.gate.stock.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 배당 메타(최초 기준일 + 배당 보유 종목 ID). 배당이 없으면 firstBasisDate 는 null. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendMetaResponse(Instant firstBasisDate, List<UUID> stockItemIds) {}
