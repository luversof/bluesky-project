package net.luversof.web.dynamiccrud.use.util;

import java.util.List;

import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.FieldServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.MainMenuServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.ProductServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.QueryServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.SubMenuServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

/**
 * 이거 요청별로 캐시 처리
 */
@UtilityClass
@DevCheckUtil
public class ThymeleafUseUtil {
	
	public static boolean isAdminMenu(String product) {
		return EventAdminConstant.KEY_PRODUCT.equals(product);
	}
	
	public static MainMenu getMainMenu(String product, String mainMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(MainMenuServiceDecorator.class).findOne(new SettingParameter(product, mainMenu, null));
	}
	
	public static List<SubMenu> getSubMenuList(String product, String mainMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(SubMenuServiceDecorator.class).findList(new SettingParameter(product, mainMenu, null));
	}
	
	public static SubMenu getSubMenu(String product, String mainMenu, String subMenu) {
		return getSubMenuList(product, mainMenu).stream().filter(x -> x.getSubMenu().equals(subMenu)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static Query getQuery(String product, String mainMenu, String subMenu, QuerySqlCommandType sqlCommandType) {
		List<Query> queryList = ApplicationContextUtil.getApplicationContext().getBean(QueryServiceDecorator.class).findList(new SettingParameter(product, mainMenu, subMenu));
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(sqlCommandType)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_QUERY_TYPE").setErrorMessageArgs(sqlCommandType.toString()));
	}

	public static List<Field> getFieldList(String product, String mainMenu, String subMenu) {
		return ApplicationContextUtil.getApplicationContext().getBean(FieldServiceDecorator.class).findList(new SettingParameter(product, mainMenu, subMenu));
	}

}
