package net.luversof.web.gate.thymeleaf.index.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import net.luversof.web.gate.thymeleaf.config.GateThymeleafProperties;

@DevCheckController
public class GateThymeleafDevCheckController {
	
	private final String pathPrefix = "/gate";
	
	@Autowired
	private GateThymeleafProperties gateThymeleafProperties; 

	@DevCheckDescription("gateThymeleafProperties조회")
	@GetMapping(pathPrefix + "/gateThymeleafProperties")
	public GateThymeleafProperties gateThymeleafProperties() {
		return gateThymeleafProperties;
	}
}
