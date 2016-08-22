package net.luversof.web.bbs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.service.BbsService;

@RestController
@RequestMapping(value = "/board/{boardAlias}", produces = MediaType.APPLICATION_JSON_VALUE)
public class BbsController {

	@Autowired
	private BbsService bbsService;
	
	@GetMapping
	public Bbs get(@PathVariable String boardAlias) {
		return bbsService.findByAlias(boardAlias); 
	}
}
