package net.luversof.web.dynamiccrud.use.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.thymeleaf.constant.DynamicCrudConstant;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;
import net.luversof.web.dynamiccrud.use.util.ThymeleafUseUtil;

/**
 * 설정된 데이터를 호출하여 화면을 구성
 */
@Controller
public class UseController {
	
	@GetMapping("/use/{product}/{mainMenu}")
	public String redirectView(@PathVariable String product, @PathVariable String mainMenu) {
		var subMenuList = ThymeleafUseUtil.getSubMenuList(product, mainMenu);
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		return "redirect:" + subMenuList.get(0).getUrl();
		
	}

	@GetMapping(DynamicCrudConstant.PATH_USE_VIEW_INDEX)
	public String view(
			@PathVariable String product, 
			@PathVariable String mainMenu, 
			@PathVariable String subMenu,
			Model model) {
		
		/** (s)  상단 메뉴 처리 **/
		var subMenuList = ThymeleafUseUtil.getSubMenuList(product, mainMenu);
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		
		model.addAttribute("subMenuList", subMenuList);
		
		if (subMenu != null && !subMenuList.stream().anyMatch(x -> x.getSubMenu().equals(subMenu))) {
			throw new BlueskyException("INVALID_SUBMENU");
		}
		
		// SubMenu를 화면 구성에 사용되는 Menu로 전환... 할 필요가 있나? (기존거 맞추느라 이렇게 한 상태)
		var menuList = new ArrayList<Menu>();
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		subMenuList.forEach(x -> {
			var menu = new Menu();
			menu.setUrl(x.getUrl());
			menu.setMessageCode(x.getSubMenuName());
			menuList.add(menu);
		});
		
		model.addAttribute("menuList", menuList);
		
		// Setting 정보를 기준으로 해당 데이터를 조회
		
		List<Field> fieldList = ThymeleafUseUtil.getFieldList(product, mainMenu, subMenu);
		model.addAttribute("fieldList", fieldList);
		model.addAttribute("hasSearchField", fieldList.stream().anyMatch(x -> x.isEnableSearch()));
		
		/** (e)  상단 메뉴 처리 **/
		
		return "use/index";
	}
	
}
