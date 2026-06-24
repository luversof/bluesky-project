package net.luversof.web.gate.util;

import java.util.List;

import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import net.luversof.web.common.util.WebCommonUtil;

@DevCheckUtil
public final class TemplateUtil {

  private TemplateUtil() {}

  /**
   * Returns the menu list for the given key. The method intentionally avoids a compile time
   * dependency on the `Menu` type. If the WebCommonUtil or the Menu type is not available at
   * runtime (e.g., missing class), the method returns an empty list instead of throwing
   * `UnresolvedCompilationErrors` or `NoClassDefFoundError`.
   */
  @DevCheckDescription(
      "Returns the menu list for the given key. If the WebCommonUtil or the Menu type is not available at runtime, returns an empty list.")
  public static List<?> getMenuList(String key) {
    try {
      return WebCommonUtil.getMenuList(key);
    } catch (NoClassDefFoundError | Exception e) {
      return List.of();
    }
  }

  /** 쿼리 파라미터용 URL 인코딩(템플릿에서 종목명을 링크에 안전하게 넣기 위함). */
  public static String urlEncode(String value) {
    if (value == null) {
      return "";
    }
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }
}
