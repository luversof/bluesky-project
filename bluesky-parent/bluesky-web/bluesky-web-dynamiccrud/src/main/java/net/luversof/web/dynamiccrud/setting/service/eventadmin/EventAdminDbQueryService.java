package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DATASOURCE_NAME;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DB_TYPE;
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
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminDbQueryService implements SettingServiceListSupplier<DbQuery> {
	
	@Getter private List<DbQuery> dbQueryList;
	
	public EventAdminDbQueryService() {
		loadData();
	}
	
	private void loadData() {
		dbQueryList = new ArrayList<>();
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU1_PROJECT);
			query.setSqlCommandType(DbQuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM Project");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU1_PROJECT);
			query.setSqlCommandType(DbQuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO Project 
					(adminProjectId, projectId, projectName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :projectName, :writer, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU1_PROJECT);
			query.setSqlCommandType(DbQuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE Project 
					SET adminProjectId = :adminProjectId, projectId = :projectId, projectName = :projectName, writer= :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU1_PROJECT);
			query.setSqlCommandType(DbQuerySqlCommandType.DELETE);
			query.setQueryString("DELETE FROM Project WHERE adminProjectId = :adminProjectId AND projectId = :projectId");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU2_MAINMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM MainMenu");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU2_MAINMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO MainMenu 
					(adminProjectId, projectId, mainMenuId, mainMenuName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :mainMenuName, :writer, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU2_MAINMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE MainMenu 
					SET adminProjectId = :adminProjectId,  projectId = :projectId, mainMenuId = :mainMenuId, mainMenuName = :mainMenuName, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU2_MAINMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM MainMenu
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM SubMenu");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO SubMenu 
					(adminProjectId, projectId, mainMenuId, subMenuId, subMenuName, template, displayOrder, groupNo, groupTemplate, pageSize, enableCount, enableExcel, enableInsert, enableUpdate, enableDelete, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :subMenuName, :template, :displayOrder, :groupNo, :groupTemplate, :pageSize, :enableCount, :enableExcel, :enableInsert, :enableUpdate, :enableDelete, :writer, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE SubMenu
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, subMenuName = :subMenuName, template = :template, displayOrder = :displayOrder, groupNo = :groupNo, groupTemplate = :groupTemplate, pageSize = :pageSize, enableCount = :enableCount, enableExcel = :enableExcel, enableInsert = :enableInsert, enableUpdate = :enableUpdate, enableDelete = :enableDelete, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			query.setSqlCommandType(DbQuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM SubMenu 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			query.setSqlCommandType(DbQuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM DbQuery");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			query.setSqlCommandType(DbQuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO DbQuery 
					(adminProjectId, projectId, mainMenuId, subMenuId, sqlCommandType, queryString, dataSourceName, dbType, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :sqlCommandType, :queryString, :dataSourceName, :dbType, :writer, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			query.setSqlCommandType(DbQuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE DbQuery
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, sqlCommandType = :sqlCommandType, queryString = :queryString, dataSourceName = :dataSourceName, dbType = :dbType, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND sqlCommandType = :__org__sqlCommandType
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			query.setSqlCommandType(DbQuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM DbQuery 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND sqlCommandType = :sqlCommandType
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			query.setSqlCommandType(DbQuerySqlCommandType.SELECT);
			query.setQueryString("SELECT * FROM DbField");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			query.setSqlCommandType(DbQuerySqlCommandType.INSERT);
			query.setQueryString("""
					INSERT INTO DbField 
					(adminProjectId, projectId, mainMenuId, subMenuId, columnId, columnName, columnType, columnPreset, columnFormat, columnValidation, columnVisible, enableSearch, enableEdit, formSize, formOrder, writer, createDate, updasteDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :columnId, :columnName, :columnType, :columnPreset, :columnFormat, :columnValidation, :columnVisible, :enableSearch, :enableEdit, :formSize, :formOrder, :writer, NOW(), NOW())
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			query.setSqlCommandType(DbQuerySqlCommandType.UPDATE);
			query.setQueryString("""
					UPDATE DbField 
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, columnId = :columnId, columnName = :columnName, columnType = :columnType, columnPreset = :columnPreset, columnFormat = :columnFormat, columnValidation = :columnValidation, columnVisible = :columnVisible, enableSearch = :enableSearch, enableEdit = :enableEdit, formSize = :formSize, formOrder = :formOrder, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND columnId = :__org__columnId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery();
			query.setAdminProjectId(KEY_ADMIN_PROJECT);
			query.setProjectId(KEY_PROJECT);
			query.setMainMenuId(KEY_MAINMENU);
			query.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			query.setSqlCommandType(DbQuerySqlCommandType.DELETE);
			query.setQueryString("""
					DELETE FROM DbField 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND columnId = :columnId
					""");
			query.setDataSourceName(DATASOURCE_NAME);
			query.setDbType(DB_TYPE);
			dbQueryList.add(query);
		}
	}

	@Override
	public List<DbQuery> findList(SettingParameter settingParameter) {
		return dbQueryList.stream()
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId())
						&& x.getMainMenuId().equals(settingParameter.mainMenuId())
						&& x.getSubMenuId().equals(settingParameter.subMenuId()))
				.toList();
	}

}
