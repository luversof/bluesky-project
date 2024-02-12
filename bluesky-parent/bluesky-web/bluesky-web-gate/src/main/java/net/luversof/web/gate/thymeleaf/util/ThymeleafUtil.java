package net.luversof.web.gate.thymeleaf.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.github.luversof.boot.util.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.web.gate.thymeleaf.config.GateThymeleafProperties;
import net.luversof.web.gate.thymeleaf.layout.domain.Menu;

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
			"sunset"
	};

	public static List<Menu> getMenuList(String key) {
		return ApplicationContextUtil.getApplicationContext().getBean(GateThymeleafProperties.class).menu().get(key);
	}
	
	public static String getRandomTheme(String...themes) {
		Random random = new Random();
		var themeList = List.of(themes);
		return themeList.get(random.nextInt(themeList.size()));
	}
	
	public static List<String> getThemeList() {
		return Arrays.asList(THEMES);
	}
}
