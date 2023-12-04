package net.luversof.web.dynamiccrud.setting.repository;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.Field;

public interface FieldRepository extends SettingRepository<Field, String> {
	
	List<Field> findByProductAndMainMenuAndSubMenu(String product, String MainMenu, String subMenu);

}
