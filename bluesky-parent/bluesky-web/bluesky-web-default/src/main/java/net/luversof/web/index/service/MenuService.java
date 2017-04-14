package net.luversof.web.index.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.SneakyThrows;

@Service
public class MenuService {

	private List<Map<String, String>> menuList;
	private Map<String, Map<String, String>> menuListUriMap = new HashMap<>();
	private static final String MENU_PATH = "menu.xml";
	private static final String MENU_KEY = "url";

	PathMatcher pathMatcher = new AntPathMatcher();

	@PostConstruct
	public void postConstruct() {
		load();
	}

	public Map<String, String> getMenu() {
		String requestURI = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getRequestURI();
		if (menuListUriMap.containsKey(requestURI)) {
			return menuListUriMap.get(requestURI);
		}
		Map<String, String> collect = menuList.stream()
				.filter(menu -> pathMatcher.match(menu.get(MENU_KEY) + "/**", requestURI))
				.sorted((menu1, menu2) -> Integer.compare(menu1.get(MENU_KEY).length(), menu2.get(MENU_KEY).length()))
				.collect(HashMap::new, Map::putAll, Map::putAll);
		 if (!collect.isEmpty()) {
			 menuListUriMap.put(requestURI, collect); 
		 }
		 return collect;
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public void load() {
		XmlMapper xmlMapper = new XmlMapper();
		ClassPathResource classPathResource = new ClassPathResource(MENU_PATH);
		InputStream inputStream = classPathResource.getInputStream();
		menuList = xmlMapper.readValue(inputStream, List.class);
	}
	
	
	public void reload() {
		load();
		menuListUriMap.clear();
	}

}
