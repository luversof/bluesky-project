package net.luversof.web.dynamiccrud.setting.repository;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

public interface SubMenuRepository extends SettingRepository<SubMenu, Long> {
	
	List<SubMenu> findByAdminProjectIdAndProjectIdAndMainMenuId(String adminProjectId, String projectId, String mainMenuId);

}
