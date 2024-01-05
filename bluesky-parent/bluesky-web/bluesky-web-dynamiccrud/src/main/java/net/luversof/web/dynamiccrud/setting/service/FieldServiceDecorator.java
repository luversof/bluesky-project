package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class FieldServiceDecorator implements SettingServiceListSupplier<Field> {

	@Autowired
	private Map<String, SettingServiceListSupplier<Field>> fieldServiceMap;

	@Override
	public List<Field> findList(SettingParameter settingParameter) {
		var list = new ArrayList<Field>();
		fieldServiceMap.forEach((key, value) -> list.addAll(value.findList(settingParameter)));
		return list;
	}
	
	
}
