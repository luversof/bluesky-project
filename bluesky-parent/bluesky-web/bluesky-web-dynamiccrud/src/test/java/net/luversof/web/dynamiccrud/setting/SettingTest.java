package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.ProductRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud.setting.repository.ProductRepository;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Test
	void productSave() {
		
		var product = new Product();
		product.setProduct("test");
		
		product.setProductName("테스트 프로덕트");
		product.setOperator("bluesky");
		var result = productRepository.save(product);
		
		log.debug("result : {}", result);
		assertThat(result).isNotNull();
		
	}
	
	@Test
	void mainMenuSave() {
		var mainMenu = new MainMenu();
		mainMenu.setProduct("test");
		mainMenu.setMainMenu("testMainMenu");

		mainMenu.setMainMenuName("mainMenuName");
		mainMenu.setOperator("bluesky");
		var result2 = mainMenuRepository.save(mainMenu);
		
		log.debug("result2 : {}", result2);
		assertThat(result2).isNotNull();
		
	}
	
	@Test
	void productFindAll() {
		var result = productRepository.findAll();
		log.debug("result : {}", result);
	}
	
	@Test
	void productFindById() {
		var page = PageRequest.of(0,  10);
		
		var productPage = productRepository.findByProduct("aion", page);
		
		log.debug("page : {}", productPage);
	}
	

	@Test
	void mainMenuFindById() {
		var page = PageRequest.of(0,  10);
		var productPage = mainMenuRepository.findByProduct("aion", page);
		
		log.debug("page : {}", productPage);
	}
	
	@Test
	void jdbcTemplateTest() {
		List<String> argList = new ArrayList<String>();
		argList.add("test");
		Product product = jdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = ?", new ProductRowMapper(), argList.toArray());
		log.debug("product : {}", product);
	}
}
