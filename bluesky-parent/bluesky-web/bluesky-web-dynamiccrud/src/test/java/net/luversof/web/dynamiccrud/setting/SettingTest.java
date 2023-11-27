package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.repository.SettingRepository;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private SettingRepository settingRepository;
	
	@Test
	void save() {
		
		var product = new Product();
		product.setName("test");
		product.setOperator("bluesky");
		var result = settingRepository.save(product);
		
		log.debug("result : {}", result);
		assertThat(result).isNotNull();
		
		var mainMenu = new MainMenu();
		mainMenu.setName("test");
		mainMenu.setOperator("bluesky");
		mainMenu.setMainMenuName("mainMenuName");
		var result2 = settingRepository.save(mainMenu);
		
		log.debug("result2 : {}", result2);
		assertThat(result2).isNotNull();
	}
	
	@Test
	void find() {
		var page = PageRequest.of(0,  10);
		
		var product = new Product();
		product.setName("test");
		
		Page<Product> productPage = settingRepository.findByName(product, page);
		
		log.debug("productPage : {}", productPage);
	}
	
}
