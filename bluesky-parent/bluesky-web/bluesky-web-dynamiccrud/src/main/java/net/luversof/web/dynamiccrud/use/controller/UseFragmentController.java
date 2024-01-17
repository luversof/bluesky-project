package net.luversof.web.dynamiccrud.use.controller;

import java.text.MessageFormat;
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
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;
import net.luversof.web.dynamiccrud.use.domain.ContentInfo;
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
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_LIST)
	public String list(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId, 
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		
		var dbQuery = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.SELECT);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		model.addAttribute("dbFieldList", dbFieldList);
		
		var page = useService.find(dbQuery, dbFieldList, pageable, paramMap);
		model.addAttribute("page", page);
		
		// 응답 데이터 목록 관리 객체
		model.addAttribute("contentInfo", new ContentInfo(page.getContent(), dbFieldList));
		
		// 여기도 필드 정보 기준으로 출력 처리를 해야 할꺼 같은데?
		
		response.setHeader("HX-Trigger", "showList");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/list";
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String modalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response) {
		response.setHeader("HX-Trigger", MessageFormat.format("{0}ModalForm,showModalForm", modalMode));	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String createModalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		
		// 로그인한 유저 정보 추가 (설정 정보의 경우 필요하여 추가함. 기본 제공 변수 값에 대한 정의가 필요할수도 있음)
		dataMap.put("writer", "bluesky계정");

		if (modalMode.equals("create")) {
			var dbQuery = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.INSERT);
			useService.create(dbQuery, dbFieldList, dataMap);
			response.setHeader("HX-Trigger", "createModal");	// Htmx 응답 트리거를 위한 설정
		} else {
			var dbQuery = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.UPDATE);
			useService.update(dbQuery, dbFieldList, dataMap);
			response.setHeader("HX-Trigger", "updateModal");	// Htmx 응답 트리거를 위한 설정
		}
		
		return "use/fragment/modalForm";
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM_DELETE)
	public String deleteModalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		var dbQuery = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.DELETE);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		useService.delete(dbQuery, dbFieldList, dataMap);
		
		response.setHeader("HX-Trigger", "deleteModal");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalForm";
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String modalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response) {
		
		
		response.setHeader("HX-Trigger", MessageFormat.format("{0}ModalBulkForm,showModalForm", modalMode));	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String importModalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response) throws JsonMappingException, JsonProcessingException {
		var dataMapList = objectMapper.readValue(dataMap.get("bulkData"), new TypeReference<List<Map<String, String>>>() {});
		
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		var dbQuery = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.INSERT);
		for (var map : dataMapList) {
			useService.create(dbQuery, dbFieldList, map);
		}
		
		response.setHeader("HX-Trigger", "importModalBulk");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/modalBulkForm";
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_EXCEL)
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
	
	/**
	 * 개발용 api
	 */
	@GetMapping(UrlConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
	
}
