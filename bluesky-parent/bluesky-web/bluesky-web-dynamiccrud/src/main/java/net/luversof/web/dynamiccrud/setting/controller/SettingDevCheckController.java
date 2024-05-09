package net.luversof.web.dynamiccrud.setting.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.util.ApplicationContextUtil;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Project;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminDbFieldService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminDbQueryService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminMainMenuService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminProjectService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminSubMenuService;

@DevCheckController
public class SettingDevCheckController {

	private static final String PATH_PREFIX = "/setting";
	
	@Autowired
	private EventAdminProjectService eventAdminProjectService;
	
	@Autowired
	private EventAdminMainMenuService eventAdminMainMenuService;
	
	@Autowired
	private EventAdminSubMenuService eventAdminSubMenuService;
	
	@Autowired
	private EventAdminDbQueryService eventAdminQueryService;
	
	@Autowired
	private EventAdminDbFieldService eventAdminFieldService;
	
	@GetMapping(PATH_PREFIX + "/project")
	public Project project() {
		return eventAdminProjectService.getProject();
	}
	
	@GetMapping(PATH_PREFIX + "/mainMenuList")
	public List<MainMenu> mainMenuList() {
		return eventAdminMainMenuService.getMainMenuList();
	}
	
	@GetMapping(PATH_PREFIX + "/subMenuList")
	public List<SubMenu> subMenuList() {
		return eventAdminSubMenuService.getSubMenuList();
	}
	
	@GetMapping(PATH_PREFIX + "/dbQueryList")
	public List<DbQuery> dbQueryList() {
		return eventAdminQueryService.getDbQueryList();
	}
	
	@GetMapping(PATH_PREFIX + "/dbFieldList")
	public List<DbField> dbFieldList() {
		return eventAdminFieldService.getDbFieldList();
	}
	
	@GetMapping(PATH_PREFIX + "/beanTest")
	public Object beanTest( ) {
		ResolvableType type = ResolvableType.forClassWithGenerics(SettingServiceListSupplier.class, DbField.class);
		String[] beanNamesForType = ApplicationContextUtil.getApplicationContext().getBeanNamesForType(type);
//		Map<String, ?> beansOfType = ApplicationContextUtil.getApplicationContext().getBeansOfType(type.getRawClass());
//		ObjectProvider<Object> beanProvider = ApplicationContextUtil.getApplicationContext().getBeanProvider(type);
//		List<Object> beanList = beanProvider.stream().toList();
		return beanNamesForType;
	}
}
