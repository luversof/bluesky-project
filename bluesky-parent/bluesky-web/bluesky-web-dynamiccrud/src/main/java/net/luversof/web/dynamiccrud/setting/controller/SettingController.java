package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;

/**
 * 관리자 설정용 페이지
 */
@Controller
public class SettingController {

	@GetMapping({ "/setting", "/setting/", "/setting/index" })
	public String redirectIndex() {
		return "redirect:/setting/product";
	}

	@GetMapping(DynamicCrudConstant.PATH_SETTING_VIEW_INDEX)
	public String settings(@PathVariable String type, Model model) {
		model.addAttribute("type", type);
		return "setting/index";
	}

}
