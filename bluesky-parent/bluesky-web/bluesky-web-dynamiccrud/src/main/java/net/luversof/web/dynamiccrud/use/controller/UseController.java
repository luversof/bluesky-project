package net.luversof.web.dynamiccrud.use.controller;

import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
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

	@GetMapping({ "/{product}/{mainMenu}", "/{product}/{mainMenu}/{subMenu}" })
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
		
		var menuList = new ArrayList<Menu>();
		subMenuList.sort(Comparator.comparingInt(SubMenu::getDisplayOrder));
		subMenuList.forEach(subMenu -> {
			var menu = new Menu();
			menu.setUrl(subMenu.getUrl());
			menu.setMessageCode(subMenu.getSubMenuName());
			menuList.add(menu);
		});
		
		model.addAttribute("menuList", menuList);
		
		if (!StringUtils.hasText(useParameter.subMenu())) {
			return "redirect:" + menuList.get(0).getUrl();
		}
		
		var mainMenu = mainMenuService.findByProductAndMainMenu(useParameter.product(), useParameter.mainMenu());
		model.addAttribute("mainMenu", mainMenu);
		
		// Setting 정보를 기준으로 해당 데이터를 조회
		
		return "use/index";
	}
}
