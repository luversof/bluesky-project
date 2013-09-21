package net.luversof.bookkeeping.domain;

import lombok.Getter;

/**
 * 기록 유형
 * @author bluesky
 *
 */
public enum EntryType {
	DEBIT(1),	//인출
	CREDIT(2);	//입금
	
	@Getter
	private long id;
	
	private EntryType(long id) {
		this.id = id;
	}
}
