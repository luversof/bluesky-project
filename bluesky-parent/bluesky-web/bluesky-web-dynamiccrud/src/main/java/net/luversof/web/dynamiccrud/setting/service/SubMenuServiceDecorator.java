package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

@Service
public class SubMenuServiceDecorator implements SettingServiceListSupplier<SubMenu>, SettingServiceDecorator {

	private Map<String, SettingServiceListSupplier<SubMenu>> subMenuServiceMap;

	public SubMenuServiceDecorator(Map<String, SettingServiceListSupplier<SubMenu>> subMenuServiceMap) {
		this.subMenuServiceMap = getSortedSettingServiceMap(subMenuServiceMap);
	}

	@Override
	public List<SubMenu> findList(SettingParameter settingParameter) {
		for (var entry : subMenuServiceMap.entrySet()) {
			var target = entry.getValue().findList(settingParameter);
			if (!target.isEmpty()) {
				return target;
			}
		}
		return Collections.emptyList();
	}

}
