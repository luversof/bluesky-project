package net.luversof.web.dynamiccrud.setting.util;

import java.util.Collections;
import java.util.List;

import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.util.ApplicationContextUtil;
import io.github.luversof.boot.util.RequestAttributeUtil;
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
public class SettingUtil extends RequestAttributeUtil {
	
	private static final String SUBMENU_LIST = "__subMenuList_{0}_{1}_{2}";
	private static final String DBQUERY_LIST = "__dbQueryList_{0}_{1}_{2}_{3}";
	private static final String DBFIELD_LIST = "__dbFieldList_{0}_{1}_{2}_{3}";
	
	public static boolean isAdminMenu(String adminProjectId) {
		return SettingConstant.KEY_ADMIN_PROJECT.equals(adminProjectId);
	}
	
	public static MainMenu getMainMenu(String adminProjectId, String projectId, String mainMenuId) {
		return ApplicationContextUtil.getApplicationContext().getBean(MainMenuServiceDecorator.class).findOne(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static List<SubMenu> getSubMenuList(SettingParameter settingParameter) {
		var attributeName = getAttributeName(SUBMENU_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId());
		List<SubMenu> subMenuList = getRequestAttribute(attributeName);
		if (subMenuList != null) {
			return subMenuList;
		}
		subMenuList = ApplicationContextUtil.getApplicationContext().getBean(SubMenuServiceDecorator.class).findList(settingParameter);
		if (subMenuList == null) {
			subMenuList = Collections.emptyList();
		}
		setRequestAttribute(attributeName, subMenuList);
		
		return subMenuList;
	}
	
	public static List<SubMenu> getSubMenuList(String adminProjectId, String projectId, String mainMenuId) {
		return getSubMenuList(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static SubMenu getSubMenu(SettingParameter settingParameter) {
		return getSubMenuList(settingParameter).stream().filter(x -> x.getSubMenuId().equals(settingParameter.subMenuId())).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static SubMenu getSubMenu(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return getSubMenuList(adminProjectId, projectId, mainMenuId).stream().filter(x -> x.getSubMenuId().equals(subMenuId)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static List<DbQuery> getDbQueryList(SettingParameter settingParameter) {
		var attributeName = getAttributeName(DBQUERY_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId(), settingParameter.subMenuId());
		List<DbQuery> dbQueryList = getRequestAttribute(attributeName);
		if (dbQueryList != null) {
			return dbQueryList;
		}
		dbQueryList = ApplicationContextUtil.getApplicationContext().getBean(DbQueryServiceDecorator.class).findList(settingParameter);
		setRequestAttribute(attributeName, dbQueryList);
		return dbQueryList;
	}
	
	public static DbQuery getDbQuery(SettingParameter settingParameter, DbQuerySqlCommandType sqlCommandType) {
		List<DbQuery> queryList = getDbQueryList(settingParameter);
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(sqlCommandType)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_QUERY_TYPE").setErrorMessageArgs(sqlCommandType.toString()));
	}
	
	public static DbQuery getDbQuery(String adminProjectId, String projectId, String mainMenuId, String subMenuId, DbQuerySqlCommandType sqlCommandType) {
		return getDbQuery(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId), sqlCommandType);
	}
	
	public static List<DbField> getDbFieldList(SettingParameter settingParameter) {
		var attributeName = getAttributeName(DBFIELD_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId(), settingParameter.subMenuId());
		List<DbField> dbFieldList = getRequestAttribute(attributeName);
		if (dbFieldList != null) {
			return dbFieldList;
		}
		dbFieldList = ApplicationContextUtil.getApplicationContext().getBean(DbFieldServiceDecorator.class).findList(settingParameter);
		setRequestAttribute(attributeName, dbFieldList);
		return dbFieldList;
	}

	public static List<DbField> getDbFieldList(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return getDbFieldList(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId));
	}

}
