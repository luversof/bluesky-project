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
					KEY_ADMIN_PROJECT,
					KEY_PROJECT,
					KEY_MAINMENU,
					KEY_SUBMENU1_PROJECT,
					"Project",
					SubMenuDbType.MySql,
					(short) 1,
					(short) 20,
					true,
					true,
					true,
					true
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					KEY_ADMIN_PROJECT,
					KEY_PROJECT,
					KEY_MAINMENU,
					KEY_SUBMENU2_MAINMENU,
					"MainMenu",
					SubMenuDbType.MySql,
					(short) 2,
					(short) 20,
					true,
					true,
					true,
					true
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					KEY_ADMIN_PROJECT,
					KEY_PROJECT,
					KEY_MAINMENU,
					KEY_SUBMENU3_SUBMENU,
					"SubMenu",
					SubMenuDbType.MySql,
					(short) 3,
					(short) 20,
					true,
					true,
					true,
					true
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					KEY_ADMIN_PROJECT,
					KEY_PROJECT,
					KEY_MAINMENU,
					KEY_SUBMENU4_DBQUERY,
					"Query",
					SubMenuDbType.MySql,
					(short) 4,
					(short) 20,
					true,
					true,
					true,
					true
			);
			subMenuList.add(subMenu);
		}

		{
			var subMenu = new SubMenu(
					KEY_ADMIN_PROJECT,
					KEY_PROJECT,
					KEY_MAINMENU,
					KEY_SUBMENU5_DBFIELD,
					"Field",
					SubMenuDbType.MySql,
					(short) 5,
					(short) 20,
					true,
					true,
					true,
					true
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
				.toList();
	}

}
