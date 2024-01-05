package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PRODUCT;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class EventAdminProductService implements SettingServiceSupplier<Product> {
	
	@Getter private Product product;
	
	public EventAdminProductService() {
		loadData();
	}
	
	private void loadData() {
		product = new Product(
			KEY_PRODUCT,
			"Event Admin"
		); 
	}

	@Override
	public Product findOne(SettingParameter settingParameter) {
		if (product.getProduct().equals(settingParameter.product())) {
			return product;
		}
		return null;
	}

}
