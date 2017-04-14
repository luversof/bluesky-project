package net.luversof.web.uidev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiDevController {
	@GetMapping("/UiDev/**/*")
	public void uidev() {};
}
