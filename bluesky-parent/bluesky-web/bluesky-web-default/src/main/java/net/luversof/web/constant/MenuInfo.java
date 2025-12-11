package net.luversof.web.constant;

public enum MenuInfo {

	BATTLENET_D3_HOME("home", "^/battleNet/d3/index$"),
	BATTLENET_D3_MY_PROFILE("myProfile", "^/battleNet/d3/my/profile$"),
	BATTLENET_D3_PROFILE("profile", "^/battleNet/d3/profile/[\\d]*$"),
	BATTLENET_D3_HERO("hero", "^/battleNet/d3/profile/[\\d]*/hero/[\\d]*$"),
	BATTLENET_D3_ITEM("item", "^/battleNet/d3/data/item/[\\d]*$");

	private String name;
	private String pathRegex;

	MenuInfo(String name, String pathRegex) {
		this.name = name;
		this.pathRegex = pathRegex;
	}

	public String getName() {
		return name;
	}

	public String getPathRegex() {
		return pathRegex;
	}

	public static String getName(String servletPath) {
		for (MenuInfo menuInfo : MenuInfo.values()) {
			if (servletPath.matches(menuInfo.getPathRegex())) {
				return menuInfo.name;
			}
		}
		return null;
	}

}
