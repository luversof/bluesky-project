package net.luversof.web.dynamiccrud.use.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

public interface UseService {
	
	Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap);
	
	Object create(SettingParameter settingParameter, Map<String, String> dataMap);
	
	Object update(SettingParameter settingParameter, Map<String, String> dataMap);
	
	Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap);

}
