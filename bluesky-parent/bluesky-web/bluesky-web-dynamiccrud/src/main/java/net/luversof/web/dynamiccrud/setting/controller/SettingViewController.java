package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class SettingViewController extends AbstractSettingViewController {
	
	@GetMapping(UrlConstant.PATH_SETTING_VIEW_INDEX)
	public String settingView(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Model model) {
		model.addAttribute(SettingConstant.ADMIN_PROJECT_ID, AdminConstant.ADMIN_PROJECT_ID_VALUE);
		return view(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, model);
	}

	@Override
	protected void checkPathVariable(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		
	}
			
}
