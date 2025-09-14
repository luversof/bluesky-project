package net.luversof.web.gate.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import lombok.experimental.UtilityClass;
import net.luversof.web.common.menu.domain.Menu;
import net.luversof.web.common.util.WebCommonUtil;

@UtilityClass
public class ThymeleafUtil {
	
	private static final String[] THEMES = new String[] {
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

	public static List<Menu> getMenuList(String key) {
		return WebCommonUtil.getMenuList(key);
	}
	
	public static String getRandomTheme() {
		return getRandomTheme(THEMES);
	}
	
	public static String getRandomTheme(String...themes) {
		var themeList = List.of(themes);
		return themeList.get(RANDOM.nextInt(themeList.size()));
	}
	
	public static List<String> getThemeList() {
		return Arrays.asList(THEMES);
	}
}
