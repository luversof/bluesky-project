package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

/**
 * Setting 설정 또한 data로 관리
 */
@Service
public class SettingDataService {

	private static final String KEY_EVENT_ADMIN_PRODUCT = "setting";
	private static final String KEY_EVENT_ADMIN_MAINMENU = "menu";
	private static final String KEY_EVENT_ADMIN_SUBMENU1_PRODUCT = "product";
	private static final String KEY_EVENT_ADMIN_SUBMENU2_MAINMENU = "mainMenu";
	private static final String KEY_EVENT_ADMIN_SUBMENU3_SUBMENU = "subMenu";
	private static final String KEY_EVENT_ADMIN_SUBMENU4_QUERY = "query";
	private static final String KEY_EVENT_ADMIN_SUBMENU5_FIELD = "field";
	
	@Getter private Product product;
	@Getter private List<MainMenu> mainMenuList;
	@Getter private List<SubMenu> subMenuList;
	@Getter private List<Query> queryList;
	@Getter private List<Field> fieldList;

	public SettingDataService() {
		
		// 만들어보자...
		{
			product = new Product(); 
			product.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			product.setProductName("Event Admin");
		}
		
		mainMenuList = new ArrayList<>();
		{
			var mainMenu = new MainMenu();
			mainMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			mainMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			mainMenu.setMainMenuName("Event Admin Setting");
			mainMenuList.add(mainMenu);
		}

		// subMenu 부분의 역할을 아직 파악 못함;
		subMenuList = new ArrayList<>();
		{
			var subMenu1 = new SubMenu();
			subMenu1.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu1.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu1.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			subMenu1.setSubMenuName("Product");
			subMenu1.setDisplayOrder(1);
			subMenuList.add(subMenu1);
		}
		
		{
			var subMenu2 = new SubMenu();
			subMenu2.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu2.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu2.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			subMenu2.setSubMenuName("MainMenu");
			subMenu2.setDisplayOrder(2);
			subMenuList.add(subMenu2);
		}
		
		{
			var subMenu3 = new SubMenu();
			subMenu3.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu3.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu3.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			subMenu3.setSubMenuName("SubMenu");
			subMenu3.setDisplayOrder(3);
			subMenuList.add(subMenu3);
		}
		
		{
			var subMenu4 = new SubMenu();
			subMenu4.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu4.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu4.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			subMenu4.setSubMenuName("Query");
			subMenu4.setDisplayOrder(4);
			subMenuList.add(subMenu4);
		}
		
		{
			var subMenu5 = new SubMenu();
			subMenu5.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu5.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu5.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			subMenu5.setSubMenuName("Field");
			subMenu5.setDisplayOrder(5);
			subMenuList.add(subMenu5);
		}
		
		queryList = new ArrayList<>();
		{
			var query1 = new Query();
			query1.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query1.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query1.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query1.setSqlCommandType("SELECT");
			query1.setQueryString("SELECT product, productName, operator, registerDate, modifyDate FROM Products");
			query1.setDataSourceName("dynamiccrud_sample");
			query1.setDbType("mariadb");
			queryList.add(query1);
		}
	}
}
