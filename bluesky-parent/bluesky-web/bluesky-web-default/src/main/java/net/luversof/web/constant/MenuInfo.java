package net.luversof.web.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access=AccessLevel.PRIVATE)
public enum MenuInfo {

	BATTLENET_D3_HOME("home", "^/battleNet/d3/index$"),
	BATTLENET_D3_MY_PROFILE("myProfile", "^/battleNet/d3/my/profile$"),
	BATTLENET_D3_PROFILE("profile", "^/battleNet/d3/profile/[\\d]*$"),
	BATTLENET_D3_HERO("hero", "^/battleNet/d3/profile/[\\d]*/hero/[\\d]*$"),
	BATTLENET_D3_ITEM("item", "^/battleNet/d3/data/item/[\\d]*$");
	
	@Getter private String name;
	@Getter private String pathRegex;
	
	public static String getName(String servletPath) {
		for (MenuInfo menuInfo : MenuInfo.values()) {
			if (servletPath.matches(menuInfo.getPathRegex())) {
				return menuInfo.name;
			}
		}
		return null;
	}
	
}
