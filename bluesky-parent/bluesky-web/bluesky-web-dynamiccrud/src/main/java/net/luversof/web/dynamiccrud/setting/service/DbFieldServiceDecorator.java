package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class DbFieldServiceDecorator implements SettingServiceListSupplier<DbField> {

	@Autowired
	private Map<String, SettingServiceListSupplier<DbField>> dbFieldServiceMap;

	@Override
	public List<DbField> findList(SettingParameter settingParameter) {
		var list = new ArrayList<DbField>();
		dbFieldServiceMap.forEach((key, value) -> list.addAll(value.findList(settingParameter)));
		return list;
	}
	
	
}
