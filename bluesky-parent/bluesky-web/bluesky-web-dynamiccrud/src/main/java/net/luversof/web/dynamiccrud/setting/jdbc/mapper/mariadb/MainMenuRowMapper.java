package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;

public class MainMenuRowMapper extends SettingRowMapper<MainMenu> {

	@Override
	public @NonNull MainMenu mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
		var mainMenu = new MainMenu(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("mainMenuId"),
			rs.getString("mainMenuName"),
			rs.getBoolean("enableDisplay")
		);
		setCommon(mainMenu, rs);
		return mainMenu;
	}

}
