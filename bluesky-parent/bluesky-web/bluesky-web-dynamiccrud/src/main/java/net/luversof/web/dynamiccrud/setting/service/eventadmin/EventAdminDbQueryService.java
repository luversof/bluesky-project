package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DATASOURCE_NAME;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBFIELD;

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
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					DbQuerySqlCommandType.SELECT,
					"SELECT * FROM Project",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					DbQuerySqlCommandType.INSERT,
					"""
					INSERT INTO Project 
					(adminProjectId, projectId, projectName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :projectName, :writer, NOW(), NOW())
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					DbQuerySqlCommandType.UPDATE,
					"""
					UPDATE Project 
					SET adminProjectId = :adminProjectId, projectId = :projectId, projectName = :projectName, writer= :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					DbQuerySqlCommandType.DELETE,
					"DELETE FROM Project WHERE adminProjectId = :adminProjectId AND projectId = :projectId",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_MAINMENU,
					DbQuerySqlCommandType.SELECT,
					"SELECT * FROM MainMenu",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_MAINMENU,
					DbQuerySqlCommandType.INSERT,
					"""
					INSERT INTO MainMenu 
					(adminProjectId, projectId, mainMenuId, mainMenuName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :mainMenuName, :writer, NOW(), NOW())
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_MAINMENU,
					DbQuerySqlCommandType.UPDATE,
					"""
					UPDATE MainMenu 
					SET adminProjectId = :adminProjectId,  projectId = :projectId, mainMenuId = :mainMenuId, mainMenuName = :mainMenuName, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_MAINMENU,
					DbQuerySqlCommandType.DELETE,
					"""
					DELETE FROM MainMenu
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_SUBMENU,
					DbQuerySqlCommandType.SELECT,
					"SELECT * FROM SubMenu",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_SUBMENU,
					DbQuerySqlCommandType.INSERT,
					"""
					INSERT INTO SubMenu 
					(adminProjectId, projectId, mainMenuId, subMenuId, subMenuName, dbType, displayOrder, pageSize, enableExcel, enableInsert, enableUpdate, enableDelete, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :subMenuName, :dbType, :displayOrder, :pageSize, :enableExcel, :enableInsert, :enableUpdate, :enableDelete, :writer, NOW(), NOW())
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_SUBMENU,
					DbQuerySqlCommandType.UPDATE,
					"""
					UPDATE SubMenu
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, subMenuName = :subMenuName, dbType = :dbType, displayOrder = :displayOrder, pageSize = :pageSize, enableExcel = :enableExcel, enableInsert = :enableInsert, enableUpdate = :enableUpdate, enableDelete = :enableDelete, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_SUBMENU,
					DbQuerySqlCommandType.DELETE,
					"""
					DELETE FROM SubMenu 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBQUERY,
					DbQuerySqlCommandType.SELECT,
					"SELECT * FROM DbQuery",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBQUERY,
					DbQuerySqlCommandType.INSERT,
					"""
					INSERT INTO DbQuery 
					(adminProjectId, projectId, mainMenuId, subMenuId, sqlCommandType, queryString, dataSourceName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :sqlCommandType, :queryString, :dataSourceName, :writer, NOW(), NOW())
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBQUERY,
					DbQuerySqlCommandType.UPDATE,
					"""
					UPDATE DbQuery
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, sqlCommandType = :sqlCommandType, queryString = :queryString, dataSourceName = :dataSourceName, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND sqlCommandType = :__org__sqlCommandType
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBQUERY,
					DbQuerySqlCommandType.DELETE,
					"""
					DELETE FROM DbQuery 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND sqlCommandType = :sqlCommandType
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBFIELD,
					DbQuerySqlCommandType.SELECT,
					"SELECT * FROM DbField",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBFIELD,
					DbQuerySqlCommandType.INSERT,
					"""
					INSERT INTO DbField 
					(adminProjectId, projectId, mainMenuId, subMenuId, columnId, columnName, columnType, columnPreset, columnFormat, columnValidation, columnVisible, enableSearch, enableEdit, formSize, formOrder, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :columnId, :columnName, :columnType, :columnPreset, :columnFormat, :columnValidation, :columnVisible, :enableSearch, :enableEdit, :formSize, :formOrder, :writer, NOW(), NOW())
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBFIELD,
					DbQuerySqlCommandType.UPDATE,
					"""
					UPDATE DbField 
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, columnId = :columnId, columnName = :columnName, columnType = :columnType, columnPreset = :columnPreset, columnFormat = :columnFormat, columnValidation = :columnValidation, columnVisible = :columnVisible, enableSearch = :enableSearch, enableEdit = :enableEdit, formSize = :formSize, formOrder = :formOrder, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND columnId = :__org__columnId
					""",
					DATASOURCE_NAME
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_DBFIELD,
					DbQuerySqlCommandType.DELETE,
					"""
					DELETE FROM DbField 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND columnId = :columnId
					""",
					DATASOURCE_NAME
			);
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
