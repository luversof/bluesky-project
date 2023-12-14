package net.luversof.web.dynamiccrud.setting.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.FieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.FieldType;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.QueryDbType;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;

/**
 * Setting 설정 또한 data로 관리
 */
@Service
public class SettingDataService {

	public static final String KEY_EVENT_ADMIN_PRODUCT = "setting";
	private static final String KEY_EVENT_ADMIN_MAINMENU = "menu";
	private static final String KEY_EVENT_ADMIN_SUBMENU1_PRODUCT = "product";
	private static final String KEY_EVENT_ADMIN_SUBMENU2_MAINMENU = "mainMenu";
	private static final String KEY_EVENT_ADMIN_SUBMENU3_SUBMENU = "subMenu";
	private static final String KEY_EVENT_ADMIN_SUBMENU4_QUERY = "query";
	private static final String KEY_EVENT_ADMIN_SUBMENU5_FIELD = "field";
	private static final QueryDbType DB_TYPE = QueryDbType.MySql; 
	
	public static final String DATASOURCE_NAME = "dynamiccrud_sample";
	
	@Getter private Product product;
	@Getter private List<MainMenu> mainMenuList;
	@Getter private List<SubMenu> subMenuList;
	@Getter private List<Query> queryList;
	@Getter private List<Field> fieldList;
	
	public SettingDataService() {
		loadData();
	}

	public void loadData() {
		
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
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
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
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
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
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
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
			subMenu.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			subMenu.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			subMenu.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			subMenu.setSubMenuName("Field");
			subMenu.setTemplate("pagingList");
			subMenu.setEnableExcel(true);
			subMenu.setEnableInsert(true);
			subMenu.setEnableUpdate(true);
			subMenu.setEnableDelete(true);
			subMenu.setDisplayOrder((short) 5);
			subMenuList.add(subMenu);
		}
		
