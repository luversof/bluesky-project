package net.luversof.web.config.core;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalControllerExceptionHandler {
	
	@ExceptionHandler
	public String handleException(Exception exception, ModelMap modelMap) {
//		modelMap.addAttribute(attributeName, attributeValue)
		return "/error";
	}

}
