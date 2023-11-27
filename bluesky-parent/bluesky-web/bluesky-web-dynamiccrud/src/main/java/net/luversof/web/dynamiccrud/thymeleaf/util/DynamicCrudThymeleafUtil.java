package net.luversof.web.dynamiccrud.thymeleaf.util;

import java.util.List;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.thymeleaf.config.DynamicCrudThymeleafProperties;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;

@UtilityClass
public class DynamicCrudThymeleafUtil {

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(DynamicCrudThymeleafProperties.class).menu().get(key);
	}
}
