package net.luversof.web.dynamiccrud.setting.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.use.domain.ContentInfo;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;
import net.luversof.web.dynamiccrud.use.view.UseExcelView;

public abstract class AbstractSettingFragmentController implements SettingFragmentControllerInterface {
	
	@Autowired
	private UseServiceDecorator useService;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Override
	public String list(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			Model model) {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		var subMenu = SettingUtil.getSubMenu();
		
		if (subMenu == null) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		
		
		if (!subMenu.isEnableDisplay()) {
			throw new BlueskyException("NOT_USE_SUBMENU");
		}
		
		pageable = PageRequest.of(pageable.getPageNumber(), subMenu.getPageSize(), pageable.getSort());
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		
		addSpelForEditColumnToDataMap(settingParameter, paramMap);
		
		var page = useService.find(settingParameter, pageable, paramMap);
		model.addAttribute("page", page);
		
		// 응답 데이터 목록 관리 객체
		model.addAttribute("contentInfo", new ContentInfo(page.getContent(), dbFieldList));
		
		// 여기도 필드 정보 기준으로 출력 처리를 해야 할꺼 같은데?
		
		return "use/fragment/list";
	}
	
	@Override
	public String modalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		return "use/fragment/modalForm";
	}
	
	@Override
	public void createModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) {
		
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		
		// 로그인한 유저 정보 추가 (설정 정보의 경우 필요하여 추가함. 기본 제공 변수 값에 대한 정의가 필요할수도 있음)
		dataMap.put("writer", "bluesky계정");
		
		addSpelForEditColumnToDataMap(settingParameter, dataMap);

		if (modalMode.equals("create")) {
			useService.create(settingParameter, dataMap);
		} else {
			useService.update(settingParameter, dataMap);
		}
	}
	
	@Override
	public void deleteModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			Model model) {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		useService.delete(settingParameter, dataMap);
	}
	
	@Override
	public String modalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		return "use/fragment/modalBulkForm";
	}
	
	@Override
	public void importModalBulk(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) throws JsonMappingException, JsonProcessingException {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		var dataMapList = objectMapper.readValue(dataMap.get("bulkData"), new TypeReference<List<Map<String, String>>>() {});
		
		for (var map : dataMapList) {
			useService.create(settingParameter, map);
		}
	}
	
	@Override
	public View excel(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			Pageable pageable,
			Model model) {
		// 다운로드의 페이지 사이즈는 어떻게 처리할지 고민 필요. 일단 기존 호출 방식을 활용
		list(adminProjectId, projectId, mainMenuId, subMenuId, pageable, paramMap, model);
		return new UseExcelView();
	}
	
	// DbFieldColumnType이 SPEL_FOR_EDIT인 경우 추가	
	private void addSpelForEditColumnToDataMap(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		// DbFieldColumnType이 SPEL_FOR_EDIT인 경우 추가
		if (dbFieldList.stream().anyMatch(dbField -> dbField.getColumnType() == DbFieldColumnType.SPEL_FOR_EDIT)) {
			var evaluationContext = new StandardEvaluationContext();
			dataMap.forEach(evaluationContext::setVariable);
			var expressionParser = new SpelExpressionParser();
			
			dbFieldList.stream().filter(dbField -> dbField.getColumnType() == DbFieldColumnType.SPEL_FOR_EDIT).forEach(dbField -> {
				var expression = expressionParser.parseExpression(dbField.getColumnFormat());
				Object value = expression.getValue(evaluationContext);
				dataMap.put(dbField.getColumnId(), String.valueOf(value));
			});
		}
	}
}
