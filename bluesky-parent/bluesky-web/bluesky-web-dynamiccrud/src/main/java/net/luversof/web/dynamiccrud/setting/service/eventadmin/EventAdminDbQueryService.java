package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DATASOURCE_NAME;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_ADMINPROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBFIELD;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_SUBMENU;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
					SUBMENU_ID_VALUE_ADMINPROJECT,
					DbQuerySqlCommandType.SELECT,
					DATASOURCE_NAME,
					"SELECT * FROM AdminProject"
			);
			dbQueryList.add(query);
		}
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_ADMINPROJECT,
					DbQuerySqlCommandType.INSERT,
					DATASOURCE_NAME,
					"""
					INSERT INTO AdminProject 
					(adminProjectId, adminProjectName, defaultGrantAuthority, roleHierarchy, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :adminProjectName, :defaultGrantAuthority, :roleHierarchy, :writer, NOW(), NOW())
					"""
			);
			dbQueryList.add(query);
		}
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_ADMINPROJECT,
					DbQuerySqlCommandType.UPDATE,
					DATASOURCE_NAME,
					"""
					UPDATE AdminProject 
					SET adminProjectId = :adminProjectId, adminProjectName = :adminProjectName,
					defaultGrantAuthority = :defaultGrantAuthority, roleHierarchy = :roleHierarchy, writer= :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId
					"""
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_ADMINPROJECT,
					DbQuerySqlCommandType.DELETE,
					DATASOURCE_NAME,
					"DELETE FROM AdminProject WHERE adminProjectId = :adminProjectId"
			);
			dbQueryList.add(query);
		}
		
		{
			var query = new DbQuery(
					ADMIN_PROJECT_ID_VALUE,
					PROJECT_ID_VALUE,
					MAINMENU_ID_VALUE,
					SUBMENU_ID_VALUE_PROJECT,
					DbQuerySqlCommandType.SELECT,
					DATASOURCE_NAME,
					"SELECT * FROM Project"
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
					DATASOURCE_NAME,
					"""
					INSERT INTO Project 
					(adminProjectId, projectId, projectName, enableMainMenuUI, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :projectName, :enableMainMenuUI, :writer, NOW(), NOW())
					"""
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
					DATASOURCE_NAME,
					"""
					UPDATE Project 
					SET adminProjectId = :adminProjectId, projectId = :projectId, projectName = :projectName, enableMainMenuUI = :enableMainMenuUI, writer= :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId
					"""
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
					DATASOURCE_NAME,
					"DELETE FROM Project WHERE adminProjectId = :adminProjectId AND projectId = :projectId"
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
					DATASOURCE_NAME,
					"SELECT * FROM MainMenu"
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
					DATASOURCE_NAME,
					"""
					INSERT INTO MainMenu 
					(adminProjectId, projectId, mainMenuId, mainMenuName, enableDisplay, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :mainMenuName, :enableDisplay, :writer, NOW(), NOW())
					"""
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
					DATASOURCE_NAME,
					"""
					UPDATE MainMenu 
					SET adminProjectId = :adminProjectId,  projectId = :projectId, mainMenuId = :mainMenuId, mainMenuName = :mainMenuName, enableDisplay = :enableDisplay, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId
					"""
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
					DATASOURCE_NAME,
					"""
					DELETE FROM MainMenu
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId
					"""
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
					DATASOURCE_NAME,
					"SELECT * FROM SubMenu"
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
					DATASOURCE_NAME,
					"""
					INSERT INTO SubMenu 
					(adminProjectId, projectId, mainMenuId, subMenuId, subMenuName, dbType, displayOrder, pageSize, enableExcel, enableInsert, enableUpdate, enableDelete, enableDisplay, authority, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :subMenuName, :dbType, :displayOrder, :pageSize, :enableExcel, :enableInsert, :enableUpdate, :enableDelete, :enableDisplay, :authority, :writer, NOW(), NOW())
					"""
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
					DATASOURCE_NAME,
					"""
					UPDATE SubMenu
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, 
					subMenuName = :subMenuName, dbType = :dbType, displayOrder = :displayOrder, pageSize = :pageSize, enableExcel = :enableExcel, 
					enableInsert = :enableInsert, enableUpdate = :enableUpdate, enableDelete = :enableDelete, enableDisplay = :enableDisplay, authority = :authority, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId
					"""
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
					DATASOURCE_NAME,
					"""
					DELETE FROM SubMenu 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId
					"""
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
					DATASOURCE_NAME,
					"SELECT * FROM DbQuery"
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
					DATASOURCE_NAME,
					"""
					INSERT INTO DbQuery 
					(adminProjectId, projectId, mainMenuId, subMenuId, sqlCommandType, queryString, dataSourceName, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :sqlCommandType, :queryString, :dataSourceName, :writer, NOW(), NOW())
					"""
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
					DATASOURCE_NAME,
					"""
					UPDATE DbQuery
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId,
					sqlCommandType = :sqlCommandType, queryString = :queryString, dataSourceName = :dataSourceName, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND sqlCommandType = :__org__sqlCommandType
					"""
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
					DATASOURCE_NAME,
					"""
					DELETE FROM DbQuery 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND sqlCommandType = :sqlCommandType
					"""
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
					DATASOURCE_NAME,
					"SELECT * FROM DbField"
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
					DATASOURCE_NAME,
					"""
					INSERT INTO DbField 
					(adminProjectId, projectId, mainMenuId, subMenuId, columnId, columnName, columnType, columnLength, columnOrder, columnPreset, columnFormat, columnValidation, columnVisible, enableSearch, enableInsert, enableUpdate, writer, createDate, updateDate) 
					VALUES (:adminProjectId, :projectId, :mainMenuId, :subMenuId, :columnId, :columnName, :columnType, :columnLength, :columnOrder, :columnPreset, :columnFormat, :columnValidation, :columnVisible, :enableSearch, :enableInsert, :enableUpdate, :writer, NOW(), NOW())
					"""
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
					DATASOURCE_NAME,
					"""
					UPDATE DbField 
					SET adminProjectId = :adminProjectId, projectId = :projectId, mainMenuId = :mainMenuId, subMenuId = :subMenuId, columnId = :columnId, columnName = :columnName, 
					columnType = :columnType, columnLength = :columnLength, columnOrder = :columnOrder, columnPreset = :columnPreset, columnFormat = :columnFormat, 
					columnValidation = :columnValidation, columnVisible = :columnVisible, enableSearch = :enableSearch, enableInsert = :enableInsert, enableUpdate = :enableUpdate, writer = :writer, updateDate = NOW() 
					WHERE adminProjectId = :__org__adminProjectId AND projectId = :__org__projectId AND mainMenuId = :__org__mainMenuId AND subMenuId = :__org__subMenuId AND columnId = :__org__columnId
					"""
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
					DATASOURCE_NAME,
					"""
					DELETE FROM DbField 
					WHERE adminProjectId = :adminProjectId AND projectId = :projectId AND mainMenuId = :mainMenuId AND subMenuId = :subMenuId AND columnId = :columnId
					"""
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
				.collect(Collectors.toList());
	}

}
