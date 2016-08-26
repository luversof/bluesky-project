package net.luversof.core.exception;

import lombok.Getter;

public class BlueskyException extends RuntimeException {
	private static final long serialVersionUID = -2499198692880482249L;

	@Getter
	private final String errorCode;

	public BlueskyException(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public BlueskyException(Enum<?> errorCode) {
		this.errorCode = resolveErrorCode(errorCode);
	}
	
	/**
	 * enum 객체의 errorCode String 값 호출
	 * @param errorCode
	 * @return
	 */
	private String resolveErrorCode(Enum<?> errorCode) {
		return String.join(".",  errorCode.getClass().getSimpleName(), errorCode.name());
	}
	
	public boolean isTargetErrorCode(Enum<?> errorCode) {
		return resolveErrorCode(errorCode).equals(this.errorCode);
	}
}
