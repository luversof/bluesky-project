package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProductRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud.setting.repository.ProductRepository;
import net.luversof.web.dynamiccrud.setting.service.FieldService;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private FieldService fieldService;
	
	@BeforeAll
	public static void beforeAll() {
		RoutingDataSourceContextHolder.setContext(() -> "dynamiccrud_sample");
	}
	
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
		
		var productPage = productRepository.findByProduct("noti", page);
		
		log.debug("page : {}", productPage);
	}
	
	@Test
	void fieldServiceFind() {
		var page = PageRequest.of(0,  10);
		var fieldPage = fieldService.find(null, page);
		log.debug("fieldPage : {}", fieldPage);
	}
	
	@Test
	void jdbcTemplateTest() {
		List<String> argList = new ArrayList<String>();
		argList.add("test");
		Product product = jdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = ?", new ProductRowMapper(), argList.toArray());
		log.debug("product : {}", product);
	}
	
	@Test
	void namedParameterJdbcTemplateTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", "noti");
		var product = namedParameterJdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = :product", paramSource, new ProductRowMapper());
		log.debug("product : {}", product);
	}
	
	/**
	 * 다른 object로 parameter를 만드는 방법
	 * 다만 여러 bean에서 가져와서 만들지는 못하는 듯?
	 */
	@Test
	void namedParameterJdbcTemplateTest_1() {
		var queryParameter = new SettingParameter("product", "noti", "main", "sub");
		var paramSource = new BeanPropertySqlParameterSource(queryParameter);
		Product product = namedParameterJdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = :product", paramSource, new ProductRowMapper());
		log.debug("product : {}", product);
	}
	
	@Test
	void namedParameterJdbcTemplateTest2() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", "noti22");
		List<Product> productList = namedParameterJdbcTemplate.query("select product, productName, operator, registerDate, modifyDate from Products where product = :product", paramSource, new ProductRowMapper());
		log.debug("product : {}", productList);
	}
	

	@Test
	void namedParameterJdbcTemplateTest3() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", "noti");
		int result = namedParameterJdbcTemplate.queryForObject("select COUNT(*) from Products where product = :product", paramSource, Integer.class);
		log.debug("result : {}", result);
	}
	
	

}
