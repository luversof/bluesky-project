package net.luversof.web.asset.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.asset.service.AssetService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/user/{id}/asset")
public class AssetController {
	
	@Autowired
	private AssetService assetService;

	@RequestMapping(value = { "" })
	public String list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		log.debug("modelMap : {}", modelMap);
		return "/asset/list";
	}
}
