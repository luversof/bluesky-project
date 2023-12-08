package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.FieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.FieldType;

public class FieldRowMapper extends SettingRowMapper<Field> {

	@Override
	public Field mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		var field = new Field(
			rs.getString("product"),
			rs.getString("mainMenu"),
			rs.getString("subMenu"),
			rs.getString("column"),
			rs.getString("name"),
			FieldType.valueOf(rs.getString("type")),
			rs.getString("preset"),
			rs.getString("format"),
			rs.getString("validation"),
			rs.getBoolean("visible"),
			FieldEnable.valueOf(rs.getString("enableSearch")),
			FieldEnable.valueOf(rs.getString("enableEdit")),
			rs.getShort("formSize"),
			rs.getShort("formOrder")
		);
		setCommon(field, rs);
		return field;
	}

}
