package net.luversof.web.dynamiccrud.setting.repository;

import net.luversof.web.dynamiccrud.setting.domain.AdminProject;

public interface AdminProjectRepository extends SettingRepository<AdminProject, Long> {

	AdminProject findByAdminProjectId(String adminProjectId);

}
