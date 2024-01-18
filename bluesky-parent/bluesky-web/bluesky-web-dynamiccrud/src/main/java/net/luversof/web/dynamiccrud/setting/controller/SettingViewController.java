package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class SettingViewController extends AbstractSettingViewController {
	
	@GetMapping(UrlConstant.PATH_SETTING_VIEW_INDEX)
	public String view(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Model model) {
		model.addAttribute("adminProjectId", SettingConstant.KEY_ADMIN_PROJECT);
		return viewInternal(SettingConstant.KEY_ADMIN_PROJECT, projectId, mainMenuId, subMenuId, model);
	}

	@Override
	protected void checkPathVariable(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		
	}
			
}
