package net.luversof.web.dynamiccrud.use.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;
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
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_ALL)
	public String findAll(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu, 
			Pageable pageable, 
			@RequestParam Map<String, String> paramMap,
			Model model) {
		
		Query query = ThymeleafUseUtil.getQuery(product, mainMenu, subMenu, QuerySqlCommandType.SELECT);
		
		List<Field> fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		model.addAttribute("fieldList", fieldList);
		
		Page<Map<String, Object>> page = useService.find(query, fieldList, pageable, paramMap);
		model.addAttribute("page", page);
		
		// 화면 처리 관련 정보 
		model.addAttribute("columnMap", ThymeleafUseUtil.getColumnMap(page, fieldList));
		
		// 여기도 필드 정보 기준으로 출력 처리를 해야 할꺼 같은데?
		return "use/fragment/findAll";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_MODAL)
	public String modal(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			Model model) {
		// parameter가 있는 경우 해당 정보를 내려준다.
		
		
		// 수정인 경우는 수정할 데이터를 내려주면 되는데 입력인 경우는 어떻게 구분? sqlCommandType을 사용할까?
//		if ("UPDATE".equals(useParameter.sqlCommandType())) {
//			var selectParameter = useParameter.toBuilder().sqlCommandType("SELECT").build();
			// 해당 컬럼의 데이터만 조회하려면 어케 함?
//		}
		
		
		
		List<Field> fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		model.addAttribute("fieldList", fieldList);
		
		// field를 적절히 보여준다.
		
		return "use/fragment/modal";
	}
	
	@PostMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_MODAL)
	public String modalPost(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			@RequestParam Map<String, String> postData,
			HttpServletResponse response,
			Model model) {
		response.setHeader("HX-Trigger", "modalPost");	// 응답 트리거를 위한 설정
		
		// 생성 처리
		
		
		
		return "use/fragment/modal";
	}
	
	/**
	 * 개발용 api
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
	
}
