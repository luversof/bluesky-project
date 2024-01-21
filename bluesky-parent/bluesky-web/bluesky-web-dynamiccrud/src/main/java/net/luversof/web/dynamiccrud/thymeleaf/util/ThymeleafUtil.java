package net.luversof.web.dynamiccrud.thymeleaf.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.thymeleaf.config.DynamicCrudThymeleafProperties;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Pagination;

@UtilityClass
public class ThymeleafUtil {
	
	private static final String URL_PATTERN = "(\\{)([\\w\\d]*)([\\:\\w\\|]*)(})";

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(DynamicCrudThymeleafProperties.class).menu().get(key);
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

}