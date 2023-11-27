package net.luversof.web.dynamiccrud.thymeleaf.domain;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.Data;

@Data
public class Menu {

	private String messageCode;
	private String url;
	
	
	public boolean isCurrentMenu() {
		var requestUri = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURI();
		return ("/".equals(url)) ? requestUri.equals(url) : requestUri.contains(url);
	}

}