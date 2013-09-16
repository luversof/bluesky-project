package net.luversof.asset.domain;

import lombok.Getter;

public enum AssetType {
	WALLET(1),
	BANK(2),
	CARD(3),
	CHECKCARD(4),
	LOAN(5);

	@Getter
	private long id;
	
	private AssetType(long id) {
		this.id = id;
	}
}