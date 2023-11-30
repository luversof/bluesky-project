package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

public class SubMenuRowMapper extends SettingRowMapper<SubMenu> {

	@Override
	public SubMenu mapRow(ResultSet rs, int rowNum) throws SQLException {
		var subMenu = new SubMenu(
			rs.getString("product"),
			rs.getString("mainMenu"),
			rs.getString("subMenu"),
			rs.getString("subMenuName"),
			rs.getString("template"),
			rs.getInt("displayOrder"),
			rs.getShort("groupNo"),
			rs.getString("groupTemplate"),
			rs.getShort("pageSize"),
			rs.getBoolean("enableCount"),
			rs.getBoolean("enableExcel"),
			rs.getBoolean("enableInsert"),
			rs.getBoolean("enableUpdate"),
			rs.getBoolean("enableDelete")
		);
		setCommon(subMenu, rs);
		return subMenu;
	}

}
