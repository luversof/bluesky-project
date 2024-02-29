package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Project;

public class ProjectRowMapper extends SettingRowMapper<Project> {

	@Override
	public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
		var project = new Project(
			rs.getString("adminProjectId"),
			rs.getString("projectId"),
			rs.getString("projectName"),
			rs.getBoolean("enableMainMenuUI")
		);
		setCommon(project, rs);
		return project;
	}

}
