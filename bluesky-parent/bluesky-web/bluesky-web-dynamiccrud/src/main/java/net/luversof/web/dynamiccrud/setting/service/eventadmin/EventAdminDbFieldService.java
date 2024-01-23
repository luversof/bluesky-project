package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBFIELD;

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
		
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_PROJECT);
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_PROJECT);
			field.setColumnId("projectName");
			field.setColumnName("프로젝트 이름");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 2);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_PROJECT);
		
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_MAINMENU);
			field.setColumnId("mainMenuName");
			field.setColumnName("메인메뉴 명");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.ENABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 3);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_SUBMENU);
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
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBQUERY);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBQUERY);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBQUERY);
			field.setColumnId("queryString");
			field.setColumnName("쿼리");
			field.setColumnType(DbFieldColumnType.TEXT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 7);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("columnHelpText");
			field.setColumnName("컬럼 도움말");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnHelpText("컬럼을 입력하는데 필요한 도움말을 tooltip 형태로 제공합니다. 지금 보고 있는 부분이 tooltip 입니다.");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 10);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("columnPlaceholder");
			field.setColumnName("컬럼 Placeholder");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPlaceholder("20자 이내 간략한 설명을 제공합니다");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 11);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("columnVisible");
			field.setColumnName("표시여부");
			field.setColumnType(DbFieldColumnType.BOOLEAN);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 12);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("enableSearch");
			field.setColumnName("검색");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("DISABLED|ENABLED|REQUIRED");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 13);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("enableEdit");
			field.setColumnName("입력");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset("DISABLED|ENABLED|REQUIRED");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 14);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("formSize");
			field.setColumnName("form 크기");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.ENABLED);
			field.setFormOrder((short) 15);
			dbFieldList.add(field);
		}
		{
			var field = new DbField();
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
			field.setProjectId(PROJECT_ID_VALUE);
			field.setMainMenuId(MAINMENU_ID_VALUE);
			field.setSubMenuId(SUBMENU_ID_VALUE_DBFIELD);
			field.setColumnId("formOrder");
			field.setColumnName("form 순서");
			field.setColumnType(DbFieldColumnType.INT);
			field.setColumnHelpText("입력 form 목록에 보이는 form 순서 뿐만 아니라 조회 목록 화면의 컬럼명 순서도 같이 지정합니다.");
			field.setColumnVisible(true);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.REQUIRED);
			field.setFormOrder((short) 16);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
	}
	
	private void addProjectField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField();
			field.setAdminProjectId(adminProjectId);
			field.setProjectId(projectId);
			field.setMainMenuId(mainMenuId);
			field.setSubMenuId(subMenuId);
			field.setColumnId("adminProjectId");
			field.setColumnName("어드민 프로젝트 Id");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPreset(PROJECT_ID_VALUE);
			field.setColumnHelpText("종속될 adminProject를 지정합니다.");
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
			field.setColumnName("프로젝트 Id");
			field.setColumnType(DbFieldColumnType.STRING);
			field.setColumnPlaceholder("대상 프로젝트 Id");
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
		field.setColumnName("메인메뉴 Id");
		field.setColumnType(DbFieldColumnType.STRING);
		field.setColumnPlaceholder("대상 메인메뉴 Id");
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
		field.setColumnName("서브메뉴 Id");
		field.setColumnType(DbFieldColumnType.STRING);
		field.setColumnPlaceholder("대상 서브메뉴 Id");
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
			field.setColumnId("idx");
			field.setColumnName("#");
			field.setColumnType(DbFieldColumnType.LONG);
			field.setColumnVisible(false);
			field.setEnableSearch(DbFieldEnable.DISABLED);
			field.setEnableEdit(DbFieldEnable.DISABLED);
			field.setFormOrder(Short.MAX_VALUE);
			dbFieldList.add(field);
		}
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
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
			field.setAdminProjectId(ADMIN_PROJECT_ID_VALUE);
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