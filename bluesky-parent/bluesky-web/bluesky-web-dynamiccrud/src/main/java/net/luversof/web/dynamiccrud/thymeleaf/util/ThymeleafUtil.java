package net.luversof.web.dynamiccrud.thymeleaf.util;

import java.util.List;

import org.springframework.data.domain.Page;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.thymeleaf.config.DynamicCrudThymeleafProperties;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;
import net.luversof.web.dynamiccrud.thymeleaf.domain.Pagination;

@UtilityClass
public class ThymeleafUtil {

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(DynamicCrudThymeleafProperties.class).menu().get(key);
	}
	
	public static Pagination getPagination(@SuppressWarnings("rawtypes") Page page) {
		return new Pagination(page);
	}
}
