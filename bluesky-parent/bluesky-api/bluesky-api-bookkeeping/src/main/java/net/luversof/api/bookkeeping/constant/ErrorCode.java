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
	
	NOT_EXIST_ASSETTYPE_ID("bookkeeping.NOT_EXIST_ASSETTYPE_ID"),
	INVALID_ASSETTYPE_ID("bookkeeping.INVALID_ASSETTYPE_ID"),
	
	;
	
	private String errorCodeStr;
	
	
	public void throwException() {
		throw new BlueskyException(this.getErrorCodeStr());
	}
}
