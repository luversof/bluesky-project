package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;

public class DbQueryRowMapper extends SettingRowMapper<DbQuery> {

	@Override
	public DbQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
		var query = new DbQuery(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("mainMenuId"),
			rs.getString("subMenuId"),
			DbQuerySqlCommandType.valueOf(rs.getString("sqlCommandType")),
			rs.getString("queryString"),
			rs.getString("dataSourceName")
		);
		setCommon(query, rs);
		return query;
	}

}
