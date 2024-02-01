package net.luversof.web.dynamiccrud.thymeleaf.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;

@UtilityClass
public class UrlConstant {
	
	public static final String PATH_SETTING_PREFIX = "/{projectId}/setting";
	public static final String PATH_USE_PREFIX = "/{adminProjectId}/use";
	public static final String PATH_FRAGMENT_PREFIX = "/fragment";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_SETTING_FRAGMENT_PREFIX = PATH_SETTING_PREFIX + PATH_FRAGMENT_PREFIX + "/{mainMenuId}/{subMenuId}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_USE_FRAGMENT_PREFIX = PATH_USE_PREFIX + PATH_FRAGMENT_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}";
	
	/** (s) setting path **/
	
	/**
	 * /{projectId}/setting/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_SETTING_VIEW_INDEX = PATH_SETTING_PREFIX + "/{mainMenuId}/{subMenuId}";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/findAll
	 */
	public static final String PATH_SETTING_FRAGMENT_LIST = PATH_SETTING_FRAGMENT_PREFIX + "/list";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}
	 */
	public static final String PATH_SETTING_FRAGMENT_MODAL_FORM = PATH_SETTING_FRAGMENT_PREFIX + "/modalForm/{modalMode:create|update}";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/modalForm/{modalMode:delete}
	 */
	public static final String PATH_SETTING_FRAGMENT_MODAL_FORM_DELETE = PATH_SETTING_FRAGMENT_PREFIX + "/modalForm/{modalMode:delete}";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/modalBulkForm/{modalMode:import|export}
	 */
	public static final String PATH_SETTING_FRAGMENT_MODAL_BULK_FORM = PATH_SETTING_FRAGMENT_PREFIX + "/modalBulkForm/{modalMode:import|export}";
	
	/**
	 * /{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/excel
	 */
	public static final String PATH_SETTING_FRAGMENT_EXCEL = PATH_SETTING_FRAGMENT_PREFIX + "/excel";
	
	/** (e) setting path **/
	
	
	/** (s) use path **/
	
	/**
	 * /{adminProjectId}/use/{projectId}/{mainMenuId}/{subMenuId}
	 */
	public static final String PATH_USE_VIEW_INDEX = PATH_USE_PREFIX + "/{projectId}/{mainMenuId}/{subMenuId}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/findAll
	 */
	public static final String PATH_USE_FRAGMENT_LIST = PATH_USE_FRAGMENT_PREFIX + "/list";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM = PATH_USE_FRAGMENT_PREFIX + "/modalForm/{modalMode:create|update}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalForm/{modalMode:delete}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_FORM_DELETE = PATH_USE_FRAGMENT_PREFIX + "/modalForm/{modalMode:delete}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/modalBulkForm/{modalMode:import|export}
	 */
	public static final String PATH_USE_FRAGMENT_MODAL_BULK_FORM = PATH_USE_FRAGMENT_PREFIX + "/modalBulkForm/{modalMode:import|export}";
	
	/**
	 * /{adminProjectId}/use/fragment/{projectId}/{mainMenuId}/{subMenuId}/excel
	 */
	public static final String PATH_USE_FRAGMENT_EXCEL = PATH_USE_FRAGMENT_PREFIX + "/excel";
	
	/** (e) use path **/
	
	
	@AllArgsConstructor
	public static enum UrlResolver {
		VIEW(PATH_SETTING_VIEW_INDEX, PATH_USE_VIEW_INDEX),
		FRAGMENT_LIST(PATH_SETTING_FRAGMENT_LIST, PATH_USE_FRAGMENT_LIST),
		FRAGMENT_MODAL_FORM(PATH_SETTING_FRAGMENT_MODAL_FORM, PATH_USE_FRAGMENT_MODAL_FORM),
		FRAGMENT_MODAL_FORM_DELETE(PATH_SETTING_FRAGMENT_MODAL_FORM_DELETE, PATH_USE_FRAGMENT_MODAL_FORM_DELETE),
		FRAGMENT_MODAL_BULK_FORM(PATH_SETTING_FRAGMENT_MODAL_BULK_FORM, PATH_USE_FRAGMENT_MODAL_BULK_FORM),
		FRAGMENT_EXCEL(PATH_SETTING_FRAGMENT_EXCEL, PATH_USE_FRAGMENT_EXCEL)
		;
		@Getter private String settingUrl;
		@Getter private String useUrl;
		
		
		public static String getUrl(String target, String adminProjectId) {
			var urlResolver = valueOf(target);
			return adminProjectId.equals(AdminConstant.ADMIN_PROJECT_ID_VALUE) ? urlResolver.getSettingUrl() : urlResolver.getUseUrl();
		}
		
//		public static String getUrl(String target, Map<String, String> data) {
//			var adminProjectId = "admin";
//			var urlResolver = valueOf(target);
//			var targetUrl = adminProjectId.equals(SettingConstant.ADMIN_PROJECT_ID_VALUE) ? urlResolver.getSettingUrl() : urlResolver.getUseUrl();
//			// thymeleaf에서는 spel의 :뒤의 값을 처리하지 못함
//			return targetUrl.replaceAll(":[\\w\\|]+", "");
//		}
	}
}
