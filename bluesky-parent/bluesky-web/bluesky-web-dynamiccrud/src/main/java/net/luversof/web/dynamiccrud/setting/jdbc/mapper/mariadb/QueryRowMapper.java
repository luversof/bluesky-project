package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Query;

public class QueryRowMapper extends SettingRowMapper<Query> {

	@Override
	public Query mapRow(ResultSet rs, int rowNum) throws SQLException {
		var query = new Query(
			rs.getString("product"),
			rs.getString("mainMenu"),
			rs.getString("subMenu"),
			rs.getString("sqlCommandType"),
			rs.getString("queryString"),
			rs.getString("dataSourceName"),
			rs.getString("dbType")
		);
		setCommon(query, rs);
		return query;
	}

}
