package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	@GetMapping({ "", "/index" })
	public String settings() {
		return "settings";
	}

}
