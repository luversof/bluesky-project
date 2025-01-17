package net.luversof.web.gate.bookkeeping.domain;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record Asset(
	UUID id,
	Bookkeeping bookkeeping,
	AssetType assetType, 
	List<Integer> bitConfigIndexList, 
	String name
) {

}
