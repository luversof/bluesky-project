package net.luversof.web.dynamiccrud.use.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;
import net.luversof.web.dynamiccrud.use.util.ThymeleafUseUtil;
import net.luversof.web.dynamiccrud.use.view.UseExcelView;

@Controller
public class UseFragmentController {
	
	@Autowired
	private UseServiceDecorator useService;
	
	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * 일단 SELECT 부분을 보여보자.
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_LIST)
	public String list(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu, 
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		
		var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.SELECT);
		var fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		model.addAttribute("fieldList", fieldList);
		
		var page = useService.find(query, fieldList, pageable, paramMap);
		model.addAttribute("page", page);
		
		// 화면 처리 관련 정보 
		model.addAttribute("columnMap", ThymeleafUseUtil.getColumnMap(page, fieldList));
		
		// 여기도 필드 정보 기준으로 출력 처리를 해야 할꺼 같은데?
		
		response.setHeader("HX-Trigger", "showList");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/list";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String modalForm(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			HttpServletResponse response) {
		response.setHeader("HX-Trigger", modalMode.equals("create") ? "showCreateModalForm" : "showUpdateModalForm");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String createModalForm(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		
		var fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		
		// 로그인한 유저 정보 추가 (설정 정보의 경우 필요하여 추가함. 기본 제공 변수 값에 대한 정의가 필요할수도 있음)
		dataMap.put("operator", "bluesky계정");

		if (modalMode.equals("create")) {
			var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.INSERT);
			useService.create(query, fieldList, dataMap);
			response.setHeader("HX-Trigger", "createModalForm,closeModalForm");	// Htmx 응답 트리거를 위한 설정
		} else {
			var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.UPDATE);
			useService.update(query, fieldList, dataMap);
			response.setHeader("HX-Trigger", "updateModalForm");	// Htmx 응답 트리거를 위한 설정
		}
		
		return "use/fragment/modalForm";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_FORM_DELETE)
	public String deleteModalForm(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.DELETE);
		var fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		useService.delete(query, fieldList, dataMap);
		
		response.setHeader("HX-Trigger", "deleteModalForm");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String modalBulkForm(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			HttpServletResponse response) {
		response.setHeader("HX-Trigger", modalMode.equals("import") ? "showImportModalBulkForm" : "showExportModalBulkForm");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String importModalBulkForm(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response) throws JsonMappingException, JsonProcessingException {
		var dataMapList = objectMapper.readValue(dataMap.get("bulkData"), new TypeReference<List<Map<String, String>>>() {});
		
		var fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.INSERT);
		for (var map : dataMapList) {
			useService.create(query, fieldList, map);
		}
		
		response.setHeader("HX-Trigger", "importModalBulkForm");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_EXCEL)
	public View excel(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		// 다운로드의 페이지 사이즈는 어떻게 처리할지 고민 필요. 일단 기존 호출 방식을 활용
		list(product, mainMenu, subMenu, PageRequest.of(0, 65536), paramMap, response, model);
		return new UseExcelView();
	}
	
	/**
	 * 개발용 api
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
	
}
