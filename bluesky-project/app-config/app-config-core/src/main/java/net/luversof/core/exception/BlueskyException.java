package net.luversof.core.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BlueskyException extends RuntimeException {
	private static final long serialVersionUID = -2499198692880482249L;

	public BlueskyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BlueskyException(String message, Throwable cause) {
		super(message, cause);
	}

	public BlueskyException(String message) {
		super(message);
	}

	public BlueskyException(Throwable cause) {
		super(cause);
	}
}
