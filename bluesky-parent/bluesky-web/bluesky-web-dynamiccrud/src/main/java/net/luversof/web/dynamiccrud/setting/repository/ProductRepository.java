package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.Product;

public interface ProductRepository extends SettingRepository<Product, String> {

	Page<Product> findByProduct(String product, Pageable pabeable);

}
