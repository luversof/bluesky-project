package net.luversof.web.dynamiccrud.setting.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class AdminProjectServiceDecorator implements SettingServiceSupplier<AdminProject>, SettingServiceDecorator {

	private Map<String, SettingServiceSupplier<AdminProject>> adminProjectServiceMap;

	public AdminProjectServiceDecorator(Map<String, SettingServiceSupplier<AdminProject>> projectServiceMap) {
		this.adminProjectServiceMap = getSortedSettingServiceMap(projectServiceMap);
	}

	@Override
	public AdminProject findOne(SettingParameter settingParameter) {
		for (var entry : adminProjectServiceMap.entrySet()) {
			var target = entry.getValue().findOne(settingParameter);
			if (target != null) {
				return target;
			}
		}
		return null;
	}

}
