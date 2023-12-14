package net.luversof.web.dynamiccrud.index.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
	
	@GetMapping({ "", "/",})
	public String index() {
		return "redirect:/use/setting/menu/product";
	}

	@GetMapping("/dev")
	public String dev() {
		return "dev";
	}

}
