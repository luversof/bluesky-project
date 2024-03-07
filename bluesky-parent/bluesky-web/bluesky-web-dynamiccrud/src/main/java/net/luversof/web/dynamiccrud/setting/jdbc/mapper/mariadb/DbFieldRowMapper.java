package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldSearchType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldVisible;
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
			rs.getShort("columnOrder"),
			rs.getString("columnGroupId"),
			rs.getString("columnDefaultValue"),
			rs.getString("columnPreset"),
			rs.getString("columnFormat"),
			rs.getString("columnValidation"),
			DbFieldVisible.valueOf(rs.getString("columnVisible")),
			DbFieldEnable.valueOf(rs.getString("enableSearch")),
			DbFieldSearchType.convertValue(rs.getString("columnSearchType")),
			rs.getString("columnSearchDefaultValue"),
			rs.getString("columnSearchValidation"),
			DbFieldEnable.valueOf(rs.getString("enableInsert")),
			DbFieldEnable.valueOf(rs.getString("enableUpdate")),
			rs.getString("formHelpText"),
			rs.getString("formPlaceholder")
		);
		setCommon(field, rs);
		return field;
	}

}
