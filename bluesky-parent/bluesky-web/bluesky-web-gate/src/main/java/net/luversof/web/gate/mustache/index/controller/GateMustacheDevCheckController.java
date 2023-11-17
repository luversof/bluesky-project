package net.luversof.web.gate.mustache.index.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;

@DevCheckController
public class GateMustacheDevCheckController {
	
	private final String pathPrefix = "/gate";

	@Autowired
	private MustacheProperties mustacheProperties;
	
	@DevCheckDescription("gateMustacheProperties 조회")
	@GetMapping(pathPrefix + "/gateMustacheProperties")
	public MustacheProperties mustacheProperties() {
		return mustacheProperties;
	}
	
}
