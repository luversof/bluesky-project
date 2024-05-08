package net.luversof.web.common.menu.domain;

import java.util.regex.Pattern;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.Data;

@Data
public class Menu {
	private String messageCode;
	private String url;
	private String activeUrlPattern;
	private boolean isDisplay;
	
	
	public boolean isCurrentMenu() {
		var requestUri = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURI();
		if (activeUrlPattern != null) {
			return Pattern.compile(activeUrlPattern).matcher(requestUri).matches();
		}
		return requestUri.startsWith(url);
	}

	public String getName() {
		return MessageUtil.getMessage(messageCode, messageCode);
	}
}
