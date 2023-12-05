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
	
	private static final String DB_TYPE = "mariadb"; 
	private static final String DATASOURCE_NAME = "dynamiccrud_sample"; 
	
	@Getter private Product product;
	@Getter private List<MainMenu> mainMenuList;
	@Getter private List<SubMenu> subMenuList;
	@Getter private List<Query> queryList;
	@Getter private List<Field> fieldList;

	public SettingDataService() {
		
		// 만들어보자...
		{
			product = new Product(
				KEY_EVENT_ADMIN_PRODUCT,
				"Event Admin"
			); 
		}
		
		mainMenuList = new ArrayList<>();
		{
			var mainMenu = new MainMenu(
				KEY_EVENT_ADMIN_PRODUCT,
				KEY_EVENT_ADMIN_MAINMENU,
				"Event Admin Setting"
			);
			mainMenuList.add(mainMenu);
		}

		// subMenu 부분의 역할을 아직 파악 못함;
		subMenuList = new ArrayList<>();
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			subMenu.setSubMenuName("Product");
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder(1);
			subMenuList.add(subMenu);
		}
		
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			subMenu.setSubMenuName("MainMenu");
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder(2);
			subMenuList.add(subMenu);
		}
		
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			subMenu.setSubMenuName("SubMenu");
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder(3);
			subMenuList.add(subMenu);
		}
		
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			subMenu.setSubMenuName("Query");
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder(4);
			subMenuList.add(subMenu);
		}
		
		{
			var subMenu = new SubMenu();
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			subMenu.setSubMenuName("Field");
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder(5);
			subMenuList.add(subMenu);
		}
		
		queryList = new ArrayList<>();
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query.setSqlCommandType("SELECT");
			query.setQueryString("SELECT product, productName, operator, registerDate, modifyDate FROM Products");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			query.setSqlCommandType("SELECT");
			query.setQueryString("SELECT product, mainMenu, mainMenuName, operator, registerDate, modifyDate FROM MainMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			query.setSqlCommandType("SELECT");
			query.setQueryString("SELECT product, mainMenu, subMenu, subMenuName, template, displayOrder, groupNo, groupTemplate, pageSize, enableCount, enableExcel, enableInsert, enableUpdate, enableDelete, operator, registerDate, modifyDate FROM SubMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			query.setSqlCommandType("SELECT");
			query.setQueryString("SELECT product, mainMenu, subMenu, sqlCommandType, queryString, dataSourceName, dbType, operator, registerDate, modifyDate FROM Queries");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			query.setSqlCommandType("SELECT");
			query.setQueryString("SELECT product, mainMenu, subMenu, `column`, `name`, `type`, preset, `format`, `validation`, visible, enableSearch, enableEdit, formSize, formOrder, operator, registerDate, modifyDate FROM Fields");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		fieldList = new ArrayList<>();
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			field.setColumn("product");
			field.setName("프로덕트 ID");
			field.setVisible(true);
			field.setFormOrder((short) 1);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			field.setColumn("productName");
			field.setName("프로덕트 이름");
			field.setVisible(true);
			field.setFormOrder((short) 2);
			fieldList.add(field);
		}
		
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
		
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			field.setColumn("product");
			field.setName("프로덕트 ID");
			field.setVisible(true);
			field.setFormOrder((short) 1);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			field.setColumn("mainMenu");
			field.setName("프로덕트 ID");
			field.setVisible(true);
			field.setFormOrder((short) 2);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			field.setColumn("mainMenuName");
			field.setName("메인메뉴 명");
			field.setVisible(true);
			field.setFormOrder((short) 3);
			fieldList.add(field);
		}
		
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
	}
	
	private void addDefaultField(String product, String mainMenu, String SubMenu) {
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(SubMenu);
			field.setColumn("operator");
			field.setName("최종 수정");
			field.setVisible(true);
			field.setFormOrder((short) 20);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(SubMenu);
			field.setColumn("modifyDate");
			field.setName("수정일");
			field.setVisible(true);
			field.setFormOrder((short) 21);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(SubMenu);
			field.setColumn("registerDate");
			field.setName("등록일");
			field.setVisible(false);
			field.setFormOrder((short) 22);
			fieldList.add(field);
		}
	}
}
