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
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.SubMenuRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMariadbSubMenuService implements SettingServiceListSupplier<SubMenu> {

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<SubMenu> ROW_MAPPER = new SubMenuRowMapper();

	
	@Override
	public List<SubMenu> findList(SettingParameter settingParameter) {
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
		
		return namedParameterJdbcTemplate.query("SELECT * FROM SubMenus WHERE product = :product AND mainMenu = :mainMenu", paramSource, ROW_MAPPER);
	}

}
