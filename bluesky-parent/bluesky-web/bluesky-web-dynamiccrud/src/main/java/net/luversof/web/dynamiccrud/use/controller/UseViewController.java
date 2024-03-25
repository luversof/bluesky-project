package net.luversof.web.dynamiccrud.use.controller;

import java.util.Comparator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.controller.AbstractSettingViewController;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.thymeleaf.constant.UrlConstant;

/**
 * 설정된 데이터를 호출하여 화면을 구성
 */
@Controller
public class UseViewController extends AbstractSettingViewController {
	
	@GetMapping("/{adminProjectId}/use/{projectId}/{mainMenuId}")
	public String redirectView(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId) {
		var subMenuList = SettingUtil.getSubMenuList(adminProjectId, projectId, mainMenuId);
		if (subMenuList.isEmpty()) {
			throw new BlueskyException("NOT_EXIST_SUBMENU");
		}
		subMenuList.sort(Comparator.comparing(SubMenu::getDisplayOrder));
		return "redirect:" + subMenuList.get(0).getUrl();
		
	}

	@GetMapping(UrlConstant.PATH_USE_VIEW_INDEX)
	public String useView(
			@PathVariable String adminProjectId,
			@PathVariable String projectId, 
			@PathVariable String mainMenuId, 
			@PathVariable String subMenuId,
			Model model) {
		return view(adminProjectId, projectId, mainMenuId, subMenuId, model);
	}


	@Override
	protected void checkPathVariable(String adminProjectId, String projectId, String mainMenuId, String subMenuId) {
		if (AdminConstant.ADMIN_PROJECT_ID_VALUE.equals(adminProjectId)) {
			throw new BlueskyException("INVALID ACCESS");
		}
	}
	
}
