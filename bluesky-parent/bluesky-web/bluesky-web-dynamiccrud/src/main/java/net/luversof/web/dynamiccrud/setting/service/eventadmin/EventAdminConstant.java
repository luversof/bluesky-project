package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;

public class EventAdminConstant {
	public static final String ADMIN_PROJECT_ID_VALUE = AdminConstant.ADMIN_PROJECT_ID_VALUE;
	public static final String PROJECT_ID_VALUE = "eventAdmin";
	public static final String MAINMENU_ID_VALUE = "menu";
	public static final String SUBMENU_ID_VALUE_ADMINPROJECT = "adminProject";
	public static final String SUBMENU_ID_VALUE_PROJECT = "project";
	public static final String SUBMENU_ID_VALUE_MAINMENU = "mainMenu";
	public static final String SUBMENU_ID_VALUE_SUBMENU = "subMenu";
	public static final String SUBMENU_ID_VALUE_DBQUERY = "dbQuery";
	public static final String SUBMENU_ID_VALUE_DBFIELD = "dbField";
	public static final SubMenuDbType DB_TYPE = SubMenuDbType.MySql;
	
	public static final String DBQUERY_SQLCOMMANDTYPE_VALUE_PRESET = "INSERT,SELECT,UPDATE,DELETE";
	
	public static final String DBFIELD_COLUMNTYPE_PRESET_VALUE = "BOOLEAN,DATE,INT,LINK,LONG,STRING,TEXT,SPEL";
	public static final String DBFIELD_VISIBLE_PRESET_VALUE = "SHOW|노출,HIDE|노출안함,NOT_USED|사용안함";
	public static final String DBFIELD_ENABLE_PRESET_VALUE = "DISABLED|비활성화,ENABLED|활성화,REQUIRED|필수";
	public static final String DBFIELD_SEARCH_TYPE_PRESET_VALUE = "NOT_USED|사용안함,EQUALS|일치검색,LIKE_RIGHT|전방일치검색,LIKE_CONTAINS|일부일치검색,MINOR_THAN|<,GREATOR_THAN|>";
	
	public static final String DATASOURCE_NAME = "dynamiccrud";
}
