package net.luversof.web.dynamiccrud.setting.service.eventadminmariadb;

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
		var adminProjectId = settingParameter.adminProjectId();
		if (!StringUtils.hasText(adminProjectId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_ADMINPROJECTID");
		}
		
		var projectId = settingParameter.projectId();
		if (!StringUtils.hasText(projectId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_PROJECTID");
		}
		
		var mainMenuId = settingParameter.mainMenuId();
		if (!StringUtils.hasText(mainMenuId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_MAINMENUID");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("adminProjectId", adminProjectId);
		paramSource.addValue("projectId", projectId);
		paramSource.addValue("mainMenuId", mainMenuId);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM SubMenu WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId", paramSource, ROW_MAPPER);
	}

}
