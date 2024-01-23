package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;

public class DbFieldRowMapper extends SettingRowMapper<DbField> {

	@Override
	public DbField mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		var field = new DbField(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("mainMenuId"),
			rs.getString("subMenuId"),
			rs.getString("columnId"),
			rs.getString("columnName"),
			DbFieldColumnType.valueOf(rs.getString("columnType")),
			rs.getString("columnPreset"),
			rs.getString("columnFormat"),
			rs.getString("columnValidation"),
			rs.getString("columnHelpText"),
			rs.getString("columnPlaceholder"),
			rs.getBoolean("columnVisible"),
			DbFieldEnable.valueOf(rs.getString("enableSearch")),
			DbFieldEnable.valueOf(rs.getString("enableEdit")),
			rs.getShort("formSize"),
			rs.getShort("formOrder")
		);
		setCommon(field, rs);
		return field;
	}

}
