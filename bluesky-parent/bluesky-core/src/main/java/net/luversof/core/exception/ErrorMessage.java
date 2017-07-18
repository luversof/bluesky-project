package net.luversof.core.exception;

import lombok.Data;

@Data
public class ErrorMessage {
	String exceptionClassName;
	boolean isDisplayableMessage = false;	//에러 메세지 화면 표시 가능여부
	private String message;
	String object;		//bindException의 경우 에러 발생 ObjectName을 전달, 보통의 경우 code를 전달
	private String field;
}
