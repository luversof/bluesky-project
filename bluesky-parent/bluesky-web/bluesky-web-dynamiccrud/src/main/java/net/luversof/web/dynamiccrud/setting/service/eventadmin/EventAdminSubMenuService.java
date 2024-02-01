package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_ADMINPROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBFIELD;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_SUBMENU;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
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
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_ADMINPROJECT,
					"AdminProject",
					SubMenuDbType.MySql,
					(short) 1,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}
		
		{
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					"Project",
					SubMenuDbType.MySql,
					(short) 2,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_MAINMENU,
					"MainMenu",
					SubMenuDbType.MySql,
					(short) 3,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_SUBMENU,
					"SubMenu",
					SubMenuDbType.MySql,
					(short) 4,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBQUERY,
					"Query",
					SubMenuDbType.MySql,
					(short) 5,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBFIELD,
					"Field",
					SubMenuDbType.MySql,
					(short) 6,
					(short) 20,
					true,
					true,
					true,
					true,
					null
			);
			subMenuList.add(subMenu);
		}
	}

	@Override
	public List<SubMenu> findList(SettingParameter settingParameter) {
		return subMenuList.stream()
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId())
						&& x.getMainMenuId().equals(settingParameter.mainMenuId()))
				.sorted(Comparator.comparing(SubMenu::getDisplayOrder))
				.collect(Collectors.toList());
	}

}
