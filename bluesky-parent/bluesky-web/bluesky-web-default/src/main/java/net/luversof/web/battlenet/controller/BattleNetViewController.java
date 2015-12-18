package net.luversof.web.battlenet.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("battleNet/d3")
public class BattleNetViewController {
	
	//@PreAuthorize("hasRole('ROLE_USER_BATTLENET')")
	@RequestMapping("/index")
	public void index() {
	}
	

}
