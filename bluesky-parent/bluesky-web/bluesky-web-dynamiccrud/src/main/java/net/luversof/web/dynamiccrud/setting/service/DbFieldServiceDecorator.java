package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class DbFieldServiceDecorator implements SettingServiceListSupplier<DbField>, SettingServiceDecorator {

	private Map<String, SettingServiceListSupplier<DbField>> dbFieldServiceMap;

	public DbFieldServiceDecorator(Map<String, SettingServiceListSupplier<DbField>> dbFieldServiceMap) {
		this.dbFieldServiceMap = getSortedSettingServiceMap(dbFieldServiceMap);
	}

	@Override
	public List<DbField> findList(SettingParameter settingParameter) {
		for (var entry : dbFieldServiceMap.entrySet()) {
			var target = entry.getValue().findList(settingParameter);
			if (!target.isEmpty()) {
				return target;
			}
		}
		return Collections.emptyList();
	}

}
