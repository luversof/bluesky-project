package net.luversof.web.dynamiccrud.setting.repository;

import net.luversof.web.dynamiccrud.setting.domain.Project;

public interface ProjectRepository extends SettingRepository<Project, Long> {

	Project findByAdminProjectIdAndProjectId(String adminProjectId, String projectId);

}
