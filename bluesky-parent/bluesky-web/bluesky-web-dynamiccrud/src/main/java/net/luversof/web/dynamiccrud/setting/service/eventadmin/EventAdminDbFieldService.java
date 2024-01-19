package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_ADMIN_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU1_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU2_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU3_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU4_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU5_DBFIELD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminDbFieldService implements SettingServiceListSupplier<DbField> {
	
	@Getter private List<DbField> dbFieldList;
	
	public EventAdminDbFieldService() {
		loadData();
	}
	
	private void loadData() {
		dbFieldList = new ArrayList<>();
		
		addProjectField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU1_PROJECT);
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU1_PROJECT);
			field.setColumnId("projectName");
			field.setColumnName("프로젝트 이름");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 2);
			dbFieldList.add(field);
		}
		addDefaultField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU1_PROJECT);
		
		addProjectField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		addMainMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU2_MAINMENU);
			field.setColumnId("mainMenuName");
			field.setColumnName("메인메뉴 명");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 3);
			dbFieldList.add(field);
		}
		addDefaultField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		
		addProjectField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		addMainMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		addSubMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("subMenuName");
			field.setColumnName("서브메뉴 명");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 4);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("dbType");
			field.setColumnName("DB 타입");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("MsSql|MySql");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 5);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("displayOrder");
			field.setColumnName("순서");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 6);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("pageSize");
			field.setColumnName("페이지크기");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("enableExcel");
			field.setColumnName("엑셀");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 11);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("enableInsert");
			field.setColumnName("입력");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 12);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("enableUpdate");
			field.setColumnName("수정");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 13);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("enableDelete");
			field.setColumnName("삭제");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 14);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU3_SUBMENU);
			field.setColumnId("link");
			field.setColumnName("링크");
			field.setColumnType(DbFieldColumnType.SPEL);
			field.setColumnFormat("'<a href=\"/' + #adminProjectId + '/use/' + #projectId + '/' + #mainMenuId + '/' + #subMenuId + '\" target=\"_blank\">link</a>'");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.DISABLED);
			field.setFormOrder((short) 15);
			dbFieldList.add(field);
		}
		addDefaultField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		
		addProjectField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU4_DBQUERY);
		addMainMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU4_DBQUERY);
		addSubMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU4_DBQUERY);
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			field.setColumnId("sqlCommandType");
			field.setColumnName("쿼리 타입");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("INSERT|SELECT|UPDATE|DELETE");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 4);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			field.setColumnId("dataSourceName");
			field.setColumnName("데이터소스 명");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 5);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU4_DBQUERY);
			field.setColumnId("queryString");
			field.setColumnName("쿼리");
			field.setColumnType(DbFieldColumnType.TEXT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 7);
			dbFieldList.add(field);
		}
		addDefaultField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU4_DBQUERY);
		
		addProjectField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU5_DBFIELD);
		addMainMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU5_DBFIELD);
		addSubMenuField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU5_DBFIELD);
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnId");
			field.setColumnName("컬럼");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 4);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnName");
			field.setColumnName("컬럼명");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnType");
			field.setColumnName("컬럼타입");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("BOOLEAN|DATE|INT|LINK|LONG|STRING|TEXT|SPEL");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 6);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnPreset");
			field.setColumnName("프리셋");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnFormat");
			field.setColumnName("포맷");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 8);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnValidation");
			field.setColumnName("검증");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("columnVisible");
			field.setColumnName("표시여부");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 10);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("enableSearch");
			field.setColumnName("검색");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("DISABLED|ENABLED|REQUIRED");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 11);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("enableEdit");
			field.setColumnName("입력");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("DISABLED|ENABLED|REQUIRED");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 12);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("formSize");
			field.setColumnName("폼크기");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 13);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(KEY_PROJECT);
			field.setMainMenuId(KEY_MAINMENU);
			field.setSubMenuId(KEY_SUBMENU5_DBFIELD);
			field.setColumnId("formOrder");
			field.setColumnName("폼순서");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 14);
			dbFieldList.add(field);
		}
		addDefaultField(KEY_ADMIN_PROJECT, KEY_PROJECT, KEY_MAINMENU, KEY_SUBMENU5_DBFIELD);
	}
	
	private void addProjectField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField();
			field.setAdminProjectId(adminProjectId);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("adminProjectId");
			field.setColumnName("어드민 프로젝트 ID");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset(KEY_PROJECT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 1);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(adminProjectId);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("projectId");
			field.setColumnName("프로젝트 ID");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 2);
			dbFieldList.add(field);
		}
	}
	
	private void addMainMenuField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		var field = new DbField();
		field.setAdminProjectId(adminProjectId);
		field.setProjectId(projectId);
		field.setMainMenuId(mainMenuId);
		field.setSubMenuId(subMenuId);
		field.setColumnId("mainMenuId");
		field.setColumnName("메인메뉴 ID");
		field.setColumnType(DbFieldColumnType.STRING);
		field.setColumnVisible(true);
		field.setEnableSearch(DbFieldEnable.ENABLED);
		field.setEnableEdit(DbFieldEnable.REQUIRED);
		field.setFormOrder((short) 3);
		dbFieldList.add(field);
	}
	
	private void addSubMenuField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		var field = new DbField();
		field.setAdminProjectId(adminProjectId);
		field.setProjectId(projectId);
		field.setMainMenuId(mainMenuId);
		field.setSubMenuId(subMenuId);
		field.setColumnId("subMenuId");
		field.setColumnName("서브메뉴 ID");
		field.setColumnType(DbFieldColumnType.STRING);
		field.setColumnVisible(true);
		field.setEnableSearch(DbFieldEnable.ENABLED);
		field.setEnableEdit(DbFieldEnable.REQUIRED);
		field.setFormOrder((short) 4);
		dbFieldList.add(field);
	}
	
	private void addDefaultField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField();
			field.setAdminProjectId(adminProjectId);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("writer");
			field.setColumnName("최종 수정자");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.DISABLED);
			field.setFormOrder((short) 20);
			dbFieldList.add(field);
		}
		
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("createDate");
			field.setColumnName("생성일");
			field.setColumnType(DbFieldColumnType.DATE);
			field.setColumnVisible(false);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.DISABLED);
			field.setFormOrder((short) 21);
			dbFieldList.add(field);
		}
		
		{
			var field = new DbField();
			field.setAdminProjectId(KEY_ADMIN_PROJECT);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("updateDate");
			field.setColumnName("수정일");
			field.setColumnType(DbFieldColumnType.DATE);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.DISABLED);
			field.setFormOrder((short) 22);
			dbFieldList.add(field);
		}
		
	}

	@Override
	public List<DbField> findList(SettingParameter settingParameter) {
		return dbFieldList.stream()
				.filter(x -> x.getAdminProjectId().equals(settingParameter.adminProjectId())
						&& x.getProjectId().equals(settingParameter.projectId())
						&& x.getMainMenuId().equals(settingParameter.mainMenuId())
						&& x.getSubMenuId().equals(settingParameter.subMenuId()))
				.sorted(Comparator.comparing(DbField::getFormOrder))
				.toList();
	}

}