package net.luversof.web.dynamiccrud.setting.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

public interface SettingService<T extends Setting> {
	
	Page<T> find(SettingParameter settingParameter, Pageable pageable);

}
