package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceDecorator;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;

@Controller
public class SettingFragmentController {
	
	@Autowired
	private SettingServiceDecorator<?> settingService;
	
	@GetMapping(DynamicCrudConstant.PATH_SETTING_FRAGMENT_FIND_ALL)
	public void findAll(@PathVariable String type, SettingParameter settingParameter, Pageable pageable, Model model) {
		model.addAttribute("page", settingService.find(settingParameter, pageable));
	}
	
	
	@GetMapping(DynamicCrudConstant.PATH_SETTING_FRAGMENT_MODAL)
	public void modal(@PathVariable String type) {
	}
	
	/**
	 * 개발 확인용 api
	 */
	@GetMapping("/setting/_components/*")
	public void components() {
	}
	
}
