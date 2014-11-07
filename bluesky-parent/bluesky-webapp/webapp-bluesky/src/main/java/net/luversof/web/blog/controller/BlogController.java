package net.luversof.web.blog.controller;

import net.luversof.blog.service.BlogService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/blog")
public class BlogController {

	@Autowired
	private BlogService blogService;

	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/$!", method=RequestMethod.POST)
	public String create(Authentication authentication) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		blogService.save(blueskyUser.getId(), blueskyUser.getUserType().name());
		return "redirect:/blog/$!/article"; 
	}
	
	@RequestMapping("/create")
	public void createForm() {
	}
}
