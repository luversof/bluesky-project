package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.Project;

public interface ProjectRepository extends SettingRepository<Project, Long> {

	Page<Project> findByAdminProjectIdAndProjectId(String adminProjectId, String projectId, Pageable pabeable);

}
