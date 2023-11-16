package net.luversof.web.gate.thymeleaf.util;

import java.util.List;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.gate.thymeleaf.config.GateThymeleafProperties;
import net.luversof.web.gate.thymeleaf.layout.domain.Menu;

@UtilityClass
public class GateThymeleafUtil {

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(GateThymeleafProperties.class).menu().get(key);
	}
	
}
