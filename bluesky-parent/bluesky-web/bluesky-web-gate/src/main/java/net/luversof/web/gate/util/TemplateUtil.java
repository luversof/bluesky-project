package net.luversof.web.gate.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import net.luversof.web.common.util.WebCommonUtil;

@DevCheckUtil
public final class TemplateUtil {

  private TemplateUtil() {}

  private static final String[] THEMES =
      new String[] {
        "bluesky",
        "light",
        "dark",
        "cupcake",
        "bumblebee",
        "emerald",
        "corporate",
        "synthwave",
        "retro",
        "cyberpunk",
        "valentine",
        "halloween",
        "garden",
        "forest",
        "aqua",
        "lofi",
        "pastel",
        "fantasy",
        "wireframe",
        "black",
        "luxury",
        "dracula",
        "cmyk",
        "autumn",
        "business",
        "acid",
        "lemonade",
        "night",
        "coffee",
        "winter",
        "dim",
        "nord",
        "sunset",
        "caramellatte",
        "abyss",
        "silk"
      };

  private static final Random RANDOM = new Random();

  /**
   * Returns the menu list for the given key. The method intentionally avoids a compile time
   * dependency on the `Menu` type. If the WebCommonUtil or the Menu type is not available at
   * runtime (e.g., missing class), the method returns an empty list instead of throwing
   * `UnresolvedCompilationErrors` or `NoClassDefFoundError`.
   */
  @DevCheckDescription("Returns the menu list for the given key. If the WebCommonUtil or the Menu type is not available at runtime, returns an empty list.")
  public static List<?> getMenuList(String key) {
    try {
      return WebCommonUtil.getMenuList(key);
    } catch (NoClassDefFoundError | Exception e) {
      return List.of();
    }
  }

  public static String getRandomTheme() {
    return getRandomTheme(THEMES);
  }

  public static String getRandomTheme(String... themes) {
    var themeList = List.of(themes);
    return themeList.get(RANDOM.nextInt(themeList.size()));
  }

  public static List<String> getThemeList() {
    return Arrays.asList(THEMES);
  }
}
