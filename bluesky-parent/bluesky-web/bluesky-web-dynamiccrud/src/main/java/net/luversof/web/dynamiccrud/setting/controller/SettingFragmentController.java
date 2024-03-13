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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.github.luversof.boot.htmx.annotation.HtmxResponseHeader;
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

@Controller
public class SettingFragmentController extends AbstractSettingFragmentController {

	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_LIST)
	@HtmxResponseHeader("listFragmentResponseTrigger")
	public String settingList(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			Model model) {
		addAttribute(model);
		return list(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, pageable, paramMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM)
	@HtmxResponseHeader("#{modalMode}ModalFormFragmentResponseTrigger,showModalFormFragmentResponseTrigger")
	public String settingModalForm(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		addAttribute(model);
		return modalForm(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM)
	@HtmxResponseHeader("#{modalMode}ModalResponseTrigger")
	@ResponseBody
	public void settingCreateModal(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) {
		addAttribute(model);
		createModal(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_FORM_DELETE)
	@HtmxResponseHeader("deleteModalResponseTrigger")
	@ResponseBody
	public void settingDeleteModal(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			Model model) {
		addAttribute(model);
		deleteModal(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_BULK_FORM)
	@HtmxResponseHeader("#{modalMode}ModalBulkFormFragmentResponseTrigger,showModalFormFragmentResponseTrigger")
	public String settingModalBulkForm(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			Model model) {
		addAttribute(model);
		return modalBulkForm(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, model);
	}
	
	@PostMapping(UrlConstant.PATH_SETTING_FRAGMENT_MODAL_BULK_FORM)
	@HtmxResponseHeader("importModalBulkResponseTrigger")
	@ResponseBody
	public void settingImportModalBulk(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@PathVariable String modalMode,
			@RequestParam Map<String, String> dataMap,
			Model model) throws JsonMappingException, JsonProcessingException {
		addAttribute(model);
		importModalBulk(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, modalMode, dataMap, model);
	}
	
	@GetMapping(UrlConstant.PATH_SETTING_FRAGMENT_EXCEL)
	public View settingExcel(
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			@RequestParam Map<String, String> paramMap,
			Pageable pageable,
			Model model) {
		addAttribute(model);
		return excel(AdminConstant.ADMIN_PROJECT_ID_VALUE, projectId, mainMenuId, subMenuId, paramMap, pageable, model);
	}
	
	
	private void addAttribute(Model model) {
		model.addAttribute(SettingConstant.ADMIN_PROJECT_ID, AdminConstant.ADMIN_PROJECT_ID_VALUE);
	}
}