		queryList = new ArrayList<>();
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Products");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO Products 
					(product, productName, operator, registerDate, modifyDate) 
					VALUES (:product, :productName, :operator, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE Products 
					SET product = :product, productName = :productName, operator = :operator, modifyDate = NOW() 
					WHERE product = :__org__product
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("DELETE FROM Products WHERE product = :product");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM MainMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			query.setSqlCommandType(QuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO MainMenus 
					(product, mainMenu, mainMenuName, operator, registerDate, modifyDate) 
					VALUES (:product, :mainMenu, :mainMenuName, :operator, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			query.setSqlCommandType(QuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE MainMenus 
					SET product = :product, mainMenu = :mainMenu, mainMenuName = :mainMenuName, operator = :operator, modifyDate = NOW() 
					WHERE product = :__org__product AND mainMenu = :__org__mainMenu
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM MainMenus 
					WHERE product = :product AND mainMenu = :mainMenu""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM SubMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			query.setSqlCommandType(QuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO SubMenus 
					(product, mainMenu, subMenu, subMenuName, template, displayOrder, groupNo, groupTemplate, pageSize, enableCount, enableExcel, enableInsert, enableUpdate, enableDelete, operator, registerDate, modifyDate) 
					VALUES (:product, :mainMenu, :subMenu, :subMenuName, :template, :displayOrder, :groupNo, :groupTemplate, :pageSize, :enableCount, :enableExcel, :enableInsert, :enableUpdate, :enableDelete, :operator, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			query.setSqlCommandType(QuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE SubMenus 
					SET product = :product, mainMenu = :mainMenu, subMenu = :subMenu, subMenuName = :subMenuName, template = :template, displayOrder = :displayOrder, groupNo = :groupNo, groupTemplate = :groupTemplate, pageSize = :pageSize, enableCount = :enableCount, enableExcel = :enableExcel, enableInsert = :enableInsert, enableUpdate = :enableUpdate, enableDelete = :enableDelete, operator = :operator, modifyDate = NOW() 
					WHERE product = :__org__product AND mainMenu = :__org__mainMenu AND subMenu = :__org__subMenu
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM SubMenus 
					WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Queries");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO Queries 
					(product, mainMenu, subMenu, sqlCommandType, queryString, dataSourceName, dbType, operator, registerDate, modifyDate) 
					VALUES (:product, :mainMenu, :subMenu, :sqlCommandType, :queryString, :dataSourceName, :dbType, :operator, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE Queries 
					SET product = :product, mainMenu = :mainMenu, subMenu = :subMenu, sqlCommandType = :sqlCommandType, queryString = :queryString, dataSourceName = :dataSourceName, dbType = :dbType, operator = :operator, modifyDate = NOW() 
					WHERE product = :__org__product AND mainMenu = :__org__mainMenu AND subMenu = :__org__subMenu AND sqlCommandType = :__org__sqlCommandType
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM SubMenus 
					WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Fields");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO Fields 
					(product, mainMenu, subMenu, `column`, `name`, `type`, preset, `format`, `validation`, `visible`, enableSearch, enableEdit, formSize, formOrder, operator, registerDate, modifyDate) 
					VALUES (:product, :mainMenu, :subMenu, :column, :name, :type, :preset, :format, :validation, :visible, :enableSearch, :enableEdit, :formSize, :formOrder, :operator, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE Fields 
					SET product = :product, mainMenu = :mainMenu, subMenu = :subMenu, `column` = :column, `name` = :name, `type` = :type, preset = :preset, `format` = :format, `validation` = :validation, `visible` = :visible, enableSearch = :enableSearch, enableEdit = :enableEdit, formSize = :formSize, formOrder = :formOrder, operator = :operator, modifyDate = NOW() 
					WHERE product = :__org__product AND mainMenu = :__org__mainMenu AND subMenu = :__org__subMenu AND `column` = :__org__column
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			query.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			query.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM Fields 
					WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu AND `column` = :column
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		fieldList = new ArrayList<>();
		
		addProjectField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
			field.setColumn("productName");
			field.setName("프로덕트 이름");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 2);
			fieldList.add(field);
		}
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU1_PRODUCT);
		
		addProjectField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
		addMainMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
			field.setColumn("mainMenuName");
			field.setName("메인메뉴 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 3);
			fieldList.add(field);
		}
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU2_MAINMENU);
		
		addProjectField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
		addMainMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
		addSubMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("subMenuName");
			field.setName("서브메뉴 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("displayOrder");
			field.setName("순서");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("enableExcel");
			field.setName("엑셀");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("enableInsert");
			field.setName("입력");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("enableUpdate");
			field.setName("수정");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 8);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
			field.setColumn("enableDelete");
			field.setName("삭제");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			fieldList.add(field);
		}
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU3_SUBMENU);
		
		addProjectField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU4_QUERY);
		addMainMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU4_QUERY);
		addSubMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU4_QUERY);
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			field.setColumn("sqlCommandType");
			field.setName("쿼리 타입");
			field.setType(FieldType.STRING);
			field.setPreset("INSERT|SELECT|UPDATE|DELETE");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			field.setColumn("dataSourceName");
			field.setName("데이터소스 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			field.setColumn("dbType");
			field.setName("DB 타입");
			field.setType(FieldType.STRING);
			field.setPreset("MsSql|MySql");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU4_QUERY);
			field.setColumn("queryString");
			field.setName("쿼리");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU4_QUERY);
		
		addProjectField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU5_FIELD);
		addMainMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU5_FIELD);
		addSubMenuField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU5_FIELD);
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("column");
			field.setName("컬럼");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("name");
			field.setName("컬럼명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("type");
			field.setName("컬럼타입");
			field.setType(FieldType.STRING);
			field.setPreset("BOOLEAN|DATE|INT|LINK|LONG|STRING|TEXT");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("preset");
			field.setName("프리셋");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("format");
			field.setName("포맷");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 8);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("validation");
			field.setName("검증");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("visible");
			field.setName("표시여부");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 10);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("enableSearch");
			field.setName("검색");
			field.setType(FieldType.STRING);
			field.setPreset("DISABLED|ENABLED|REQUIRED");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 11);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("enableEdit");
			field.setName("입력");
			field.setType(FieldType.STRING);
			field.setPreset("DISABLED|ENABLED|REQUIRED");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 12);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("formSize");
			field.setName("퐄크기");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 13);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_EVENT_ADMIN_PRODUCT);
			field.setMainMenu(KEY_EVENT_ADMIN_MAINMENU);
			field.setSubMenu(KEY_EVENT_ADMIN_SUBMENU5_FIELD);
			field.setColumn("formOrder");
			field.setName("폼순서");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 14);
			fieldList.add(field);
		}
		addDefaultField(KEY_EVENT_ADMIN_PRODUCT, KEY_EVENT_ADMIN_MAINMENU, KEY_EVENT_ADMIN_SUBMENU5_FIELD);
	}
	
	private void addProjectField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("product");
		field.setName("프로덕트 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 1);
		fieldList.add(field);
	}
	
	private void addMainMenuField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("mainMenu");
		field.setName("메인메뉴 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 2);
		fieldList.add(field);
	}
	
	private void addSubMenuField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("subMenu");
		field.setName("서브메뉴 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 3);
		fieldList.add(field);
	}
	
	private void addDefaultField(String product, String mainMenu, String subMenu) {
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("operator");
			field.setName("최종 수정");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 20);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("modifyDate");
			field.setName("수정일");
			field.setType(FieldType.DATE);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 21);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("registerDate");
			field.setName("등록일");
			field.setType(FieldType.DATE);
			field.setVisible(false);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 22);
			fieldList.add(field);
		}
	}
}
