package net.luversof.core.util;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.MessageSourceAccessor;

import lombok.Setter;

public abstract class CoreUtil {

	@Setter
	private static MessageSourceAccessor messageSourceAccessor;
	
	public static String getMessage(MessageSourceResolvable resolvable) {
		return messageSourceAccessor.getMessage(resolvable);
	}
	
	public static String getMessage(String code, String defaultMessage) {
		return messageSourceAccessor.getMessage(code, defaultMessage);
	}
	
}
