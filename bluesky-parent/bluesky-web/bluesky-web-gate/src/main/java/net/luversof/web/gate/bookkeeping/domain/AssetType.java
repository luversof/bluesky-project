package net.luversof.web.gate.bookkeeping.domain;

import java.util.UUID;

import lombok.Builder;
import net.luversof.web.gate.bookkeeping.constant.AssetTypeCode;

@Builder(toBuilder = true)
public record AssetType(
	UUID id,
	UUID bookkeepingId,
	AssetTypeCode code,
	String name 
) {

}
