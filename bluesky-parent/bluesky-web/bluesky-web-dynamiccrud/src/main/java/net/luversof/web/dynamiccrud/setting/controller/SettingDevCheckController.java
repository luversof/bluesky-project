package net.luversof.web.dynamiccrud.setting.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminFieldService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminMainMenuService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminProductService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminQueryService;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminSubMenuService;

@DevCheckController
public class SettingDevCheckController {

	private final String pathPrefix = "/setting";
	
	@Autowired
	private EventAdminProductService eventAdminProductService;
	
	@Autowired
	private EventAdminMainMenuService eventAdminMainMenuService;
	
	@Autowired
	private EventAdminSubMenuService eventAdminSubMenuService;
	
	@Autowired
	private EventAdminQueryService eventAdminQueryService;
	
	@Autowired
	private EventAdminFieldService eventAdminFieldService;
	
	@GetMapping(pathPrefix + "/product")
	public Product product() {
		return eventAdminProductService.getProduct();
	}
	
	@GetMapping(pathPrefix + "/mainMenuList")
	public List<MainMenu> mainMenuList() {
		return eventAdminMainMenuService.getMainMenuList();
	}
	
	@GetMapping(pathPrefix + "/subMenuList")
	public List<SubMenu> subMenuList() {
		return eventAdminSubMenuService.getSubMenuList();
	}
	
	@GetMapping(pathPrefix + "/queryList")
	public List<Query> queryList() {
		return eventAdminQueryService.getQueryList();
	}
	
	@GetMapping(pathPrefix + "/fieldList")
	public List<Field> fieldList() {
		return eventAdminFieldService.getFieldList();
	}
}
