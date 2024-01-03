package net.luversof.web.dynamiccrud.use.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.FieldService;
import net.luversof.web.dynamiccrud.setting.service.QueryService;
import net.luversof.web.dynamiccrud.setting.service.SettingDataService;
import net.luversof.web.dynamiccrud.setting.service.SubMenuService;

/**
 * 이거 요청별로 캐시 처리
 */
@UtilityClass
@DevCheckUtil
public class ThymeleafUseUtil {
	
	public static LinkedHashMap<String, String> getColumnMap(Page<Map<String, Object>> page, List<Field> fieldList) {
		if (page == null || !page.hasContent()) {
			return null;
		}
		
		return getColumnMap(page.getContent().get(0),  fieldList);
	}
	

	public static LinkedHashMap<String, String> getColumnMap(Map<String, Object> data, List<Field> fieldList) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		
		var map = new LinkedHashMap<String, String>();
		
		// Field 정보가 없으면 그대로 노출 처리
		data.keySet().forEach(key -> {
			if (fieldList != null && !fieldList.isEmpty()) {
				var targetField = fieldList.stream().filter(x -> x.getColumn().equals(key)).findAny().orElseGet(() -> null);
				if (targetField == null) {
					map.put(key, key);
				} else {
					if (targetField.isVisible()) {
						map.put(targetField.getColumn(), targetField.getName());
					}
				}
			} else {
				map.put(key, key);
			}
		});
		
		// fieldList중 SPEL 형식이 있는 경우 추가해야 함
		// 잠만.. 이거 엑셀에도 보여줘야 하고 view에도 보여줘야 하는데 이걸 한꺼번해 처리할 수 있는 객체를 만드는게 더 좋을 것 같은데?
		return map;
	}
	
	public static boolean isAdminMenu(String product) {
		return SettingDataService.KEY_EVENT_ADMIN_PRODUCT.equals(product);
	}
	
	public static List<SubMenu> getSubMenuList(String product, String mainMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(SubMenuService.class).findByProductAndMainMenu(product, mainMenu);
	}
	
	public static SubMenu getSubMenu(String product, String mainMenu, String subMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(SubMenuService.class).findByProductAndMainMenu(product, mainMenu).stream().filter(x -> x.getSubMenu().equals(subMenu)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static Query getQuery(String product, String mainMenu, String subMenu, QuerySqlCommandType sqlCommandType) {
		List<Query> queryList = ApplicationContextUtil.getApplicationContext().getBean(QueryService.class).findByProductAndMainMenuAndSubMenu(product, mainMenu, subMenu);
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(sqlCommandType)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_QUERY_TYPE").setErrorMessageArgs(sqlCommandType.toString()));
	}

	public static List<Field> getFieldList(String product, String mainMenu, String subMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(FieldService.class).findByProductAndMainMenuAndSubMenu(product, mainMenu, subMenu);
	}

}
