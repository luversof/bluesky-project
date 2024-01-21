package net.luversof.web.dynamiccrud.setting.controller;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
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

	/**
	 * 일단 SELECT 부분을 보여보자.
	 */
	@Override
	public String list(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		model.addAttribute("dbFieldList", dbFieldList);
		
		var page = useService.find(settingParameter, pageable, paramMap);
		model.addAttribute("page", page);
		
		// 응답 데이터 목록 관리 객체
		model.addAttribute("contentInfo", new ContentInfo(page.getContent(), dbFieldList));
		
		// 여기도 필드 정보 기준으로 출력 처리를 해야 할꺼 같은데?
		
		response.setHeader("HX-Trigger", "showList");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/list";
	}
	
	@Override
	public String modalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response,
			Model model) {
		response.setHeader("HX-Trigger", MessageFormat.format("{0}ModalForm,showModalForm", modalMode));	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@Override
	public String createModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		
		// 로그인한 유저 정보 추가 (설정 정보의 경우 필요하여 추가함. 기본 제공 변수 값에 대한 정의가 필요할수도 있음)
		dataMap.put("writer", "bluesky계정");

		if (modalMode.equals("create")) {
			useService.create(settingParameter, dataMap);
			response.setHeader("HX-Trigger", "createModal");	// Htmx 응답 트리거를 위한 설정
		} else {
			useService.update(settingParameter, dataMap);
			response.setHeader("HX-Trigger", "updateModal");	// Htmx 응답 트리거를 위한 설정
		}
		
		return "use/fragment/modalForm";
	}
	
	@Override
	public String deleteModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		useService.delete(settingParameter, dataMap);
		
		response.setHeader("HX-Trigger", "deleteModal");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@Override
	public String modalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response,
			Model model) {
		
		
		response.setHeader("HX-Trigger", MessageFormat.format("{0}ModalBulkForm,showModalForm", modalMode));	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@Override
	public String importModalBulk(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) throws JsonMappingException, JsonProcessingException {
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);
		var dataMapList = objectMapper.readValue(dataMap.get("bulkData"), new TypeReference<List<Map<String, String>>>() {});
		
		for (var map : dataMapList) {
			useService.create(settingParameter, map);
		}
		
		response.setHeader("HX-Trigger", "importModalBulk");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@Override
	public View excel(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		// 다운로드의 페이지 사이즈는 어떻게 처리할지 고민 필요. 일단 기존 호출 방식을 활용
		list(adminProjectId, projectId, mainMenuId, subMenuId, PageRequest.of(0, 65536), paramMap, response, model);
		return new UseExcelView();
	}
}
