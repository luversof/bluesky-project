package net.luversof.core.util;

import java.text.MessageFormat;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public abstract class RequestAttributeUtil {

	@SuppressWarnings("unchecked")
	protected static <T> T getRequestAttribute(String name) {
		return (T) RequestContextHolder.currentRequestAttributes().getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}
	
	protected static void setRequestAttribute(String name, Object value) {
		RequestContextHolder.currentRequestAttributes().setAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
	}
	
	protected static String getAttributeName(String key, Object ... arguments) {
		return MessageFormat.format(key, arguments);
	}
}
