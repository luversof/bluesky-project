package net.luversof.web.dynamiccrud.thymeleaf.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UrlConstant {
	
	public static final String PATH_SETTING_PREFIX = "/{projectId}/setting";
	public static final String PATH_USE_PREFIX = "/{adminProjectId}/use";
	public static final String PATH_FRAGMENT_PREFIX = "/fragment";
	
	/**
	 * {projectId}/setting/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_SETTING_VIEW_INDEX = PATH_SETTING_PREFIX + "/{mainMenuId}/{subMenuId}";
	
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
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:delete}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM_DELETE = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:delete}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalBulkForm/{modalMode:import|export}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_BULK_FORM = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/modalBulkForm/{modalMode:import|export}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/excel
	 */
	public static final String PATH_USE_FRAGMENT_EXCEL = PATH_USE_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}/excel";
	
}
