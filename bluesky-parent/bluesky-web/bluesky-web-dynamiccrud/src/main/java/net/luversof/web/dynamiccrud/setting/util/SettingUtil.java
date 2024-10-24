package net.luversof.web.dynamiccrud.setting.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.ResolvableType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import io.github.luversof.boot.context.ApplicationContextUtil;
import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.web.servlet.util.ServletRequestUtil;
import io.github.luversof.boot.web.util.RequestAttributeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;

/**
 * 이거 요청별로 캐시 처리
 */
@UtilityClass
@DevCheckUtil
public class SettingUtil extends RequestAttributeUtil {
	
	private static final String SETTINGPARAMETER = "__settingParameter";
	private static final String ADMINPROJECT = "__adminProject_{0}";
	private static final String PROJECT = "__project_{0}_{1}";
	private static final String MAINMENU = "__mainMenu_{0}_{1}_{2}";
	private static final String MAINMENU_LIST = "__mainMenuList_{0}_{1}_{2}";
	private static final String SUBMENU_LIST = "__subMenuList_{0}_{1}_{2}";
	private static final String DBQUERY_LIST = "__dbQueryList_{0}_{1}_{2}_{3}";
	private static final String DBFIELD_LIST = "__dbFieldList_{0}_{1}_{2}_{3}";

	// 두번째 path가 setting인 경우에 해당하는지 체크
	private static final Pattern pattern = Pattern.compile("(?:\\/[\\w\\d]+)(\\/setting\\/).*");
	
	public static boolean isAdminMenu(String adminProjectId) {
		return AdminConstant.ADMIN_PROJECT_ID_VALUE.equals(adminProjectId);
	}
	
