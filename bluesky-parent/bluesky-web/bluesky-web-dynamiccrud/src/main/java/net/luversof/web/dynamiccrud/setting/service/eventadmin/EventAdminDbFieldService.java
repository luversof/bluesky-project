package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.ADMIN_PROJECT_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_COLUMNTYPE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_ENABLE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBFIELD_VISIBLE_PRESET_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.DBQUERY_SQLCOMMANDTYPE_VALUE_PRESET;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.MAINMENU_ID_VALUE;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.PROJECT_ID_VALUE;
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
				"MsSql|MySql",
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
				"link",
				"링크",
				DbFieldColumnType.SPEL,
				Short.MAX_VALUE,
				(short) 18,
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
				"MsSql의 경우 SELECT query 등록 시 order by 절이 필수 항목입니다.",
				null
			);
			dbFieldList.add(field);
		}
		addDefaultField(ADMIN_PROJECT_ID_VALUE, PROJECT_ID_VALUE, MAINMENU_ID_VALUE, SUBMENU_ID_VALUE_DBQUERY);
		
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
	
	private void addProjectField(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
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
				"종속될 adminProject를 지정합니다.",
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