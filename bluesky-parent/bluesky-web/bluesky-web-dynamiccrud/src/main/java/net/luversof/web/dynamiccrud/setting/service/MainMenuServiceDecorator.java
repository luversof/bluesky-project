package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class MainMenuServiceDecorator implements SettingServiceSupplier<MainMenu>, SettingServiceListSupplier<MainMenu>, SettingServiceDecorator {

	private Map<String, SettingServiceSupplier<MainMenu>> mainMenuServiceMap;
	
	private Map<String, SettingServiceListSupplier<MainMenu>> mainMenuServiceListMap;

	public MainMenuServiceDecorator(Map<String, SettingServiceSupplier<MainMenu>> mainMenuServiceMap, Map<String, SettingServiceListSupplier<MainMenu>> mainMenuServiceListMap) {
		this.mainMenuServiceMap = getSortedSettingServiceMap(mainMenuServiceMap);
		this.mainMenuServiceListMap = getSortedSettingServiceMap(mainMenuServiceListMap);
	}

	@Override
	public MainMenu findOne(SettingParameter settingParameter) {
		for (var entry : mainMenuServiceMap.entrySet()) {
			var target = entry.getValue().findOne(settingParameter);
			if (target != null) {
				return target;
			}
		}
		return null;
	}
	
	@Override
	public List<MainMenu> findList(SettingParameter settingParameter) {
		for (var entry : mainMenuServiceListMap.entrySet()) {
			var target = entry.getValue().findList(settingParameter);
			if (!target.isEmpty()) {
				return target;
			}
		}
		return Collections.emptyList();
	}

}
