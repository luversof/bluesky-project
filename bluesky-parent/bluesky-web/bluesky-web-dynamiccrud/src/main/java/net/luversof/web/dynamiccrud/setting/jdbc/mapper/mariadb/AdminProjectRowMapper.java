package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.AdminProject;

public class AdminProjectRowMapper extends SettingRowMapper<AdminProject> {

	@Override
	public AdminProject mapRow(ResultSet rs, int rowNum) throws SQLException {
		var adminProject = new AdminProject(
				rs.getString("adminProjectId"),
				rs.getString("adminProjectName"),
				rs.getString("defaultGrantAuthority"),
				rs.getString("roleHierarchy")
		);
		setCommon(adminProject, rs);
		return adminProject;
	}

}
