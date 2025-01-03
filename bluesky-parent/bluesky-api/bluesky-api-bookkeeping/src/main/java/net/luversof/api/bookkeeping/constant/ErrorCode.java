package net.luversof.api.bookkeeping.constant;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
	
	ALREADY_EXIST_BOOKKEEPING("bookkeeping.ALREADY_EXIST_BOOKKEEPING"),
	NOT_EXIST_BOOKKEEPING("bookkeeping.NOT_EXIST_BOOKKEEPING"),
	
	NOT_EXIST_BOOKKEEPING_ID("bookkeeping.NOT_EXIST_BOOKKEEPING_ID"),
	INVALID_BOOKKEEPING_ID("bookkeeping.INVALID_BOOKKEEPING_ID"),
	
	NOT_EXIST_ASSET("bookkeeping.NOT_EXIST_ASSET"),
	
	INVALID_ASSET_ID("bookkeeping.INVALID_ASSET_ID"),
	
	NOT_EXIST_ASSETTYPE_ID("bookkeeping.NOT_EXIST_ASSETTYPE_ID"),
	INVALID_ASSETTYPE_ID("bookkeeping.INVALID_ASSETTYPE_ID"),
	
	NOT_EXIST_ENTRYTYPE("bookkeeping.NOT_EXIST_ENTRYTYPE"),
	INVALID_ENTRYTYPE("bookkeeping.INVALID_ENTRYTYPE"),
	
	INVALID_ENTRYTYPECODE("bookkeeping.INVALID_ENTRYTYPECODE"),
	
	
	NOT_EXIST_ENTRY("bookkeeping.NOT_EXIST_ENTRY"),
	;
	
	private String errorCodeStr;
	
	public BlueskyException exception() {
		return new BlueskyException(this.getErrorCodeStr());
	}
	
	public void throwException() throws BlueskyException {
		throw exception();
	}
}