	/**
	 * setting Parameter의 조회
	 * 1. attribute에 캐싱된 값 조회
	 * 2. HandlerMapping의 URI_TEMPLATE_VARIABLES_ATTRIBUTE 값 조회
	 * 3. filter의 경우 위 2번이 설정되기 전 단계이기 때문에 별도의 ServletRequestUtil로 조회 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static SettingParameter getSettingParameter() {
		
		if (RequestContextHolder.getRequestAttributes() == null) {
			return null;
		}
		
		var attributeName = getAttributeName(SETTINGPARAMETER);
		Optional<SettingParameter> settingParameterOptional = getRequestAttribute(attributeName);
		if (settingParameterOptional != null) {
			return settingParameterOptional.get();
		}
		
		// 두번째 path가 setting인 경우 adminProjectId는 고정값을 지정해야 함
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		Matcher matcher = pattern.matcher(request.getRequestURI());
		boolean isSettingUrl = matcher.matches();
		
		Map<String, String> pathVariableMap = null;
		
		var uriTemplateVariablesAttribute = getRequestAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (uriTemplateVariablesAttribute != null && uriTemplateVariablesAttribute instanceof Map map) {
			pathVariableMap = map;
		} else {
			pathVariableMap = ServletRequestUtil.getUriVariableMap();
		}
		
		if (pathVariableMap == null) {
			return null;
		}
		
		settingParameterOptional = Optional.of(
			new SettingParameter(
				isSettingUrl ? AdminConstant.ADMIN_PROJECT_ID_VALUE : pathVariableMap.get(SettingConstant.ADMIN_PROJECT_ID),
				pathVariableMap.get(SettingConstant.PROJECT_ID),
				pathVariableMap.get(SettingConstant.MAINMENU_ID),
				pathVariableMap.get(SettingConstant.SUBMENU_ID)
			)
		);
	
		
		setRequestAttribute(attributeName, settingParameterOptional);
		return settingParameterOptional.get();
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Setting> SettingServiceSupplier<T> getSettingServiceSupplierDecorator(Class<T> clazz) {
		ResolvableType type = ResolvableType.forClassWithGenerics(SettingServiceSupplier.class, clazz);
		return (SettingServiceSupplier<T>) ApplicationContextUtil.getApplicationContext().getBeanProvider(type).stream().filter(x -> x instanceof SettingServiceDecorator).findFirst().orElseGet(() -> null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Setting> SettingServiceListSupplier<T> getSettingServiceListSupplierDecorator(Class<T> clazz) {
		ResolvableType type = ResolvableType.forClassWithGenerics(SettingServiceListSupplier.class, clazz);
		return (SettingServiceListSupplier<T>) ApplicationContextUtil.getApplicationContext().getBeanProvider(type).stream().filter(x -> x instanceof SettingServiceDecorator).findFirst().orElseGet(() -> null);
	}
	
	public static AdminProject getAdminProject(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null) {
			return null;
		}
		var attributeName = getAttributeName(ADMINPROJECT, settingParameter.adminProjectId());
		return getObject(attributeName, () -> getSettingServiceSupplierDecorator(AdminProject.class).findOne(settingParameter));
	}
	
	public static AdminProject getAdminProject(String adminProjectId) {
		return getAdminProject(new SettingParameter(adminProjectId, null, null, null));
	}
	
	public static AdminProject getAdminProject() {
		return getAdminProject(getSettingParameter());
	}
	
	public static Project getProject(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null || settingParameter.projectId() == null) {
			return null;
		}
		
		var attributeName = getAttributeName(PROJECT, settingParameter.adminProjectId(), settingParameter.projectId());
		return getObject(attributeName, () -> getSettingServiceSupplierDecorator(Project.class).findOne(settingParameter));
	}
	
	public static Project getProject(String adminProjectId, String projectId) {
		return getProject(new SettingParameter(adminProjectId, projectId, null, null));
	}
	
	public static Project getProject() {
		return getProject(getSettingParameter());
	}
	
	public static MainMenu getMainMenu(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null || settingParameter.projectId() == null || settingParameter.mainMenuId() == null) {
			return null;
		}
		
		var attributeName = getAttributeName(MAINMENU, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId());
		return getObject(attributeName, () -> getSettingServiceSupplierDecorator(MainMenu.class).findOne(settingParameter));
	}
	
	public static MainMenu getMainMenu(String adminProjectId, String projectId, String mainMenuId) {
		return getMainMenu(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static MainMenu getMainMenu() {
		return getMainMenu(getSettingParameter());
	}
	
	public static List<MainMenu> getMainMenuList(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null || settingParameter.projectId() == null) {
			return Collections.emptyList();
		}

		var attributeName = getAttributeName(MAINMENU_LIST, settingParameter.adminProjectId(), settingParameter.projectId());
		return getList(attributeName, () -> getSettingServiceListSupplierDecorator(MainMenu.class).findList(settingParameter));
	}
	
	public static List<MainMenu> getMainMenuList(String adminProjectId, String projectId) {
		return getMainMenuList(new SettingParameter(adminProjectId, projectId, null, null));
	}
	
	public static List<MainMenu> getMainMenuList() {
		return getMainMenuList(getSettingParameter());
	}
	
	public static List<SubMenu> getSubMenuList(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null || settingParameter.projectId() == null || settingParameter.mainMenuId() == null) {
			return Collections.emptyList();
		}
		
		var attributeName = getAttributeName(SUBMENU_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId());
		return getList(attributeName, () -> getSettingServiceListSupplierDecorator(SubMenu.class).findList(settingParameter));
	}
	
	public static List<SubMenu> getSubMenuList(String adminProjectId, String projectId, String mainMenuId) {
		return getSubMenuList(new SettingParameter(adminProjectId, projectId, mainMenuId, null));
	}
	
	public static List<SubMenu> getSubMenuList() {
		return getSubMenuList(getSettingParameter());
	}
	
	public static SubMenu getSubMenu(SettingParameter settingParameter) {
		return getSubMenuList(settingParameter).stream().filter(x -> x.getSubMenuId().equals(settingParameter.subMenuId())).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static SubMenu getSubMenu(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return getSubMenuList(adminProjectId, projectId, mainMenuId).stream().filter(x -> x.getSubMenuId().equals(subMenuId)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_SUBMENU"));
	}
	
	public static SubMenu getSubMenu() { 
		return getSubMenu(getSettingParameter());
	}
	
	public static List<DbQuery> getDbQueryList(SettingParameter settingParameter) {
		if (settingParameter == null || settingParameter.adminProjectId() == null || settingParameter.projectId() == null || settingParameter.mainMenuId() == null || settingParameter.subMenuId() == null) {
			return Collections.emptyList();
		}
		
		var attributeName = getAttributeName(DBQUERY_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId(), settingParameter.subMenuId());
		return getList(attributeName, () -> getSettingServiceListSupplierDecorator(DbQuery.class).findList(settingParameter));
	}
	
	public static DbQuery getDbQuery(SettingParameter settingParameter, DbQuerySqlCommandType sqlCommandType) {
		List<DbQuery> queryList = getDbQueryList(settingParameter);
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(sqlCommandType)).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_QUERY_TYPE", sqlCommandType.toString()));
	}
	
	public static DbQuery getDbQuery(String adminProjectId, String projectId, String mainMenuId, String subMenuId, DbQuerySqlCommandType sqlCommandType) {
		return getDbQuery(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId), sqlCommandType);
	}
	
	public static DbQuery getDbQuery(DbQuerySqlCommandType sqlCommandType) {
		return getDbQuery(getSettingParameter(), sqlCommandType);
	}
	
	public static List<DbField> getDbFieldList(SettingParameter settingParameter) {
		if (settingParameter == null  || settingParameter.adminProjectId() == null || settingParameter.projectId() == null || settingParameter.mainMenuId() == null || settingParameter.subMenuId() == null) {
			return Collections.emptyList();
		}
		
		var attributeName = getAttributeName(DBFIELD_LIST, settingParameter.adminProjectId(), settingParameter.projectId(), settingParameter.mainMenuId(), settingParameter.subMenuId());
		return getList(attributeName, () -> getSettingServiceListSupplierDecorator(DbField.class).findList(settingParameter));
	}

	public static List<DbField> getDbFieldList(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		return getDbFieldList(new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId));
	}
	
	public static List<DbField> getDbFieldList() {
		return getDbFieldList(getSettingParameter());
	}

}
