package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;

public class MainMenuRowMapper extends SettingRowMapper<MainMenu> {

	@Override
	public MainMenu mapRow(ResultSet rs, int rowNum) throws SQLException {
		var mainMenu = new MainMenu(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("mainMenuId"),
			rs.getString("mainMenuName")
		);
		setCommon(mainMenu, rs);
		return mainMenu;
	}

}
