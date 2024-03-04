package net.luversof.web.dynamiccrud.use.controller;

import java.util.Map;

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
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.setting.controller.AbstractSettingFragmentController;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class UseFragmentController extends AbstractSettingFragmentController {
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_LIST)
	public String useList(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		return list(adminProjectId, projectId, mainMenuId, subMenuId, pageable, paramMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String useModalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response, 
			Model model) {
		return modalForm(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	public String useCreateModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		return  createModal(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM_DELETE)
	public String useDeleteModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		return deleteModal(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String useModalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response,
			Model model) {
		return modalBulkForm(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	public String useImportModalBulk(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) throws JsonMappingException, JsonProcessingException {
		return importModalBulk(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_EXCEL)
	public View useExcel(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			Pageable pageable,
			HttpServletResponse response,
			Model model) {
		return excel(adminProjectId, projectId, mainMenuId, subMenuId, paramMap, pageable, response, model);
	}
	
}
