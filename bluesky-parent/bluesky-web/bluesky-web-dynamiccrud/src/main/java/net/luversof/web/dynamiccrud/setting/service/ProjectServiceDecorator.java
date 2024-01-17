package net.luversof.web.dynamiccrud.setting.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class ProjectServiceDecorator implements SettingServiceSupplier<Project> {

	@Autowired
	private Map<String, SettingServiceSupplier<Project>> projectServiceMap;

	@Override
	public Project findOne(SettingParameter settingParameter) {
		for (var entry: projectServiceMap.entrySet()) {
			var target = entry.getValue().findOne(settingParameter);
			if (target != null) {
				return target;
			}
		}
		return null;
	}
	
	
	
}
