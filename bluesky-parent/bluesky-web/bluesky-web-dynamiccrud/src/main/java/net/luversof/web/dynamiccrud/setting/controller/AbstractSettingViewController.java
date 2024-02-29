package net.luversof.web.dynamiccrud.setting.controller;

import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;

/**
 * 동일 기능을 path 분기하여 사용하기 위해 제공된은 abstract class
 */
public abstract class AbstractSettingViewController implements SettingViewControllerInterface {

	/**
	 * 동일 기능에 대해 path 분기 처리를 하기
	 * @param adminProjectId
	 * @param projectId
	 * @param mainMenuId
	 * @param subMenuId
	 */
	protected abstract void checkPathVariable(String adminProjectId, String projectId, String mainMenuId, String subMenuId);
	
	@Override
	public String view(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Model model) {
		
		checkPathVariable(adminProjectId, projectId, mainMenuId, subMenuId);
		
		/** (s)  상단 메뉴 처리 **/
		var subMenuList = SettingUtil.getSubMenuList(adminProjectId, projectId, mainMenuId);
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		
		model.addAttribute("subMenuList", subMenuList);
		
		var subMenu = SettingUtil.getSubMenu(adminProjectId, projectId, mainMenuId, subMenuId);
		if (subMenu == null) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		
		
		if (!subMenu.isEnableDisplay()) {
			throw new BlueskyException("NOT_USE_SUBMENU");
		}
		
		// Setting 정보를 기준으로 해당 데이터를 조회
		
		List<DbField> dbFieldList = SettingUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		model.addAttribute("dbFieldList", dbFieldList);
		model.addAttribute("hasSearchField", dbFieldList.stream().anyMatch(x -> x.isEnableSearch()));
		
		/** (e)  상단 메뉴 처리 **/
		
		return "use/index";
	}
}
