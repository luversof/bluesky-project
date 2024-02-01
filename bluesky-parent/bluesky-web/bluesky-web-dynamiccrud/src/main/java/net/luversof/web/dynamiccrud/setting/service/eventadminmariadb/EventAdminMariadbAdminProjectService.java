package net.luversof.web.dynamiccrud.setting.service.eventadminmariadb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.AdminProject;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.AdminProjectRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

public class EventAdminMariadbAdminProjectService implements SettingServiceSupplier<AdminProject> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<AdminProject> ROW_MAPPER = new AdminProjectRowMapper();
	
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	public AdminProject findOne(SettingParameter settingParameter) {
		var adminProjectId = settingParameter.adminProjectId();
		if (!StringUtils.hasText(adminProjectId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_ADMINPROJECTID");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("adminProjectId", adminProjectId);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM Project WHERE adminProjectId = :adminProjectId", paramSource, ROW_MAPPER).stream().findAny().orElseGet(() -> null);
	}

}