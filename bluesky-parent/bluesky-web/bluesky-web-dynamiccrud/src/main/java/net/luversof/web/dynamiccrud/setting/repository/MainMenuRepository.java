package net.luversof.web.dynamiccrud.setting.repository;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;

public interface MainMenuRepository extends SettingRepository<MainMenu, Long> {

	MainMenu findByAdminProjectIdAndProjectIdAndMainMenuId(String adminProjectId, String projectId, String mainMenuId);

}
