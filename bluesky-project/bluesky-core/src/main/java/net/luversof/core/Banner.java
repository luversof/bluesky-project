package net.luversof.core;

import java.io.PrintStream;

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
	public static void write(PrintStream printStream, String module) {
		for (String line : BANNER) {
			printStream.println(line);
		}
		String version = Banner.class.getPackage().getImplementationVersion();
		version = (version == null ? "" : " (v" + version + ")");
		String padding = "";
		while (padding.length() < STRAP_LINE_SIZE - (version.length() + module.length())) {
			padding += " ";
		}
		printStream.println(new StringBuilder().append(":: ").append(module).append(" ::").append(padding).append(version).toString());
		printStream.println();
	}
}
