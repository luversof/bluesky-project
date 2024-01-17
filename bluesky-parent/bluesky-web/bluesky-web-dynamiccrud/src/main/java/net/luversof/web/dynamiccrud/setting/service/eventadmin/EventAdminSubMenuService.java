package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_ADMIN_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU1_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU2_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU3_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU4_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU5_DBFIELD;

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
			subMenu.setAdminProjectId(KEY_ADMIN_PROJECT);
			subMenu.setProjectId(KEY_PROJECT);
			subMenu.setMainMenuId(KEY_MAINMENU);
			subMenu.setSubMenuId(KEY_SUBMENU1_PROJECT);
			subMenu.setSubMenuName("Project");
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
			subMenu.setAdminProjectId(KEY_ADMIN_PROJECT);
			subMenu.setProjectId(KEY_PROJECT);
			subMenu.setMainMenuId(KEY_MAINMENU);
			subMenu.setSubMenuId(KEY_SUBMENU2_MAINMENU);
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
			subMenu.setAdminProjectId(KEY_ADMIN_PROJECT);
			subMenu.setProjectId(KEY_PROJECT);
			subMenu.setMainMenuId(KEY_MAINMENU);
			subMenu.setSubMenuId(KEY_SUBMENU3_SUBMENU);
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
			subMenu.setAdminProjectId(KEY_ADMIN_PROJECT);
			subMenu.setProjectId(KEY_PROJECT);
			subMenu.setMainMenuId(KEY_MAINMENU);
			subMenu.setSubMenuId(KEY_SUBMENU4_DBQUERY);
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
			subMenu.setAdminProjectId(KEY_ADMIN_PROJECT);
			subMenu.setProjectId(KEY_PROJECT);
			subMenu.setMainMenuId(KEY_MAINMENU);
			subMenu.setSubMenuId(KEY_SUBMENU5_DBFIELD);
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
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId())
						&& x.getMainMenuId().equals(settingParameter.mainMenuId()))
				.toList();
	}

}
