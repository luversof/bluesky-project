package net.luversof.web.dynamiccrud.use.util;

import java.util.List;

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
