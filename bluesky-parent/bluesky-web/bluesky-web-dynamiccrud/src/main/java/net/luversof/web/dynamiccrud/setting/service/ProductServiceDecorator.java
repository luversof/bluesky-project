package net.luversof.web.dynamiccrud.setting.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@Service
public class ProductServiceDecorator implements SettingServiceSupplier<Product> {

	@Autowired
	private Map<String, SettingServiceSupplier<Product>> productServiceMap;

	@Override
	public Product findOne(SettingParameter settingParameter) {
		for (var entry: productServiceMap.entrySet()) {
			var target = entry.getValue().findOne(settingParameter);
			if (target != null) {
				return target;
			}
		}
		return null;
	}
	
	
	
}
