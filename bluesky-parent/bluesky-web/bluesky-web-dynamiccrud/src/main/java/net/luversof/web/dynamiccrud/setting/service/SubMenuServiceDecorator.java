package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

@Service
public class SubMenuServiceDecorator implements SettingServiceListSupplier<SubMenu> {
	
	@Autowired
	private Map<String, SettingServiceListSupplier<SubMenu>> subMenuServiceMap;

	@Override
	public List<SubMenu> findList(SettingParameter settingParameter) {
		var list = new ArrayList<SubMenu>();
		subMenuServiceMap.forEach((key, value) -> list.addAll(value.findList(settingParameter)));
		return list;
	}

}
