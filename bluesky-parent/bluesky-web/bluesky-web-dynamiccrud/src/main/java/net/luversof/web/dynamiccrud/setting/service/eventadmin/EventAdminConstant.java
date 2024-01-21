package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;

public class EventAdminConstant {
	public static final String ADMIN_PROJECT_ID_VALUE = SettingConstant.ADMIN_PROJECT_ID_VALUE;
	public static final String PROJECT_ID_VALUE = "eventAdmin";
	public static final String MAINMENU_ID_VALUE = "menu";
	public static final String SUBMENU_ID_VALUE_PROJECT = "project";
	public static final String SUBMENU_ID_VALUE_MAINMENU = "mainMenu";
	public static final String SUBMENU_ID_VALUE_SUBMENU = "subMenu";
	public static final String SUBMENU_ID_VALUE_DBQUERY = "dbQuery";
	public static final String SUBMENU_ID_VALUE_DBFIELD = "dbField";
	public static final SubMenuDbType DB_TYPE = SubMenuDbType.MySql; 
	public static final String DATASOURCE_NAME = "dynamiccrud";
}
