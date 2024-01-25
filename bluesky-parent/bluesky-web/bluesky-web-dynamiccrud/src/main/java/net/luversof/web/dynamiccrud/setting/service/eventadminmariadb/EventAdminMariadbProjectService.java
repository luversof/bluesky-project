package net.luversof.web.dynamiccrud.setting.service.eventadminmariadb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProjectRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMariadbProjectService implements SettingServiceSupplier<Project> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<Project> ROW_MAPPER = new ProjectRowMapper();
	
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Project findOne(SettingParameter settingParameter) {
		var adminProjectId = settingParameter.adminProjectId();
		if (!StringUtils.hasText(adminProjectId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_ADMINPROJECTID");
		}
		
		var projectId = settingParameter.projectId();
		if (!StringUtils.hasText(projectId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_PROJECTID");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("adminProjectId", adminProjectId);
		paramSource.addValue("projectId", projectId);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM Project WHERE adminProjectId = :adminProjectId AND projectId = :projectId", paramSource, ROW_MAPPER).stream().findAny().orElseGet(() -> null);
	}

}
