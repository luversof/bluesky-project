package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PRODUCT;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;

@Service
public class EventAdminMainMenuService implements SettingServiceSupplier<MainMenu> {
	
	@Getter private List<MainMenu> mainMenuList;
	
	public EventAdminMainMenuService() {
		loadData();
	}
	
	private void loadData() {
		mainMenuList = new ArrayList<>();
		{
			var mainMenu = new MainMenu(
					KEY_PRODUCT,
					KEY_MAINMENU,
				"Event Admin Setting"
			);
			mainMenuList.add(mainMenu);
		}	
	}

	@Override
	public MainMenu findOne(SettingParameter settingParameter) {
		return mainMenuList.stream()
				.filter(x -> x.getProduct().equals(settingParameter.product())
						&& x.getMainMenu().equals(settingParameter.mainMenu()))
				.findAny().orElseGet(() -> null);
	}

}


