package net.luversof.api.bookkeeping.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.context.BlueskyContextHolder;
import io.github.luversof.boot.core.CoreProperties;
import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import io.github.luversof.boot.web.CookieModuleProperties;
import io.github.luversof.boot.web.CookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;

@DevCheckController
public class BookkeepingDevCheckController {
	
	private static final String PATH_PREFIX = "/bookkeeping";	// NOSONAR
	
	@DevCheckDescription("coreProperties 테스트")
	@GetMapping(PATH_PREFIX + "/coreProperties")
	public CoreProperties getCoreProperties() {
		return BlueskyContextHolder.getProperties(CoreProperties.class);
	}
	
	@DevCheckDescription("moduleName 확인")
	@GetMapping(PATH_PREFIX + "/moduleName")
	public String getModuleName() {
		return BlueskyContextHolder.getContext().getModuleName();
	}
	
	@DevCheckDescription("cookieProperties 테스트")
	@GetMapping(PATH_PREFIX + "/cookieProperties")
	public CookieProperties getCookieProperties() {
		return BlueskyContextHolder.getProperties(CookieProperties.class);
	}

	@DevCheckDescription("requestUrl 테스트")
	@GetMapping(PATH_PREFIX + "/requestUrl")
	public String requestUrl(HttpServletRequest request) {
		return request.getRequestURL().toString();
	}
	
	@Setter(onMethod_ = @Autowired(required = false))
	private Map<String, CookieModuleProperties> cookieModulePropertiesMap;
	
	@DevCheckDescription("cookieModulePropertiesMap 테스트")
	@GetMapping(PATH_PREFIX + "/cookieModulePropertiesMap")
	public Map<String, CookieModuleProperties> cookieModulePropertiesMap() {
		return cookieModulePropertiesMap;
	}
	
	@DevCheckDescription("otherCookieProperties 테스트")
	@GetMapping(PATH_PREFIX + "/otherCookieProperties")
	public CookieProperties otherCookieProperties() {
		return BlueskyContextHolder.getProperties(CookieProperties.class, "otherCookieProperties"); 
	}
}
