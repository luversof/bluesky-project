package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceDecorator;

@Controller
@RequestMapping("/setting/fragment")
public class SettingFragmentController {
	
	@Autowired
	private SettingServiceDecorator<?> settingService;
	
	@GetMapping("/{type:product|mainMenu|subMenu|query|field}/findAll")
	public String page(@PathVariable String type, SettingParameter settingParameter, Pageable pageable, Model model) {
		model.addAttribute("page", settingService.find(settingParameter, pageable));
		return "setting/_fragment/" + type + "Page";
	}
	
}
