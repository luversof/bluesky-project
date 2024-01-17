package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;

public class EventAdminConstant {
	public static final String KEY_ADMIN_PROJECT = SettingConstant.KEY_ADMIN_PROJECT;
	public static final String KEY_PROJECT = "eventAdmin";
	public static final String KEY_MAINMENU = "menu";
	public static final String KEY_SUBMENU1_PROJECT = "project";
	public static final String KEY_SUBMENU2_MAINMENU = "mainMenu";
	public static final String KEY_SUBMENU3_SUBMENU = "subMenu";
	public static final String KEY_SUBMENU4_DBQUERY = "dbQuery";
	public static final String KEY_SUBMENU5_DBFIELD = "dbField";
	public static final SubMenuDbType DB_TYPE = SubMenuDbType.MySql; 
	public static final String DATASOURCE_NAME = "dynamiccrud";
}
