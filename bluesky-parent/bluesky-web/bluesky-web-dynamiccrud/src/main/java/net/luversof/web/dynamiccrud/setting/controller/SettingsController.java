package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	@GetMapping({ "", "/", "/index", "/{type:product|mainMenu|subMenu|query|field}" })
	public String settings(@PathVariable String type, Model model) {
		model.addAttribute("type", type);
		return "settings/index";
	}

}
