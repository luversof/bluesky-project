package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

public interface SubMenuRepository extends SettingRepository<SubMenu, String> {
	
	Page<SubMenu> findByProduct(String product, Pageable pabeable);
	
	Page<SubMenu> findByMainMenu(String mainMenu, Pageable pabeable);
	
	Page<SubMenu> findBySubMenu(String subMenu, Pageable pabeable);
	
	Page<SubMenu> findByProductAndMainMenu(String product, String mainMenu, Pageable pabeable);
	
	Page<SubMenu> findByProductAndSubMenu(String product, String subMenu, Pageable pabeable);
	
	Page<SubMenu> findByMainMenuAndSubMenu(String mainMenu, String subMenu, Pageable pabeable);
	
	Page<SubMenu> findByProductAndMainMenuAndSubMenu(String product, String mainMenu, String subMenu, Pageable pabeable);

}
