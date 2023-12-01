package net.luversof.web.dynamiccrud.setting.repository;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;

public interface MainMenuRepository extends SettingRepository<MainMenu, String> {

	MainMenu findByProductAndMainMenu(String product, String mainMenu);

}
