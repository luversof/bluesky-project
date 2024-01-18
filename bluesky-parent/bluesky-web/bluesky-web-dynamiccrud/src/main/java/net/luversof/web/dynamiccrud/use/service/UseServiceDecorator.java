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
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var subMenu = SettingUtil.getSubMenu(settingParameter);
		if (subMenu.getDbType().equals(SubMenuDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			// query 기준으로 결과 반환 해야 하는데..
			return useService.find(settingParameter, pageable, dataMap);
		}
		return null;
	}
	
	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		var subMenu = SettingUtil.getSubMenu(settingParameter);
		if (subMenu.getDbType().equals(SubMenuDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.create(settingParameter, dataMap);
		}
		return null;
	}

	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		var subMenu = SettingUtil.getSubMenu(settingParameter);
		if (subMenu.getDbType().equals(SubMenuDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.update(settingParameter, dataMap);
		}
		return null;
	}

	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		var subMenu = SettingUtil.getSubMenu(settingParameter);
		if (subMenu.getDbType().equals(SubMenuDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.delete(settingParameter, dataMap);
		}
		return null;
	}
}
