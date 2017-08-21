package net.luversof.web.index.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.luversof.web.constant.AuthorizeRole;
import net.luversof.web.index.service.MenuService;

@RestController
@RequestMapping(value = "/_check", produces = MediaType.APPLICATION_JSON_VALUE)
public class DevCheckController {
	
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private ConfigurableWebEnvironment environment;
	
	@Autowired
	private SpringResourceTemplateResolver defaultTemplateResolver;

	@GetMapping("/menuReload")
	public void menuReload() {
		menuService.reload();
	}
	
	@GetMapping("/allProperties")
	@SneakyThrows
	public Map<String, Object> allProperties() {
		
		String keyPrefix = "";
		Map<String, Object> subProperties = new LinkedHashMap<String, Object>();
		for (PropertySource<?> source : environment.getPropertySources()) {
			if (source instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
					String key = getSubKey(name, "", keyPrefix);
					if (key != null && !subProperties.containsKey(key)) {
						subProperties.put(key, source.getProperty(name));
					}
				}
			}
		}
		
		return subProperties;
	}
	
	private static String getSubKey(String name, String rootPrefix, String keyPrefix) {
		if (name.startsWith(rootPrefix + keyPrefix)) {
			return name.substring((rootPrefix + keyPrefix).length());
		}
		return null;
	}
	
	@GetMapping("/decoupledLogicToggle")
	public boolean decoupledLogicToggle() {
		defaultTemplateResolver.setUseDecoupledLogic(!defaultTemplateResolver.getUseDecoupledLogic());
		return defaultTemplateResolver.getUseDecoupledLogic();
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
	
//	@Autowired
//	private TestGateway testGateway;
//	
//	@GetMapping("/addQueue")
//	public int addQueue(@RequestParam(defaultValue = "1") int count) {
//		for (int i = 0 ; i < count ; i++) {
//			testGateway.sendToRabbit("테스트호출 " + i);
//		}
//		return count;
//	}
}
