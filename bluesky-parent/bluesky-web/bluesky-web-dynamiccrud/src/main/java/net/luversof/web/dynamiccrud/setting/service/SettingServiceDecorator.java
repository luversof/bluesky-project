package net.luversof.web.dynamiccrud.setting.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class SettingServiceDecorator<T extends Setting> implements SettingService<T> {
	
	@Autowired
	private Map<String, SettingService<T>> settingServiceMap;

	@Override
	public Page<T> find(SettingParameter settingParameter, Pageable pageable) {
		return settingServiceMap.get(settingParameter.type() + "Service").find(settingParameter, pageable);
	}
	

}
