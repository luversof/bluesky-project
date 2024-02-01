package net.luversof.web.dynamiccrud.setting.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.util.ApplicationContextUtil;
import io.github.luversof.boot.util.RequestAttributeUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.AdminProjectServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.DbFieldServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.DbQueryServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.MainMenuServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.ProjectServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.SubMenuServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;

/**
 * 이거 요청별로 캐시 처리
 */
@UtilityClass
@DevCheckUtil
public class SettingUtil extends RequestAttributeUtil {
	
	private static final String ADMINPROJECT = "__adminProject_{0}";
	private static final String PROJECT = "__project_{0}_{1}";
	private static final String MAINMENU = "__mainMenu_{0}_{1}_{2}";
	private static final String SUBMENU_LIST = "__subMenuList_{0}_{1}_{2}";
	private static final String DBQUERY_LIST = "__dbQueryList_{0}_{1}_{2}_{3}";
	private static final String DBFIELD_LIST = "__dbFieldList_{0}_{1}_{2}_{3}";
	
	public static boolean isAdminMenu(String adminProjectId) {
		return AdminConstant.ADMIN_PROJECT_ID_VALUE.equals(adminProjectId);
	}
	
	public static AdminProject getAdminProject(SettingParameter settingParameter) {
		var attributeName = getAttributeName(ADMINPROJECT, settingParameter.adminProjectId());
		Optional<AdminProject> adminProjectOptional = getRequestAttribute(attributeName);
		if (adminProjectOptional != null) {
			return adminProjectOptional.get();
		}
		adminProjectOptional = Optional.ofNullable(ApplicationContextUtil.getApplicationContext().getBean(AdminProjectServiceDecorator.class).findOne(settingParameter));
		setRequestAttribute(attributeName, adminProjectOptional);
		
		return adminProjectOptional.get();
	}
	
	public static AdminProject getAdminProject(String adminProjectId) {
		return getAdminProject(new SettingParameter(adminProjectId, null, null, null));
	}
	
	public static Project getProject(SettingParameter settingParameter) {
		var attributeName = getAttributeName(PROJECT, settingParameter.adminProjectId(), settingParameter.projectId());
		Optional<Project> projectOptional = getRequestAttribute(attributeName);
		if (projectOptional != null) {
			return projectOptional.get();
		}
		projectOptional = Optional.ofNullable(ApplicationContextUtil.getApplicationContext().getBean(ProjectServiceDecorator.class).findOne(settingParameter));
		setRequestAttribute(attributeName, projectOptional);
		
		return projectOptional.get();
	}
	
	public static Project getProject(String adminProjectId, String projectId) {
		return getProject(new SettingParameter(adminProjectId, projectId, null, null));
	}
	
	public static MainMenu getMainMenu(SettingParameter settingParameter) {
		var attributeName = getAttributeName(MAINMENU, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId());
		Optional<MainMenu> mainMenuOptional = getRequestAttribute(attributeName);
		if (mainMenuOptional != null) {
			return mainMenuOptional.get();
		}
		mainMenuOptional = Optional.ofNullable(ApplicationContextUtil.getApplicationContext().getBean(MainMenuServiceDecorator.class).findOne(settingParameter));
		setRequestAttribute(attributeName, mainMenuOptional);
		
		return mainMenuOptional.get();
	}
	
	public static MainMenu getMainMenu(String adminProjectId, String projectId, String mainMenuId) {
		return getMainMenu(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
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
