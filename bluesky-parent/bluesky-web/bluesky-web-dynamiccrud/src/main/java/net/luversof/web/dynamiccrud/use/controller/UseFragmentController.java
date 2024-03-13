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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.github.luversof.boot.htmx.annotation.HtmxResponseHeader;
import net.luversof.web.dynamiccrud.setting.controller.AbstractSettingFragmentController;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class UseFragmentController extends AbstractSettingFragmentController {
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_LIST)
	@HtmxResponseHeader("listFragmentResponseTrigger")
	public String useList(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			Model model) {
		return list(adminProjectId, projectId, mainMenuId, subMenuId, pageable, paramMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	@HtmxResponseHeader("#{modalMode}ModalFormFragmentResponseTrigger,showModalFormFragmentResponseTrigger")
	public String useModalForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		return modalForm(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM)
	@HtmxResponseHeader("#{modalMode}ModalResponseTrigger")
	@ResponseBody
	public void useCreateModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) {
		createModal(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_FORM_DELETE)
	@HtmxResponseHeader("deleteModalResponseTrigger")
	@ResponseBody
	public void useDeleteModal(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			Model model) {
		deleteModal(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	@HtmxResponseHeader("#{modalMode}ModalBulkFormFragmentResponseTrigger,showModalFormFragmentResponseTrigger")
	public String useModalBulkForm(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		return modalBulkForm(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, model);
	}
	
	@PostMapping(UrlConstant.PATH_USE_FRAGMENT_MODAL_BULK_FORM)
	@HtmxResponseHeader("importModalBulkResponseTrigger")
	@ResponseBody
	public void useImportModalBulk(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) throws JsonMappingException, JsonProcessingException {
		importModalBulk(adminProjectId, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_USE_FRAGMENT_EXCEL)
	public View useExcel(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			Pageable pageable,
			Model model) {
		return excel(adminProjectId, projectId, mainMenuId, subMenuId, paramMap, pageable, model);
	}
	
}
