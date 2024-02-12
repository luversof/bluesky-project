package net.luversof.web.dynamiccrud.thymeleaf.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Pagination;

@UtilityClass
public class ThymeleafUtil {
	
	private static final String URL_PATTERN = "(\\{)([\\w\\d]*)([\\:\\w\\|]*)(})";
	
	private static final String[] THEMES = new String[] {
			"bluesky",
			"light",
			"dark",
			"cupcake",
			"bumblebee",
			"emerald",
			"corporate",
			"synthwave",
			"retro",
			"cyberpunk",
			"valentine",
			"halloween",
			"garden",
			"forest",
			"aqua",
			"lofi",
			"pastel",
			"fantasy",
			"wireframe",
			"black",
			"luxury",
			"dracula",
			"cmyk",
			"autumn",
			"business",
			"acid",
			"lemonade",
			"night",
			"coffee",
			"winter",
			"dim",
			"nord",
			"sunset"
	};

	public static List<Menu> getMenuList(String adminProjectId, String projectId, String mainMenuId) {
		var subMenuList = SettingUtil.getSubMenuList(adminProjectId, projectId, mainMenuId);
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		var menuList = new ArrayList<Menu>();
		subMenuList.forEach(x -> {
			var menu = new Menu();
			menu.setUrl(x.getUrl());
			menu.setMessageCode(x.getSubMenuName());
			menuList.add(menu);
		});
		return menuList;
	}
	
	/**
	 * layout에서 처리하는 menu호출이 null인 경우 기본 menu를 호출 처리
	 * @return
	 */
	public static List<Menu> getDefaultMenuList() {
		return getMenuList(
				EventAdminConstant.ADMIN_PROJECT_ID_VALUE,
				EventAdminConstant.PROJECT_ID_VALUE,
				EventAdminConstant.MAINMENU_ID_VALUE);
	}
	
	public static Pagination getPagination(@SuppressWarnings("rawtypes") Page page) {
		return new Pagination(page);
	}
	
	public static String getUrl(String target) {
		return getUrl(target, null);
	}
	
	private String getAttribute(String key) {
		return (String) RequestContextHolder.getRequestAttributes().getAttribute(key, RequestAttributes.SCOPE_REQUEST);
	}
	
	private void putRequestParameterToMap(Map<String, String> map, String key) {
		if(!map.containsKey(key)) {
			map.put(key, getAttribute(key));
		}
	}
	
	public static String getUrl(String target, Map<String, String> map) {
		var targetMap = map == null ? new HashMap<String, String>() : new HashMap<String, String>(map);
		putRequestParameterToMap(targetMap, SettingConstant.ADMIN_PROJECT_ID);
		putRequestParameterToMap(targetMap, SettingConstant.PROJECT_ID);
		putRequestParameterToMap(targetMap, SettingConstant.MAINMENU_ID);
		putRequestParameterToMap(targetMap, SettingConstant.SUBMENU_ID);

		var url = UrlConstant.UrlResolver.getUrl(target, targetMap.get(SettingConstant.ADMIN_PROJECT_ID));
		
		Pattern pattern = Pattern.compile(URL_PATTERN);
		Matcher matcher = pattern.matcher(url);
		
		var replaceUrl = url;
		while (matcher.find()) {
			String key = matcher.group(2);
			if (targetMap.containsKey(key)) {
				replaceUrl = replaceUrl.replace(matcher.group(), targetMap.get(key));
			}
		}
		return replaceUrl;
	}
	
	public static boolean isAdminMenu() {
		return SettingUtil.isAdminMenu(getAttribute(SettingConstant.ADMIN_PROJECT_ID));
	}
	
	public static MainMenu getMainMenu() {
		return SettingUtil.getMainMenu(
			getAttribute(SettingConstant.ADMIN_PROJECT_ID),
			getAttribute(SettingConstant.PROJECT_ID),
			getAttribute(SettingConstant.MAINMENU_ID));
	}
	
	public static SubMenu getSubMenu() {
		return SettingUtil.getSubMenu(
			getAttribute(SettingConstant.ADMIN_PROJECT_ID),
			getAttribute(SettingConstant.PROJECT_ID),
			getAttribute(SettingConstant.MAINMENU_ID),
			getAttribute(SettingConstant.SUBMENU_ID));
	}
	
	public static List<DbField> getDbFieldList() {
		return SettingUtil.getDbFieldList(
			getAttribute(SettingConstant.ADMIN_PROJECT_ID),
			getAttribute(SettingConstant.PROJECT_ID),
			getAttribute(SettingConstant.MAINMENU_ID),
			getAttribute(SettingConstant.SUBMENU_ID));
	}
	
	public static String getRandomTheme(String...themes) {
		Random random = new Random();
		var themeList = List.of(themes);
		return themeList.get(random.nextInt(themeList.size()));
	}
	
	public static List<String> getThemeList() {
		return Arrays.asList(THEMES);
	}

}