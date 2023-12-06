package net.luversof.web.dynamiccrud.use.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.FieldService;
import net.luversof.web.dynamiccrud.setting.service.MainMenuService;
import net.luversof.web.dynamiccrud.setting.service.SubMenuService;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;
import net.luversof.web.dynamiccrud.use.domain.UseParameter;

/**
 * 설정된 데이터를 호출하여 화면을 구성
 */
@Controller
@RequestMapping("/use")
public class UseController {
	
	@Autowired
	private MainMenuService mainMenuService;
	
	@Autowired
	private SubMenuService subMenuService;
	
	@Autowired
	private FieldService fieldService;
	
	@GetMapping("/{product}/{mainMenu}")
	public String redirectView(UseParameter useParameter) {
		var subMenuList = subMenuService.findByProductAndMainMenu(useParameter.product(), useParameter.mainMenu());
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		return "redirect:" + subMenuList.get(0).getUrl();
		
	}

	@GetMapping( "/{product}/{mainMenu}/{subMenu}" )
	public String view(UseParameter useParameter, Model model) {
		
		// Setting 정보를 호출하여 해당 요청에 대한 설정이 있는지 확인
		// Product, MainMenu는 볼것도 없이 SubMenu만 바로 조회하면 될 듯?
		
		var subMenuList = subMenuService.findByProductAndMainMenu(useParameter.product(), useParameter.mainMenu());
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		
		model.addAttribute("subMenuList", subMenuList);
		
		if (useParameter.subMenu() != null && !subMenuList.stream().anyMatch(subMenu -> subMenu.getSubMenu().equals(useParameter.subMenu()))) {
			throw new BlueskyException("INVALID_SUBMENU");
		}
		
		model.addAttribute("useParameter", useParameter);
		
		// SubMenu를 화면 구성에 사용되는 Menu로 전환... 할 필요가 있나? (기존거 맞추느라 이렇게 한 상태)
		var menuList = new ArrayList<Menu>();
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		subMenuList.forEach(subMenu -> {
			var menu = new Menu();
			menu.setUrl(subMenu.getUrl());
			menu.setMessageCode(subMenu.getSubMenuName());
			menuList.add(menu);
		});
		
		model.addAttribute("menuList", menuList);
		
		var mainMenu = mainMenuService.findByProductAndMainMenu(useParameter.product(), useParameter.mainMenu());
		model.addAttribute("mainMenu", mainMenu);
		
		// Setting 정보를 기준으로 해당 데이터를 조회
		
		List<Field> fieldList = getFieldList(useParameter);
		model.addAttribute("fieldList", fieldList);
		model.addAttribute("hasSearchField", fieldList.stream().anyMatch(x -> x.isEnableEdit()));
		
		return "use/index";
	}
	
	private List<Field> getFieldList(UseParameter useParameter) {
		List<Field> fieldList = fieldService.findByProductAndMainMenuAndSubMenu(useParameter.product(), useParameter.mainMenu(), useParameter.subMenu());
		return fieldList;
	}
	
}
