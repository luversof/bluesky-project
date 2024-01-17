package net.luversof.web.dynamiccrud.setting.repository;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.DbField;

public interface DbFieldRepository extends SettingRepository<DbField, Long> {
	
	List<DbField> findByAdminProjectIdAndProjectIdAndMainMenuIdAndSubMenuId(String adminProjectId, String projectId, String mainMenuId, String subMenuId);

}
