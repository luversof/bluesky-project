package net.luversof.api.bookkeeping.constant;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BookkeepingError {
	
	INVALID_REQUEST,
	
	ALREADY_EXIST_BOOKKEEPING,
	NOT_EXIST_BOOKKEEPING,
	
	NOT_EXIST_BOOKKEEPING_ID,
	INVALID_BOOKKEEPING_ID,
	
	NOT_EXIST_ASSET,
	UNABLE_DELETE_ASSET,
	
	INVALID_ASSET_ID,
	
	NOT_EXIST_ASSETTYPE_ID,
	INVALID_ASSETTYPE_ID,
	
	NOT_EXIST_ENTRYTYPE,
	INVALID_ENTRYTYPE,
	
	INVALID_ENTRYTYPECODE,
	
	
	NOT_EXIST_ENTRY,
	;
	
	public BlueskyException exception() {
		return new BlueskyException(this);
	}
	
	public void throwException() throws BlueskyException {
		throw exception();
	}
}
