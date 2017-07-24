package net.luversof.core.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BlueskyException extends RuntimeException {
	private static final long serialVersionUID = -2499198692880482249L;

	private final String errorCode;
	
	private String localizedMessage;
	
	private String errorPage = ErrorPage.DEFAULT;

	private String[] errorMessageArgs;
	
	public BlueskyException(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public BlueskyException errorPage(String errorPage) {
		this.errorPage = errorPage;
		return this;
	}
	
	public BlueskyException(Enum<?> errorCode) {
		this.errorCode = resolveErrorCode(errorCode);
	}
	
	public BlueskyException setErrorMessageArgs(String... errorMessageArgs) {
		this.errorMessageArgs = errorMessageArgs;
		return this;
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
