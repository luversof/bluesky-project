package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProductRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.FieldRepository;
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud.setting.repository.ProductRepository;
import net.luversof.web.dynamiccrud.setting.repository.QueryRepository;
import net.luversof.web.dynamiccrud.setting.repository.SubMenuRepository;
import net.luversof.web.dynamiccrud.setting.service.FieldServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;
import net.luversof.web.dynamiccrud.setting.service.eventadminmysql.EventAdminMariadbProductService;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@SuppressWarnings("unused")
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
	private FieldServiceDecorator fieldService;
	
	@BeforeAll
	public static void beforeAll() {
		RoutingDataSourceContextHolder.setContext(() -> "dynamiccrud_sample");
	}
	
	
	@Test
	@DisplayName("insert query 생성")
	void insertQuery() {
		// insert query 생성 시 where 조건은 '__org__' 를 붙인 값으로 처리
		String query = "UPDATE SubMenus SET queryString = :queryStriung, dataSourceName = :dataSourceName, dbType = :dbType where product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu";
		String[] split = Pattern.compile(" WHERE ", Pattern.CASE_INSENSITIVE).split(query);
		log.debug("test : length: {}, {}", split.length, List.of(split));
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
	void productRepositoryFind() {
		var result = productRepository.findByProduct("setting", PageRequest.of(0,  10));
		log.debug("result : {}", result);
	}
	
	
	@Test
	void queryRepositoryFind() {
		List<Query> queryList = queryRepository.findByProductAndMainMenuAndSubMenu("setting", "menu", "product");
		log.debug("queryList : {}", queryList);
	}
	
	@Test
	void fieldServiceFind() {
		var fieldPage = fieldService.findList(new SettingParameter(EventAdminConstant.KEY_PRODUCT, EventAdminConstant.KEY_MAINMENU, EventAdminConstant.KEY_SUBMENU1_PRODUCT));
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
		var queryParameter = new SettingParameter("noti", "main", "sub");
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
	
	
	@Autowired
	private EventAdminMariadbProductService eventAdminMariadbProductService;
	
	@Test
	void eventAdminMysqlProductServiceTest() {
		Product product = eventAdminMariadbProductService.findOne(new SettingParameter("noti", null, null));
		assertThat(product).isNotNull();
		log.debug("product : {}", product);
	}

}
