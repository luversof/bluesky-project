package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProductRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.FieldRepository;
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud.setting.repository.ProductRepository;
import net.luversof.web.dynamiccrud.setting.repository.QueryRepository;
import net.luversof.web.dynamiccrud.setting.repository.SubMenuRepository;
import net.luversof.web.dynamiccrud.setting.service.FieldService;
import net.luversof.web.dynamiccrud.setting.service.SettingDataService;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@Autowired
	private SubMenuRepository subMenuRepository;
	
	@Autowired
	private QueryRepository queryRepository;
	
	@Autowired
	private FieldRepository fieldRepository;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private FieldService fieldService;
	
	@Autowired
	private SettingDataService settingDataService;
	
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
	@Disabled
	void productRepositorySave() {
		Product product = settingDataService.getProduct();
		log.debug("product : {}", product);
		
		var result = productRepository.save(product);
		log.debug("result : {}", result);
	}
	
	@Test
	void productRepositoryFind() {
		var result = productRepository.findByProduct("setting", PageRequest.of(0,  10));
		log.debug("result : {}", result);
	}
	
	@Test
	@Disabled
	void mainMenuRepositorySave() {
		List<MainMenu> mainMenuList = settingDataService.getMainMenuList();
		log.debug("mainMenuList : {}", mainMenuList);
		
		var result = mainMenuRepository.saveAll(mainMenuList);
		log.debug("result : {}", result);
	}
	
	@Test
	@Disabled
	void subMenuRepositorySave() {
		List<SubMenu> subMenuList = settingDataService.getSubMenuList();
		log.debug("subMenuList : {}", subMenuList);
		
		var result = subMenuRepository.saveAll(subMenuList);
		log.debug("result : {}", result);
	}
	
	@Test
	@Disabled
	void queryRepositorySave() {
		List<Query> queryMenuList = settingDataService.getQueryList();
		log.debug("queryMenuList : {}", queryMenuList);
		
		var result = queryRepository.saveAll(queryMenuList);
		log.debug("result : {}", result);
	}
	
	@Test
	void queryRepositoryFind() {
		List<Query> queryList = queryRepository.findByProductAndMainMenuAndSubMenu("setting", "menu", "product");
		log.debug("queryList : {}", queryList);
	}
	
	@Test
	@Disabled
	void fieldRepositorySave() {
		List<Field> fieldList = settingDataService.getFieldList();
		log.debug("fieldList : {}", fieldList);
		
		var result = fieldRepository.saveAll(fieldList);
		log.debug("result : {}", result);
	}
	
	@Test
	void fieldServiceFind() {
		var page = PageRequest.of(0,  10);
		var fieldPage = fieldService.find(null, page);
		log.debug("fieldPage : {}", fieldPage);
	}
	
	@Test
	void fieldRepositoryFind() {
		List<Field> fieldList = fieldRepository.findByProductAndMainMenuAndSubMenu("setting", "menu", "product");
		log.debug("fieldList : {}", fieldList);
	}
	
	
	@Test
	void jdbcTemplateProjectSelectTest() {
		List<String> argList = new ArrayList<String>();
		argList.add("test");
		Product product = jdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = ?", new ProductRowMapper(), argList.toArray());
		log.debug("product : {}", product);
	}
	
	@Test
	void namedParameterJdbcTemplateProjectSelectTest() {
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
	void namedParameterJdbcTemplateProjectSelectTest2() {
		var queryParameter = new SettingParameter("product", "noti", "main", "sub");
		var paramSource = new BeanPropertySqlParameterSource(queryParameter);
		Product product = namedParameterJdbcTemplate.queryForObject("select product, productName, operator, registerDate, modifyDate from Products where product = :product", paramSource, new ProductRowMapper());
		log.debug("product : {}", product);
	}
	
	@Test
	void namedParameterJdbcTemplateProjectSelectListTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", "noti22");
		List<Product> productList = namedParameterJdbcTemplate.query("select product, productName, operator, registerDate, modifyDate from Products where product = :product", paramSource, new ProductRowMapper());
		log.debug("product : {}", productList);
	}
	

	@Test
	void namedParameterJdbcTemplateProjectSelectCountTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", "noti");
		int result = namedParameterJdbcTemplate.queryForObject("select COUNT(*) from Products where product = :product", paramSource, Integer.class);
		log.debug("result : {}", result);
	}
	
	

}
