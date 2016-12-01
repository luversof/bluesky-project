package net.luversof.web.index.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.web.index.service.MenuService;

@RestController
@RequestMapping("/_check/")
public class CheckAdminController {
	
	@Autowired
	private MenuService menuService;

	@GetMapping("/menuLoad")
	public void menuLoad() {
		menuService.load();
	}
}
