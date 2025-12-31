package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;

import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;

public class DbQueryRowMapper extends SettingRowMapper<DbQuery> {

	@Override
	public @NonNull DbQuery mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
		var query = new DbQuery(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("mainMenuId"),
			rs.getString("subMenuId"),
			DbQuerySqlCommandType.valueOf(rs.getString("sqlCommandType")),
			rs.getString("dataSourceName"),
			rs.getString("queryString")
		);
		setCommon(query, rs);
		return query;
	}

}
