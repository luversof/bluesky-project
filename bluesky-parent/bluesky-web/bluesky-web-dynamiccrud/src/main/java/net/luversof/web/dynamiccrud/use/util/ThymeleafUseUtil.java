package net.luversof.web.dynamiccrud.use.util;

import java.util.List;

import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.DbFieldServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.DbQueryServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.MainMenuServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.SubMenuServiceDecorator;

/**
 * 이거 요청별로 캐시 처리
 */
@UtilityClass
@DevCheckUtil
public class ThymeleafUseUtil {
	
	public static boolean isAdminMenu(String adminProjectId) {
		return SettingConstant.KEY_ADMIN_PROJECT.equals(adminProjectId);
	}
	
	public static MainMenu getMainMenu(String adminProjectId, String projectId, String mainMenuId) {
		return ApplicationContextUtil.getApplicationContext().getBean(MainMenuServiceDecorator.class).findOne(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static List<SubMenu> getSubMenuList(String adminProjectId, String projectId, String mainMenuId) {
		return ApplicationContextUtil.getApplicationContext().getBean(SubMenuServiceDecorator.class).findList(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static SubMenu getSubMenu(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return getSubMenuList(adminProjectId, projectId, mainMenuId).stream().filter(x -> x.getSubMenuId().equals(subMenuId)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static DbQuery getDbQuery(String adminProjectId, String projectId, String mainMenuId, String subMenuId, DbQuerySqlCommandType sqlCommandType) {
		List<DbQuery> queryList = ApplicationContextUtil.getApplicationContext().getBean(DbQueryServiceDecorator.class).findList(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId));
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(sqlCommandType)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_QUERY_TYPE").setErrorMessageArgs(sqlCommandType.toString()));
	}

	public static List<DbField> getDbFieldList(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return ApplicationContextUtil.getApplicationContext().getBean(DbFieldServiceDecorator.class).findList(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId));
	}

}
