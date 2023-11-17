package net.luversof.web.gate.thymeleaf.index.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class IndexThymeleafController {

	@GetMapping({ "/", "/index" })
	public String index() {
		return "index";
	}

	@GetMapping("/ui-components")
	public void uiComponents() {
	}

}
