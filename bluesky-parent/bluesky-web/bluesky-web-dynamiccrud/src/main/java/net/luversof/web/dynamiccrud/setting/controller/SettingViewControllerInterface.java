package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.ui.Model;

/**
 * 동일 기능처리를 여러 path로 처리하기 위해 구성한 interface
 * path 별 권한 부여 목적을 위해 분기 
 */
public interface SettingViewControllerInterface {

	/**
	 * 보기 페이지
	 * @param adminProjectId
	 * @param projectId
	 * @param mainMenuId
	 * @param subMenuId
	 * @param model
	 * @return
	 */
	String view(
			String adminProjectId, 
			String projectId, 
			String mainMenuId, 
			String subMenuId, 
			Model model);

}
