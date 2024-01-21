package net.luversof.web.dynamiccrud.use.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;

@Service
public class UseServiceDecorator implements UseService {
	
	@Autowired
	private Map<String, UseService> useServiceMap;
	
	@Override
	public SubMenuDbType getSupportDbType() {
		return null;
	}

	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var useService = getUseService(settingParameter);
		return useService == null ? null : useService.find(settingParameter, pageable, dataMap);
	}
	
	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		var useService = getUseService(settingParameter);
		return useService == null ? null : useService.create(settingParameter, dataMap);
	}

	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		var useService = getUseService(settingParameter);
		return useService == null ? null : useService.update(settingParameter, dataMap);
	}

	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		var useService = getUseService(settingParameter);
		return useService == null ? null : useService.delete(settingParameter, dataMap);
	}
	
	private UseService getUseService(SettingParameter settingParameter) {
		var subMenu = SettingUtil.getSubMenu(settingParameter);
		for (var useService : useServiceMap.values()) {
			if (subMenu.getDbType().equals(useService.getSupportDbType())) {
				return useService;
			}
		}
		return null;
	}


}
