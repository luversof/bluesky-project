package net.luversof.web.dynamiccrud.setting.service.eventadminmysql;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.FieldRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMysqlFieldService implements SettingServiceListSupplier<Field> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<Field> ROW_MAPPER = new FieldRowMapper();

	@Override
	public List<Field> findList(SettingParameter settingParameter) {
		var product = settingParameter.product();
		if (!StringUtils.hasText(product)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_PRODUCT");
		}
		
		var mainMenu = settingParameter.mainMenu();
		if (!StringUtils.hasText(mainMenu)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_MAINMENU");
		}
		
		var subMenu = settingParameter.subMenu();
		if (!StringUtils.hasText(subMenu)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_SUBMENU");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", product);
		paramSource.addValue("mainMenu", mainMenu);
		paramSource.addValue("subMenu", subMenu);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM Fields WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu", paramSource, ROW_MAPPER);
	}

}
