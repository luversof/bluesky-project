package net.luversof.api.bookkeeping.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntryTypeCode {

	INCOME(1),
	OUTGOING(2),
	TRANSFER(3),
	;
	
	private int code;

	public static EntryTypeCode findByCode(int code) {
		for (var entryTypeCode : EntryTypeCode.values()) {
			if (entryTypeCode.getCode() == code) {
				return entryTypeCode;
			}
		}
		return null;
	}
}
