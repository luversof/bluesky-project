package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DATASOURCE_NAME;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DB_TYPE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU1_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU2_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU3_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU4_QUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU5_FIELD;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminQueryService implements SettingServiceListSupplier<Query> {
	
	@Getter private List<Query> queryList;
	
	public EventAdminQueryService() {
		loadData();
	}
	
	private void loadData() {
		queryList = new ArrayList<>();
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Products");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU1_PRODUCT);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU1_PRODUCT);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU1_PRODUCT);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("DELETE FROM Products WHERE product = :product");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU2_MAINMENU);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM MainMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU2_MAINMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU2_MAINMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU2_MAINMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU3_SUBMENU);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM SubMenus");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU3_SUBMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU3_SUBMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU3_SUBMENU);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Queries");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU4_QUERY);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU4_QUERY);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU4_QUERY);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM Queries 
					WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu AND sqlCommandType = :sqlCommandType
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Fields");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
		
		{
			var query = new Query();
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU5_FIELD);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU5_FIELD);
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
			query.setProduct(KEY_PRODUCT);
			query.setMainMenu(KEY_MAINMENU);
			query.setSubMenu(KEY_SUBMENU5_FIELD);
			query.setSqlCommandType(QuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM Fields 
					WHERE product = :product AND mainMenu = :mainMenu AND subMenu = :subMenu AND `column` = :column
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			queryList.add(query);
		}
	}

	@Override
	public List<Query> findList(SettingParameter settingParameter) {
		return queryList.stream()
				.filter(x -> x.getProduct().equals(settingParameter.product())
						&& x.getMainMenu().equals(settingParameter.mainMenu())
						&& x.getSubMenu().equals(settingParameter.subMenu()))
				.toList();
	}

}
