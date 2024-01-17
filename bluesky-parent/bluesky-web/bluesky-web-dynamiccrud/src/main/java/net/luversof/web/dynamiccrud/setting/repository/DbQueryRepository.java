package net.luversof.web.dynamiccrud.setting.repository;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.DbQuery;

public interface DbQueryRepository extends SettingRepository<DbQuery, Long> {

	List<DbQuery> findByAdminProjectIdAndProjectIdAndMainMenuIdAndSubMenuId(String adminProjectId, String projectId, String mainMenuId, String subMenuId);

}
