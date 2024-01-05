package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU1_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU2_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU3_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU4_QUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU5_FIELD;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminSubMenuService implements SettingServiceListSupplier<SubMenu> {

	@Getter
	private List<SubMenu> subMenuList;
	
	public EventAdminSubMenuService() {
		loadData();
	}

	private void loadData() {
		subMenuList = new ArrayList<>();
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_PRODUCT);
			subMenu.setMainMenu(KEY_MAINMENU);
			subMenu.setSubMenu(KEY_SUBMENU1_PRODUCT);
			subMenu.setSubMenuName("Product");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 1);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_PRODUCT);
			subMenu.setMainMenu(KEY_MAINMENU);
			subMenu.setSubMenu(KEY_SUBMENU2_MAINMENU);
			subMenu.setSubMenuName("MainMenu");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 2);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_PRODUCT);
			subMenu.setMainMenu(KEY_MAINMENU);
			subMenu.setSubMenu(KEY_SUBMENU3_SUBMENU);
			subMenu.setSubMenuName("SubMenu");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 3);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_PRODUCT);
			subMenu.setMainMenu(KEY_MAINMENU);
			subMenu.setSubMenu(KEY_SUBMENU4_QUERY);
			subMenu.setSubMenuName("Query");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 4);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_PRODUCT);
			subMenu.setMainMenu(KEY_MAINMENU);
			subMenu.setSubMenu(KEY_SUBMENU5_FIELD);
			subMenu.setSubMenuName("Field");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 5);
			subMenuList.add(subMenu);
		}
	}

	@Override
	public List<SubMenu> findList(SettingParameter settingParameter) {
		return subMenuList.stream()
				.filter(x -> x.getProduct().equals(settingParameter.product())
						&& x.getMainMenu().equals(settingParameter.mainMenu()))
				.toList();
	}

}
