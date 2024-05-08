package net.luversof.web.common.menu.util;

import java.util.List;

import io.github.luversof.boot.context.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.common.menu.config.WebCommonProperties;
import net.luversof.web.common.menu.domain.Menu;

@UtilityClass
public class WebCommonUtil {

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(WebCommonProperties.class).menu().get(key);
	}
	
}
