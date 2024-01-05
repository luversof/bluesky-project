package net.luversof.web.dynamiccrud.setting.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class MainMenuServiceDecorator implements SettingServiceSupplier<MainMenu> {

	@Autowired
	private Map<String, SettingServiceSupplier<MainMenu>> mainMenuServiceMap;

	@Override
	public MainMenu findOne(SettingParameter settingParameter) {
		for (var entry: mainMenuServiceMap.entrySet()) {
			var target = entry.getValue().findOne(settingParameter);
			if (target != null) {
				return target;
			}
		}
		return null;
	}
	

}
