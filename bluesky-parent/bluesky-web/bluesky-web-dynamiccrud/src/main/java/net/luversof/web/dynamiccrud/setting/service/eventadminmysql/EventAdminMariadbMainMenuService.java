package net.luversof.web.dynamiccrud.setting.service.eventadminmysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.MainMenuRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMariadbMainMenuService implements SettingServiceSupplier<MainMenu> {

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private static final RowMapper<MainMenu> ROW_MAPPER = new MainMenuRowMapper();
	
	@Override
	public MainMenu findOne(SettingParameter settingParameter) {
		var product = settingParameter.product();
		if (!StringUtils.hasText(product)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_PRODUCT");
		}
		
		var mainMenu = settingParameter.mainMenu();
		if (!StringUtils.hasText(mainMenu)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_MAINMENU");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", product);
		paramSource.addValue("mainMenu", mainMenu);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM MainMenus WHERE product = :product AND mainMenu = :mainMenu", paramSource, ROW_MAPPER).stream().findAny().orElseGet(() -> null);
	}

}


