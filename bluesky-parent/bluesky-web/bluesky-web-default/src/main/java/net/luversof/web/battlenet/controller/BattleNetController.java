package net.luversof.web.battlenet.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.web.AuthorizeRole;

@Controller
@RequestMapping("battleNet")
public class BattleNetController {
	
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping("/index")
	public void index() {
		System.out.println("setasetse");
	}

}
