package net.luversof.web.dynamiccrud.thymeleaf.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UrlConstant {
	
	public static final String PATH_SETTING_PREFIX = "/{projectId}/setting";
	public static final String PATH_USE_PREFIX = "/{adminProjectId}/use";
	public static final String PATH_FRAGMENT_PREFIX = "/fragment";
	
//	public static final String PATH_VARIABLE_TYPE = "{type:project|mainMenu|subMenu|query|field}";
	
	/**
	 * /setting/{type}
	 */
//	public static final String PATH_SETTING_VIEW_INDEX = PATH_SETTING_PREFIX + "/" + PATH_VARIABLE_TYPE;
	
//	public static final String PATH_SETTING_FRAGMENT_PREFIX = PATH_SETTING_PREFIX + PATH_FRAGMENT_PREFIX;
	
	/**
	 * /setting/fragment/{type}/list
	 */
//	public static final String PATH_SETTING_FRAGMENT_LIST = PATH_SETTING_FRAGMENT_PREFIX + "/" + PATH_VARIABLE_TYPE + "/list";
	
	/**
	 * /setting/fragment/{type}/modal
	 */
//	public static final String PATH_SETTING_FRAGMENT_MODAL = PATH_SETTING_FRAGMENT_PREFIX + "/" + PATH_VARIABLE_TYPE + "/modal";
	
	
	
	
	
	/**
	 * /{adminProjectId}/use/{projectId}/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_USE_VIEW_INDEX = PATH_USE_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}";
	
	public static final String PATH_USE_FRAGMENT_PREFIX = PATH_USE_PREFIX + PATH_FRAGMENT_PREFIX;
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/findAll
	 */
	public static final String PATH_USE_FRAGMENT_LIST = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/list";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modal
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}";
	
	public static final String PATH_USE_FRAGMENT_MODAL_FORM_DELETE = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:delete}";
	
	public static final String PATH_USE_FRAGMENT_MODAL_BULK_FORM = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalBulkForm/{modalMode:import|export}";
	
	public static final String PATH_USE_FRAGMENT_EXCEL = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/excel";
	
}
