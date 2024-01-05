package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class QueryServiceDecorator implements SettingServiceListSupplier<Query> {

	@Autowired
	private Map<String, SettingServiceListSupplier<Query>> queryServiceMap;

	@Override
	public List<Query> findList(SettingParameter settingParameter) {
		var list = new ArrayList<Query>();
		queryServiceMap.forEach((key, value) -> list.addAll(value.findList(settingParameter)));
		return list;
	}
	

}
