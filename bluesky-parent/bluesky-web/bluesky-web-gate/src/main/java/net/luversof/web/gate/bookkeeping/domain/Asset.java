package net.luversof.web.gate.bookkeeping.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record Asset(long idx, String assetId, String bookkeepingId, String assetGroupId, String name) {

}
