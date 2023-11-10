package net.luversof.web.gate.thymeleaf.util;

import java.util.List;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.gate.thymeleaf.config.GateThymeleafProperties;
import net.luversof.web.gate.thymeleaf.layout.domain.MainMenu;

@UtilityClass
public class GateThymeleafUtil {

	public static List<MainMenu> getMainMenuList() {
		return ApplicationContextUtil.getApplicationContext().getBean(GateThymeleafProperties.class).mainMenuList();
	}
}
