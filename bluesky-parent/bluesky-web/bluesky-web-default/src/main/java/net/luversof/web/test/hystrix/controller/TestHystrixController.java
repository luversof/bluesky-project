package net.luversof.web.test.hystrix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.web.test.hystrix.service.TestHystrixService;

@RestController
public class TestHystrixController {

	@Autowired
	private TestHystrixService testHystrixService;
	
	@GetMapping("/testHystrix")
	public String test(@RequestParam String message) {
		return testHystrixService.defaultStores(message);
	}
	
	@GetMapping("/testHystrix2")
	public String test2() {
		return testHystrixService.defaultStores("test2");
	}
	
	@GetMapping("/testHystrix3")
	public String test3() {
		return testHystrixService.defaultStores("ERROR");
	}
}
