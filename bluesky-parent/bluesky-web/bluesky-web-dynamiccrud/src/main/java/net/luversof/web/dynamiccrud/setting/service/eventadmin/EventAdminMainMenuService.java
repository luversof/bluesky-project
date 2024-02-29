package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class EventAdminMainMenuService implements SettingServiceSupplier<MainMenu>, SettingServiceListSupplier<MainMenu> {
	
	@Getter private List<MainMenu> mainMenuList;
	
	public EventAdminMainMenuService() {
		loadData();
	}
	
	private void loadData() {
		mainMenuList = new ArrayList<>();
		{
			var mainMenu = new MainMenu(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					"Event Admin Setting",
					true
			);
			mainMenuList.add(mainMenu);
		}	
	}

	@Override
	public MainMenu findOne(SettingParameter settingParameter) {
		return mainMenuList.stream()
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId())
						&& x.getMainMenuId().equals(settingParameter.mainMenuId()))
				.findAny().orElseGet(() -> null);
	}
	
	@Override
	public List<MainMenu> findList(SettingParameter settingParameter) {
		return mainMenuList.stream()
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId()))
				.collect(Collectors.toList());
	}

}


