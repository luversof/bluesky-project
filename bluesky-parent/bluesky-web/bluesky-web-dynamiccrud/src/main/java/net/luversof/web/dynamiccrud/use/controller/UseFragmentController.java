package net.luversof.web.dynamiccrud.use.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.service.QueryService;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;
import net.luversof.web.dynamiccrud.use.domain.UseParameter;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;

@Controller
public class UseFragmentController {
	
	
	@Autowired
	private QueryService queryService;
	
	@Autowired
	private UseServiceDecorator useService;

	/**
	 * 일단 SELECT 부분을 보여보자.
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_ALL)
	public String findAll(UseParameter useParameter, Pageable pageable, Model model) {
		List<Query> queryList = queryService.findByProductAndMainMenuAndSubMenu(useParameter.product(), useParameter.mainMenu(), useParameter.subMenu());
		Query query = queryList.stream().filter(x -> x.getSqlCommandType().equals("SELECT")).findAny().orElseThrow(() -> new BlueskyException("NOT_EXIST_SELECT_QUERY"));
		model.addAttribute("page", useService.find(query, pageable));
		return "use/fragment/findAll";
	}
	
	@GetMapping(DynamicCrudConstant.PATH_USE_FRAGMENT_FIND_MODAL)
	public String modal() {
		return "use/fragment/modal";
	}
	
	

	/**
	 * 개발용 api
	 */
	@GetMapping(DynamicCrudConstant.PATH_USE_PREFIX + "/_components/*")
	public void components() {
	}
}
