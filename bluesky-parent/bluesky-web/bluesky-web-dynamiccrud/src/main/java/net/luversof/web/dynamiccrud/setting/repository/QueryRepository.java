package net.luversof.web.dynamiccrud.setting.repository;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.Query;

public interface QueryRepository extends SettingRepository<Query, String> {

	List<Query> findByProductAndMainMenuAndSubMenu(String product, String mainMenu, String subMenu);

}
