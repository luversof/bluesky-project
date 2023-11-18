package net.luversof.web.gate.feign.bookkeeping.domain;

import lombok.Builder;
import net.luversof.web.gate.bookkeeping.constant.AssetGroupType;

@Builder(toBuilder = true)
public record AssetGroup(long idx, String assetGroupId, String bookkeepingId, AssetGroupType assetGroupType, String name) {

}
