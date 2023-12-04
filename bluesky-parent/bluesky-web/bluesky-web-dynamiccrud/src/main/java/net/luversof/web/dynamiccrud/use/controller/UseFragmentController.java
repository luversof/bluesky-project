package net.luversof.web.dynamiccrud.use.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.service.FieldService;
import net.luversof.web.dynamiccrud.setting.service.QueryService;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;
import net.luversof.web.dynamiccrud.use.domain.UseParameter;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;

@Controller
public class UseFragmentController {
	
	
	@Autowired
	private QueryService queryService;
	
	@Autowired
	private FieldService fieldService;
	
	@Autowired
	private UseServiceDecorator useService;

	/**
	 * 일단 SELECT 부분을 보여보자.
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_ALL)
	public String findAll(UseParameter useParameter, Pageable pageable, Model model) {
		var parameter = useParameter.toBuilder().sqlCommandType("SELECT").build();
		Query query = getQuery(parameter);
		model.addAttribute("page", useService.find(query, pageable));
		return "use/fragment/findAll";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_MODAL)
	public String modal(UseParameter useParameter, Model model) {
		// parameter가 있는 경우 해당 정보를 내려준다.
		
		
		// 수정인 경우는 수정할 데이터를 내려주면 되는데 입력인 경우는 어떻게 구분?
		
		
		
		List<Field> fieldList = getFieldList(useParameter);
		
		// field를 적절히 보여준다.
		
		return "use/fragment/modal";
	}

	private Query getQuery(UseParameter useParameter) {
		List<Query> queryList = queryService.findByProductAndMainMenuAndSubMenu(useParameter.product(), useParameter.mainMenu(), useParameter.subMenu());
		return queryList.stream().filter(x -> x.getSqlCommandType().equals(useParameter.sqlCommandType())).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_QUERY"));
	}
	
	private List<Field> getFieldList(UseParameter useParameter) {
		List<Field> fieldList = fieldService.findByProductAndMainMenuAndSubMenu(useParameter.product(), useParameter.mainMenu(), useParameter.subMenu());
		return fieldList;
	}
	

	/**
	 * 개발용 api
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
	
}
