package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Field;

public class FieldRowMapper extends SettingRowMapper<Field> {

	@Override
	public Field mapRow(ResultSet rs, int rowNum) throws SQLException {
		var field = new Field(
			rs.getString("product"),
			rs.getString("mainMenu"),
			rs.getString("subMenu"),
			rs.getString("column"),
			rs.getString("name"),
			rs.getString("type"),
			rs.getString("preset"),
			rs.getString("format"),
			rs.getString("validation"),
			rs.getBoolean("visible"),
			rs.getString("enableSearch"),
			rs.getString("enableEdit"),
			rs.getShort("formSize"),
			rs.getShort("formOrder")
		);
		setCommon(field, rs);
		return field;
	}

}
