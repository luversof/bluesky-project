package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 설정용 페이지
 */
@Controller
@RequestMapping("/setting")
public class SettingController {

	@GetMapping({ "", "/", "/index", "/{type:product|mainMenu|subMenu|query|field}" })
	public String settings(@PathVariable String type, Model model) {
		model.addAttribute("type", type);
		return "setting/index";
	}

}
