//package net.luversof.web.test.hystrix.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.ModelMap;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import net.luversof.web.test.hystrix.service.TestHystrixService;
//
//@Controller
//@RequestMapping(value = "/", produces = { MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE })
//public class TestHystrixController {
//
//	@Autowired
//	private TestHystrixService testHystrixService;
//	
//	@GetMapping("/testHystrix")
//	public void test(@RequestParam String message, ModelMap modelMap) {
//		modelMap.addAttribute("result", testHystrixService.getStores(message));
//	}
//	
//	@GetMapping("/testHystrix2")
//	public void test2(ModelMap modelMap) {
//		modelMap.addAttribute("result", testHystrixService.getStores("test2"));
//	}
//	
//	@GetMapping("/testHystrix3")
//	public void test3(ModelMap modelMap) {
//		modelMap.addAttribute("result", testHystrixService.getStores("ERROR"));
//	}
//	
//
//	@GetMapping(value = "/testHystrix4", produces = MediaType.APPLICATION_JSON_VALUE)
//	public void test4(ModelMap modelMap) {
//		modelMap.addAttribute("result", testHystrixService.getStores("test4"));
//	}
//	
//	@GetMapping(value = "/testHystrix5", produces = MediaType.TEXT_HTML_VALUE)
//	public void test5(ModelMap modelMap) {
//		modelMap.addAttribute("result", testHystrixService.getStores("test5"));
//	}
//}
