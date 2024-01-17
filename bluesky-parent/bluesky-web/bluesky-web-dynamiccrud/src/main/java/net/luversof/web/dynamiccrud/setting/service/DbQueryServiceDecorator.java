package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class DbQueryServiceDecorator implements SettingServiceListSupplier<DbQuery> {

	@Autowired
	private Map<String, SettingServiceListSupplier<DbQuery>> dbQueryServiceMap;

	@Override
	public List<DbQuery> findList(SettingParameter settingParameter) {
		var list = new ArrayList<DbQuery>();
		dbQueryServiceMap.forEach((key, value) -> list.addAll(value.findList(settingParameter)));
		return list;
	}
	

}
