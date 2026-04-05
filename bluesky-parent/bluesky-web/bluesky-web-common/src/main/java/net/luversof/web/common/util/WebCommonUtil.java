package net.luversof.web.common.util;

import java.util.List;

import io.github.luversof.boot.context.ApplicationContextUtil;
import net.luversof.web.common.config.WebCommonProperties;
import net.luversof.web.common.menu.domain.Menu;

public final class WebCommonUtil {

  private WebCommonUtil() {}

  public static List<Menu> getMenuList(String key) {
    return ApplicationContextUtil.getApplicationContext()
        .getBean(WebCommonProperties.class)
        .menu()
        .get(key);
  }
}
