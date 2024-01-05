package net.luversof.web.dynamiccrud.setting.service.eventadmin;

import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU1_PRODUCT;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU2_MAINMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU3_SUBMENU;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU4_QUERY;
import static net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant.KEY_SUBMENU5_FIELD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.FieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.FieldType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceListSupplier;

@Service
public class EventAdminFieldService implements SettingServiceListSupplier<Field> {
	
	@Getter private List<Field> fieldList;
	
	public EventAdminFieldService() {
		loadData();
	}
	
	private void loadData() {
		fieldList = new ArrayList<>();
		
		addProjectField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU1_PRODUCT);
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU1_PRODUCT);
			field.setColumn("productName");
			field.setName("프로덕트 이름");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 2);
			fieldList.add(field);
		}
		addDefaultField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU1_PRODUCT);
		
		addProjectField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		addMainMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU2_MAINMENU);
			field.setColumn("mainMenuName");
			field.setName("메인메뉴 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 3);
			fieldList.add(field);
		}
		addDefaultField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU2_MAINMENU);
		
		addProjectField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		addMainMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		addSubMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("subMenuName");
			field.setName("서브메뉴 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("template");
			field.setName("템플릿");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("displayOrder");
			field.setName("순서");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("groupNo");
			field.setName("그룹번호");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("groupTemplate");
			field.setName("그룹템플릿");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 8);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("pageSize");
			field.setName("페이지크기");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("enableCount");
			field.setName("카운팅");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 10);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("enableExcel");
			field.setName("엑셀");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 11);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("enableInsert");
			field.setName("입력");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 12);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("enableUpdate");
			field.setName("수정");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 13);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("enableDelete");
			field.setName("삭제");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 14);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU3_SUBMENU);
			field.setColumn("link");
			field.setName("링크");
			field.setType(FieldType.SPEL);
			field.setFormat("'<a href=\"/use/' + #product + '/' + #mainMenu + '/' + #subMenu + '\" target=\"_blank\">link</a>'");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 15);
			fieldList.add(field);
		}
		addDefaultField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU3_SUBMENU);
		
		addProjectField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU4_QUERY);
		addMainMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU4_QUERY);
		addSubMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU4_QUERY);
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU4_QUERY);
			field.setColumn("sqlCommandType");
			field.setName("쿼리 타입");
			field.setType(FieldType.STRING);
			field.setPreset("INSERT|SELECT|UPDATE|DELETE");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU4_QUERY);
			field.setColumn("dataSourceName");
			field.setName("데이터소스 명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU4_QUERY);
			field.setColumn("dbType");
			field.setName("DB 타입");
			field.setType(FieldType.STRING);
			field.setPreset("MsSql|MySql");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU4_QUERY);
			field.setColumn("queryString");
			field.setName("쿼리");
			field.setType(FieldType.TEXT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		addDefaultField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU4_QUERY);
		
		addProjectField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU5_FIELD);
		addMainMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU5_FIELD);
		addSubMenuField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU5_FIELD);
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("column");
			field.setName("컬럼");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 4);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("name");
			field.setName("컬럼명");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 5);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("type");
			field.setName("컬럼타입");
			field.setType(FieldType.STRING);
			field.setPreset("BOOLEAN|DATE|INT|LINK|LONG|STRING|TEXT|SPEL");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.ENABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 6);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("preset");
			field.setName("프리셋");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 7);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("format");
			field.setName("포맷");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 8);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("validation");
			field.setName("검증");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 9);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("visible");
			field.setName("표시여부");
			field.setType(FieldType.BOOLEAN);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 10);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("enableSearch");
			field.setName("검색");
			field.setType(FieldType.STRING);
			field.setPreset("DISABLED|ENABLED|REQUIRED");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 11);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("enableEdit");
			field.setName("입력");
			field.setType(FieldType.STRING);
			field.setPreset("DISABLED|ENABLED|REQUIRED");
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 12);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("formSize");
			field.setName("폼크기");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.ENABLED);
			field.setFormOrder((short) 13);
			fieldList.add(field);
		}
		{
			var field = new Field();
			field.setProduct(KEY_PRODUCT);
			field.setMainMenu(KEY_MAINMENU);
			field.setSubMenu(KEY_SUBMENU5_FIELD);
			field.setColumn("formOrder");
			field.setName("폼순서");
			field.setType(FieldType.INT);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.REQUIRED);
			field.setFormOrder((short) 14);
			fieldList.add(field);
		}
		addDefaultField(KEY_PRODUCT, KEY_MAINMENU, KEY_SUBMENU5_FIELD);
	}
	
	private void addProjectField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("product");
		field.setName("프로덕트 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 1);
		fieldList.add(field);
	}
	
	private void addMainMenuField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("mainMenu");
		field.setName("메인메뉴 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 2);
		fieldList.add(field);
	}
	
	private void addSubMenuField(String product, String mainMenu, String subMenu) {
		var field = new Field();
		field.setProduct(product);
		field.setMainMenu(mainMenu);
		field.setSubMenu(subMenu);
		field.setColumn("subMenu");
		field.setName("서브메뉴 ID");
		field.setType(FieldType.STRING);
		field.setVisible(true);
		field.setEnableSearch(FieldEnable.ENABLED);
		field.setEnableEdit(FieldEnable.REQUIRED);
		field.setFormOrder((short) 3);
		fieldList.add(field);
	}
	
	private void addDefaultField(String product, String mainMenu, String subMenu) {
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("operator");
			field.setName("최종 수정");
			field.setType(FieldType.STRING);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 20);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("modifyDate");
			field.setName("수정일");
			field.setType(FieldType.DATE);
			field.setVisible(true);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 21);
			fieldList.add(field);
		}
		
		{
			var field = new Field();
			field.setProduct(product);
			field.setMainMenu(mainMenu);
			field.setSubMenu(subMenu);
			field.setColumn("registerDate");
			field.setName("등록일");
			field.setType(FieldType.DATE);
			field.setVisible(false);
			field.setEnableSearch(FieldEnable.DISABLED);
			field.setEnableEdit(FieldEnable.DISABLED);
			field.setFormOrder((short) 22);
			fieldList.add(field);
		}
	}

	@Override
	public List<Field> findList(SettingParameter settingParameter) {
		return fieldList.stream()
				.filter(x -> x.getProduct().equals(settingParameter.product())
						&& x.getMainMenu().equals(settingParameter.mainMenu())
						&& x.getSubMenu().equals(settingParameter.subMenu()))
				.sorted(Comparator.comparing(Field::getFormOrder))
				.toList();
	}

}
