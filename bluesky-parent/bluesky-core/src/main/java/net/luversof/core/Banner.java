package net.luversof.core;


public class Banner {

	private static final String[] BANNER = {
		"",
		" ____  _                 _            ____            _           _   ",
		"| __ )| |_   _  ___  ___| | ___   _  |  _ \\ _ __ ___ (_) ___  ___| |_ ",
		"|  _ \\| | | | |/ _ \\/ __| |/ / | | | | |_) | '__/ _ \\| |/ _ \\/ __| __|",
		"| |_) | | |_| |  __/\\__ \\   <| |_| | |  __/| | | (_) | |  __/ (__| |_ ",
		"|____/|_|\\__,_|\\___||___/_|\\_\\\\__, | |_|   |_|  \\___// |\\___|\\___|\\__|",
		"                              |___/                |__/               "
	};

	private static final int STRAP_LINE_SIZE = 42;

	/**
	 * Write the banner to the specified print stream.
	 * 
	 * @param printStream
	 *            the output print stream
	 */
	public static void write(Object object) {
		String name = object.getClass().getPackage().getName();
		for (String line : BANNER) {
			System.out.println(line);
		}
		String version = Banner.class.getPackage().getImplementationVersion();
		version = (version == null ? "" : " (v" + version + ")");
		String padding = "";
		while (padding.length() < STRAP_LINE_SIZE - (version.length() + name.length())) {
			padding += " ";
		}
		System.out.println(new StringBuilder().append(":: ").append(name).append(" ::").append(padding).append(version).toString());
		System.out.println();
	}
}
