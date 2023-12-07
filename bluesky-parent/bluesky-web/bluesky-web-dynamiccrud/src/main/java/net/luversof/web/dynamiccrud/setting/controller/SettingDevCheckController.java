package net.luversof.web.dynamiccrud.setting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import net.luversof.web.dynamiccrud.setting.service.SettingDataService;

@DevCheckController
public class SettingDevCheckController {

	private final String pathPrefix = "/setting";
	
	@Autowired
	private SettingDataService settingDataService;
	
	@DevCheckDescription("settingData reload")
	@GetMapping(pathPrefix + "/reloadSettingData")
	public boolean reloadSettingData() {
		settingDataService.loadData();
		return true;
	}
}
