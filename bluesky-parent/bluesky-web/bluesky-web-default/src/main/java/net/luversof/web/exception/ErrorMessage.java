package net.luversof.web.exception;

import lombok.Data;

@Data
public class ErrorMessage {
	private String objectName;
	private String message;
	private String field;
}
