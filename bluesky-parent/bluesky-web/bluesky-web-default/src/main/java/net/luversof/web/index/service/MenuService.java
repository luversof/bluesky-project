package net.luversof.web.index.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.SneakyThrows;

@Service
public class MenuService {

	private List<Map<String, String>> menuList;
	private static final String MENU_PATH = "menu.xml";
	private static final String MENU_KEY = "url";

	@PostConstruct
	public void postConstruct() {
		load();
	}

	public Map<String, String> getMenu() {
		String requestURI = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getRequestURI();
		 return menuList.stream()
				.filter(menu -> requestURI.startsWith(menu.get(MENU_KEY)))
				.sorted((menu1, menu2) -> Integer.compare(menu1.get(MENU_KEY).length(), menu2.get(MENU_KEY).length()))
				.collect(HashMap::new, Map::putAll, Map::putAll);
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public void load() {
		XmlMapper xmlMapper = new XmlMapper();
		ClassPathResource classPathResource = new ClassPathResource(MENU_PATH);
		InputStream inputStream = classPathResource.getInputStream();
		menuList = xmlMapper.readValue(inputStream, List.class);
	}

}
