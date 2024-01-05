package net.luversof.web.dynamiccrud.thymeleaf.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DynamicCrudConstant {
	
//	public static final String PATH_SETTING_PREFIX = "/setting";
	public static final String PATH_USE_PREFIX = "/use";
	public static final String PATH_FRAGMENT_PREFIX = "/fragment";
	
//	public static final String PATH_VARIABLE_TYPE = "{type:product|mainMenu|subMenu|query|field}";
	
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
	 * /use/{product}/{mainMenu}/{subMenu}
	 */
	public static final String PATH_USE_VIEW_INDEX = PATH_USE_PREFIX + "/{product}/{mainMenu}/{subMenu}";
	
	public static final String PATH_USE_FRAGMENT_PREFIX = PATH_USE_PREFIX + PATH_FRAGMENT_PREFIX;
	
	/**
	 * /use/fragment/{product}/{mainMenu}/{subMenu}/findAll
	 */
	public static final String PATH_USE_FRAGMENT_LIST = PATH_USE_FRAGMENT_PREFIX + "/{product}/{mainMenu}/{subMenu}/list";
	
	/**
	 * /use/fragment/{product}/{mainMenu}/{subMenu}/modal
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM = PATH_USE_FRAGMENT_PREFIX + "/{product}/{mainMenu}/{subMenu}/modalForm/{modalMode:create|update}";
	
	public static final String PATH_USE_FRAGMENT_MODAL_FORM_DELETE = PATH_USE_FRAGMENT_PREFIX + "/{product}/{mainMenu}/{subMenu}/modalForm/{modalMode:delete}";
	
	public static final String PATH_USE_FRAGMENT_MODAL_BULK_FORM = PATH_USE_FRAGMENT_PREFIX + "/{product}/{mainMenu}/{subMenu}/modalBulkForm/{modalMode:import|export}";
	
	public static final String PATH_USE_FRAGMENT_EXCEL = PATH_USE_FRAGMENT_PREFIX + "/{product}/{mainMenu}/{subMenu}/excel";
	
}
