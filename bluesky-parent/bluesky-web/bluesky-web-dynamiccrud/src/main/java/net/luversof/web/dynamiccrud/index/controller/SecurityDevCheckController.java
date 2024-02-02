package net.luversof.web.dynamiccrud.index.controller;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;

@DevCheckController
public class SecurityDevCheckController {
	
	private final String pathPrefix = "/security";
	
	@GetMapping(pathPrefix + "/authorities")
	public Collection<? extends GrantedAuthority> authorities() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
	}	

}
