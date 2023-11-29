package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;

public interface MainMenuRepository extends SettingRepository<MainMenu, String> {

	Page<MainMenu> findByProduct(String product, Pageable pabeable);
	
	Page<MainMenu> findByMainMenu(String mainMenu, Pageable pabeable);
	
	Page<MainMenu> findByProductAndMainMenu(String product, String mainMenu, Pageable pabeable);

}
