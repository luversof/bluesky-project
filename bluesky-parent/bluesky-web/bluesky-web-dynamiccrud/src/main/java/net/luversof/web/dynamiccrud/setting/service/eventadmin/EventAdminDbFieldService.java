package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_COLUMNTYPE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_ENABLE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_VISIBLE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBQUERY_SQLCOMMANDTYPE_VALUE_PRESET;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_ADMINPROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBFIELD;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_DBQUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_PROJECT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.SUBMENU_ID_VALUE_SUBMENU;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldVisible;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminDbFieldService implements SettingServiceListSupplier<DbField> {
	
	@Getter private List<DbField> dbFieldList;
	
	public EventAdminDbFieldService() {
		loadData();
	}
	
	private void loadData() {
		dbFieldList = new ArrayList<>();
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_ADMINPROJECT);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_ADMINPROJECT,
				"adminProjectName",
				"관리 프로젝트 이름",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 10,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_ADMINPROJECT,
				"defaultGrantAuthority",
				"기본 부여 권한",
				DbFieldColumnType.STRING,
				(short) 30,
				(short) 13,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"유저 권한 조회 시 기본 설정 되는 권한 정보입니다. 미 설정 시 DMT 기본 설정을 사용합니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_ADMINPROJECT,
				"roleHierarchy",
				"권한 계층 구조",
				DbFieldColumnType.STRING,
				(short) 1000,
				(short) 14,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"유저 권한 조회 시 설정 되는 권한 계층 구조 정보입니다. 미 설정 시 DMT 기본 설정을 사용합니다.",
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_ADMINPROJECT);
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_PROJECT);
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_PROJECT);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_PROJECT,
				"projectName",
				"프로젝트 이름",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 10,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_PROJECT);
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_MAINMENU,
				"mainMenuName",
				"메인메뉴 명",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 11,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_MAINMENU);
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"subMenuName",
				"서브메뉴 명",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 10,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"dbType",
				"DB 타입",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 11,
				SubMenuDbType.toPresetString(),
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"displayOrder",
				"순서",
				DbFieldColumnType.INT,
				(short) 20,
				(short) 12,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"화면 상단에 위치한 메뉴 목록에 보이는 순서를 지정합니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"pageSize",
				"페이지 크기",
				DbFieldColumnType.INT,
				(short) 20,
				(short) 13,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"enableExcel",
				"엑셀",
				DbFieldColumnType.BOOLEAN,
				(short) 20,
				(short) 14,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"enableInsert",
				"입력",
				DbFieldColumnType.BOOLEAN,
				(short) 20,
				(short) 15,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"enableUpdate",
				"수정",
				DbFieldColumnType.BOOLEAN,
				(short) 20,
				(short) 16,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"enableDelete",
				"삭제",
				DbFieldColumnType.BOOLEAN,
				(short) 20,
				(short) 17,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"authority",
				"접근 권한",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 18,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"해당 메뉴의 접근 권한을 설정합니다. 미 설정 시 기본 설정을 사용합니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_SUBMENU,
				"link",
				"링크",
				DbFieldColumnType.SPEL,
				Short.MAX_VALUE,
				(short) 19,
				null,
				"'<a href=\"/' + #adminProjectId + '/use/' + #projectId + '/' + #mainMenuId + '/' + #subMenuId + '\" target=\"_blank\">link</a>'",
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_SUBMENU);
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBQUERY,
				"sqlCommandType",
				"쿼리 타입",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 10,
				DBQUERY_SQLCOMMANDTYPE_VALUE_PRESET,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"SubMenu 1개에는 CRUD 쿼리타입 당 1개씩 총 4개까지 등록이 가능합니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBQUERY,
				"dataSourceName",
				"데이터소스 이름",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 11,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"""
				ConnectionInfo에 등록되어 있는 dataSource Name을 설정하면 미리 DataSource bean을 설정하지 않아도 후처리로 DataSource를 생성하고 등록합니다.
				다만 잘못설정된 연결 정보를 설정한 경우 connectionInfo lazy load를 불필요하게 계속 호출하는 것을 방지하기 위해 현재 호출 실패시 1시간 동안 재호출을 막아두었습니다.
				""",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBQUERY,
				"queryString",
				"쿼리",
				DbFieldColumnType.TEXT,
				Short.MAX_VALUE,
				(short) 12,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"""
				MsSql의 경우 SELECT query 등록 시 order by 절이 필수 항목입니다. (MySql은 선택 항목)
				예) SELECT * FROM SomeTable Order By idx DESC
				SELECT 쿼리는 동적으로 페이징/검색 쿼리를 구성합니다. 
				등록 시 Where 조건을 설정한 경우 동적 쿼리를 구성하지 않으며 연관한 기능들이 제대로 동작하지 않습니다.
				""",
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		
		addAdminProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		addProjectField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		addMainMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		addSubMenuField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnId",
				"컬럼 Id",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 10,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnName",
				"컬럼 이름",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 11,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnType",
				"컬럼 타입",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 12,
				DBFIELD_COLUMNTYPE_PRESET_VALUE,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"SPEL 타입의 경우 DB에 저장되는 컬럼이 아닌 목록 화면에 추가가 필요할 때 사용됩니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnLength",
				"컬럼 길이",
				DbFieldColumnType.INT,
				(short) 40,
				(short) 13,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnOrder",
				"컬럼 순서",
				DbFieldColumnType.INT,
				(short) 40,
				(short) 14,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"입력 form 목록에 보이는 form 순서 및 조회 목록 화면의 컬럼명 순서를 지정합니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnPreset",
				"프리셋",
				DbFieldColumnType.STRING,
				(short) 1000,
				(short) 15,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"""
				정해진 값 중 선택을 하는 컬럼은 preset을 지정하면 됩니다. ',' 로 각 값을 구분하면 됩니다.
				예)  Avalue,Bvalue,Cvalue
				만약 해당 값에 대해 화면의 표시 처리는 다르게 하려는 경우 '|'로 각 값의 표시값을 추가할 수 있습니다.
				예) Avalue|A값,Bvalue|B값,Cvalue|C값
				""",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnFormat",
				"포맷",
				DbFieldColumnType.STRING,
				(short) 1000,
				(short) 16,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"현재 컬럼타입이 SPEL인 경우에만 사용하도록 구현된 상태입니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnValidation",
				"검증",
				DbFieldColumnType.STRING,
				(short) 1000,
				(short) 17,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"현재 미구현 상태",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"columnVisible",
				"표시여부",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 18,
				DBFIELD_VISIBLE_PRESET_VALUE,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				"""
				표시여부는 해당 컬럼을 화면에 노출할지 여부에 대한 설정입니다.
				대부분 노출/비노출 설정을 선택하면 되며 해당 컬럼의 데이터를 사용하지 않아야 하는 경우 사용안함을 선택하면 됩니다.  
				""",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"enableSearch",
				"검색",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 19,
				DBFIELD_ENABLE_PRESET_VALUE,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"enableInsert",
				"입력",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 20,
				DBFIELD_ENABLE_PRESET_VALUE,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"enableUpdate",
				"수정",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 21,
				DBFIELD_ENABLE_PRESET_VALUE,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"formHelpText",
				"폼 도움말",
				DbFieldColumnType.STRING,
				(short) 1000,
				(short) 22,
				null,
				null,
				null,
				DbFieldVisible.HIDE,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				"컬럼을 입력하는데 필요한 도움말을 tooltip 형태로 제공합니다. 지금 보고 있는 부분이 tooltip 입니다.",
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				ADMIN_PROJECT_ID_VALUE,
				PROJECT_ID_VALUE,
				MAINMENU_ID_VALUE,
				SUBMENU_ID_VALUE_DBFIELD,
				"formPlaceholder",
				"폼 Placeholder",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 23,
				null,
				null,
				null,
				DbFieldVisible.HIDE,
				DbFieldEnable.DISABLED,
				DbFieldEnable.ENABLED,
				DbFieldEnable.ENABLED,
				null,
				"40자 이내 간략한 설명을 제공합니다"
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBFIELD);
	}
	
	private void addAdminProjectField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField(
					adminProjectId,
					projectId,
					mainMenuId,
					subMenuId,
					"adminProjectId",
					"어드민 프로젝트 Id",
					DbFieldColumnType.STRING,
					(short) 20,
					(short) 1,
					PROJECT_ID_VALUE,
					null,
					null,
					DbFieldVisible.SHOW,
					DbFieldEnable.DISABLED,
					DbFieldEnable.REQUIRED,
					DbFieldEnable.REQUIRED,
					"종속된 관리툴을 지정합니다. 현재 주소를 기반으로 기본 설정되므로 별도의 설정이 필요없습니다.",
					null
					);
			dbFieldList.add(field);
		}
	}
	
	private void addProjectField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"projectId",
				"프로젝트 Id",
				DbFieldColumnType.STRING,
				(short) 20,
				(short) 2,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				"대상 프로젝트 Id"
			);
			dbFieldList.add(field);
		}
	}
	
	private void addMainMenuField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"mainMenuId",
				"메인메뉴 Id",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 3,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				"대상 메인메뉴 Id"
			);
			dbFieldList.add(field);
		}
	}
	
	private void addSubMenuField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"subMenuId",
				"서브메뉴 Id",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 4,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.ENABLED,
				DbFieldEnable.REQUIRED,
				DbFieldEnable.REQUIRED,
				null,
				"대상 서브메뉴 Id"
			);
			dbFieldList.add(field);
		}
	}
	
	private void addDefaultField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"idx",
				"#",
				DbFieldColumnType.LONG,
				null,
				Short.MAX_VALUE,
				null,
				null,
				null,
				DbFieldVisible.HIDE,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"writer",
				"최종 수정자",
				DbFieldColumnType.STRING,
				(short) 40,
				(short) 40,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"createDate",
				"생성일",
				DbFieldColumnType.DATE,
				(short) 40,
				(short) 41,
				null,
				null,
				null,
				DbFieldVisible.HIDE,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				null,
				null
			);
			dbFieldList.add(field);
		}
		
		{
			var field = new DbField(
				adminProjectId,
				projectId,
				mainMenuId,
				subMenuId,
				"updateDate",
				"수정일",
				DbFieldColumnType.DATE,
				(short) 40,
				(short) 42,
				null,
				null,
				null,
				DbFieldVisible.SHOW,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				DbFieldEnable.DISABLED,
				null,
				null
			);
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
				.sorted(Comparator.comparing(DbField::getColumnOrder))
				.collect(Collectors.toList());
	}

}