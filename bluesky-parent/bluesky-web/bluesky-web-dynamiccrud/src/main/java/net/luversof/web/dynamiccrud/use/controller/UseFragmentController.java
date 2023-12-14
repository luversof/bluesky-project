package net.luversof.web.dynamiccrud.use.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.setting.domain.QuerySqlCommandType;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;
import net.luversof.web.dynamiccrud.use.util.ThymeleafUseUtil;

@Controller
public class UseFragmentController {
	
	@Autowired
	private UseServiceDecorator useService;

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
		
		response.setHeader("HX-Trigger", "fragmentList");	// Htmx 응답 트리거를 위한 설정
		return "use/fragment/list";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL)
	public String modal(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode) {
		return "use/fragment/modal";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL)
	public String createModal(
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
			response.setHeader("HX-Trigger", "createModal");	// Htmx 응답 트리거를 위한 설정
		} else {
			var query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.UPDATE);
			useService.update(query, fieldList, dataMap);
			response.setHeader("HX-Trigger", "updateModal");	// Htmx 응답 트리거를 위한 설정
		}
		
		return "use/fragment/modal";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_MODAL_DELETE)
	public String deleteModal(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@PathVariable String modalMode,
			@RequestParam MultiValueMap<String, String> dataMap,
			HttpServletResponse response,
			Model model) {
		response.setHeader("HX-Trigger", "deleteModal");	// Htmx 응답 트리거를 위한 설정
		
		return "use/fragment/modal";
	}
	
	/**
	 * 개발용 api
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
	
}
