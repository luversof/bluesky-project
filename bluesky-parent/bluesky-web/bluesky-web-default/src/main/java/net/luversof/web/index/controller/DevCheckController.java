package net.luversof.web.index.controller;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.luversof.web.constant.AuthorizeRole;
import net.luversof.web.index.service.MenuService;

@RestController
@RequestMapping("/_check/")
public class DevCheckController {
	
	@Autowired
	private MenuService menuService;

	@GetMapping("/menuLoad")
	public void menuLoad() {
		menuService.load();
	}
	

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class User {
		@NotNull(groups = CheckName.class)
		private String name;
		private String type;
		
		public static interface CheckName {}
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@GetMapping("/exceptionOrderTest")
	public void exceptionOrderTest(@Validated(User.CheckName.class) User user, ModelMap modelMap) {
		modelMap.addAttribute(user);
	}
}
