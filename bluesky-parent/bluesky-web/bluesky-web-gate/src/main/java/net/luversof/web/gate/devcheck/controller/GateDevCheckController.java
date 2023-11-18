package net.luversof.web.gate.devcheck.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.context.support.BlueskyReloadableResourceBundleMessageSource;
import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;

@DevCheckController
public class GateDevCheckController {
	
	private final String pathPrefix = "/gate";
	
	@Autowired
	private BlueskyReloadableResourceBundleMessageSource messageSource;
	
	@Autowired
	private Environment environment;

//	@GetMapping(pathPrefix + "/messageSource")
	public MessageSource messageSource() {
		return messageSource;
	}
	
	@DevCheckDescription("다국어 메세지 전체 목록 조회")
	@GetMapping(pathPrefix + "/messageSources")
	public Map<Object, Object> getMessageSources(@RequestParam(required = false) String searchKeyword) {
		Map<Object, Object> map = new LinkedHashMap<>();
		List<Object> keyList = messageSource.getProperties().keySet().stream()
				.filter(key -> searchKeyword == null || searchKeyword.isEmpty() || String.valueOf(key).toLowerCase().contains(searchKeyword.toLowerCase()))
				.sorted().toList();
		keyList.forEach(key -> map.put(key, messageSource.getProperties().get(key)));
		return map;
	}
	
	@DevCheckDescription("다국어 메세지 Locale별 전체 목록 조회")
	@GetMapping("/messageSourcesByLocale")
	public Map<Object, Object> getMessageSourcesByLocale(@RequestParam(required = false) Locale locale, @RequestParam(required = false) String searchKeyword) {
		final Locale targetLocale = (locale == null) ? LocaleContextHolder.getLocale() : locale;
		Map<Object, Object> map = new LinkedHashMap<>();
		List<Object> keyList = messageSource.getProperties().keySet().stream()
				.filter(key -> searchKeyword == null || searchKeyword.isEmpty() || String.valueOf(key).toLowerCase().contains(searchKeyword.toLowerCase()))
				.sorted().toList();
		
		keyList.forEach(key -> map.put(key, messageSource.getMessage(String.valueOf(key), null, targetLocale)));
		return map;
	}
	
//	@DevCheckDescription("다국어 메세지 Locale별 전체 목록 조회")
//	@GetMapping("/messageSourcesByLocale")
//	public String getMessageSourcesByLocale(@RequestParam(required = false) Locale locale, @RequestParam(required = false) String searchKeyword) {
//		final Locale targetLocale = (locale == null) ? LocaleContextHolder.getLocale() : locale;
//		return messageSource.getMessage(searchKeyword, null, targetLocale);
//	}
	
	@GetMapping("/authentication")
	public Authentication testSecurityHasRole() {
		SecurityContext context = SecurityContextHolder.getContext();
		return context.getAuthentication();
	}
	
	@GetMapping("/property")
	public String property(String key) {
		return environment.getProperty(key);
	}
}
