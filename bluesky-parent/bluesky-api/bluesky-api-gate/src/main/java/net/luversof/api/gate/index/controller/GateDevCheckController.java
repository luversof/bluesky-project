package net.luversof.api.gate.index.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.devcheck.core.annotation.DevCheckController;

@DevCheckController
@RestController
@RequestMapping(value = "${bluesky-boot.dev-check.path-prefix}", produces = MediaType.APPLICATION_JSON_VALUE)
public class GateDevCheckController {

	@GetMapping("/test1")
	public String test1() {
		return "test";
	}
	
	@PreAuthorize("hasRole('ROLE_USER')")
	@GetMapping("/test2")
	public String test2() {
		SecurityContext context = SecurityContextHolder.getContext();
		context.getAuthentication();
		return "test2";
		
	}
}
