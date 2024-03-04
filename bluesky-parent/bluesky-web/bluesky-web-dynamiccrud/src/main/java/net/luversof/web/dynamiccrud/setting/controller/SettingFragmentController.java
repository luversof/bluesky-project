package net.luversof.web.dynamiccrud.setting.controller;

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
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class SettingFragmentController extends AbstractSettingFragmentController {

	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_LIST)
	public String settingList(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return list(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, pageable, paramMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM)
	public String settingModalForm(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return modalForm(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM)
	public String settingCreateModal(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return  createModal(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM_DELETE)
	public String settingDeleteModal(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return deleteModal(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_BULK_FORM)
	public String settingModalBulkForm(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return modalBulkForm(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, response, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_BULK_FORM)
	public String settingImportModalBulk(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			HttpServletResponse response,
			Model model) throws JsonMappingException, JsonProcessingException {
		addAttribute(model);
		return importModalBulk(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, response, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_EXCEL)
	public View settingExcel(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			Pageable pageable,
			HttpServletResponse response,
			Model model) {
		addAttribute(model);
		return excel(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, paramMap, pageable, response, model);
	}
	
	
	private void addAttribute(Model model) {
		model.addAttribute(SettingConstant.ADMIN_PROJECT_ID, AdminConstant.ADMIN_PROJECT_ID_VALUE);
	}
}
