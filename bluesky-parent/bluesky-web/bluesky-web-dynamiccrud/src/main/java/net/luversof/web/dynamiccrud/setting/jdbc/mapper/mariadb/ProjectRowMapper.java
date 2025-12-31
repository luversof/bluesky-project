package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;

import net.luversof.web.dynamiccrud.setting.domain.Project;

public class ProjectRowMapper extends SettingRowMapper<Project> {

	@Override
	public @NonNull Project mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
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
