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
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.DbQueryRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMariadbDbQueryService implements SettingServiceListSupplier<DbQuery> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<DbQuery> ROW_MAPPER = new DbQueryRowMapper();
	
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	public List<DbQuery> findList(SettingParameter settingParameter) {
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
		
		var subMenuId = settingParameter.subMenuId();
		if (!StringUtils.hasText(subMenuId)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_SUBMENUID");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("adminProjectId", adminProjectId);
		paramSource.addValue("projectId", projectId);
		paramSource.addValue("mainMenuId", mainMenuId);
		paramSource.addValue("subMenuId", subMenuId);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM DbQuery WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId", paramSource, ROW_MAPPER);
	}

}
