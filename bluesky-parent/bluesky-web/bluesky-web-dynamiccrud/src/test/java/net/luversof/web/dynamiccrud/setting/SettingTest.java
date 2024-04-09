package net.luversof.web.dynamiccrud.setting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProjectRowMapper;
import net.luversof.web.dynamiccrud.setting.service.DbFieldServiceDecorator;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;
import net.luversof.web.dynamiccrud.setting.service.eventadminmariadb.EventAdminMariadbProjectService;

@Slf4j
public class SettingTest implements GeneralTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private DbFieldServiceDecorator fieldService;
	
	@BeforeAll
	public static void beforeAll() {
		RoutingDataSourceContextHolder.setContext(() -> "dynamiccrud");
	}
	
	
	@Test
	@DisplayName("insert query 생성")
	void insertQuery() {
		// insert query 생성 시 where 조건은 '__org__' 를 붙인 값으로 처리
		String query = "UPDATE SubMenu SET queryString = :queryStriung, dataSourceName = :dataSourceName, dbType = :dbType where productId = :productId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId";
		String[] split = Pattern.compile(" WHERE ", Pattern.CASE_INSENSITIVE).split(query);
		log.debug("test : length: {}, {}", split.length, List.of(split));
	}
	
	
	@Test
	void fieldServiceFind() {
		var fieldPage = fieldService.findList(new SettingParameter(EventAdminConstant.ADMIN_PROJECT_ID_VALUE, EventAdminConstant.PROJECT_ID_VALUE, EventAdminConstant.MAINMENU_ID_VALUE, EventAdminConstant.SUBMENU_ID_VALUE_PROJECT));
		log.debug("fieldPage : {}", fieldPage);
	}
	
	@Test
	void jdbcTemplateProjectSelectTest() {
		List<String> argList = new ArrayList<String>();
		argList.add("test");
		Project product = jdbcTemplate.queryForObject("select * from Project where projectId = ?", new ProjectRowMapper(), argList.toArray());
		log.debug("product : {}", product);
	}
	
	@Test
	void namedParameterJdbcTemplateProjectSelectTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("projectId", "noti");
		var project = namedParameterJdbcTemplate.queryForObject("select * from Project where projectId = :projectId", paramSource, new ProjectRowMapper());
		log.debug("product : {}", project);
	}
	
	/**
	 * 다른 object로 parameter를 만드는 방법
	 * 다만 여러 bean에서 가져와서 만들지는 못하는 듯?
	 */
	@Test
	void namedParameterJdbcTemplateProjectSelectTest2() {
		var queryParameter = new SettingParameter("admin", "noti", "main", "sub");
		var paramSource = new BeanPropertySqlParameterSource(queryParameter);
		Project project = namedParameterJdbcTemplate.queryForObject("select * from Project where projectId = :projectId", paramSource, new ProjectRowMapper());
		log.debug("product : {}", project);
	}
	
	@Test
	void namedParameterJdbcTemplateProjectSelectListTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("projectId", "a%");
//		List<Project> projectList = namedParameterJdbcTemplate.query("select * from Project where projectId = :projectId", paramSource, new ProjectRowMapper());
//		List<Project> projectList = namedParameterJdbcTemplate.query("select * from Project where projectId like '%:projectId%'", paramSource, new ProjectRowMapper());
//		List<Project> projectList = namedParameterJdbcTemplate.query("select * from Project where projectId like '%' + :projectId + '%'", paramSource, new ProjectRowMapper());
		List<Project> projectList = namedParameterJdbcTemplate.query("select * from Project where projectId like :projectId", paramSource, new ProjectRowMapper());
		log.debug("project : {}", projectList);
	}
	

	@Test
	void namedParameterJdbcTemplateProjectSelectCountTest() {
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("projectId", "noti");
		int result = namedParameterJdbcTemplate.queryForObject("select COUNT(*) from Project where projectId = :projectId", paramSource, Integer.class);
		log.debug("result : {}", result);
	}
	
	
	@Autowired
	private EventAdminMariadbProjectService eventAdminMariadbProductService;
	
	@Test
	void eventAdminMysqlProductServiceTest() {
		Project project = eventAdminMariadbProductService.findOne(new SettingParameter("admin", "noti", null, null));
		assertThat(project).isNotNull();
		log.debug("project : {}", project);
	}

}
