package net.luversof.bookkeeping.domain;

import lombok.Getter;

/**
 * 자산 유형
 * @author bluesky
 *
 */
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
