package net.luversof.web.gate.thymeleaf.dynamiccrud.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * domain에 따라 동적으로 crud를 처리하는 방식을 구현해보자.
 */
@Controller
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class DynamicCrudController {

	
	@GetMapping
	public 	String index(@RequestParam String type) {
		return "crudtest/index";
	}
}
