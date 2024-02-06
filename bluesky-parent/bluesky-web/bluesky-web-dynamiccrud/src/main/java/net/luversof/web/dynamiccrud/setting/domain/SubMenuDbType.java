package net.luversof.web.dynamiccrud.setting.domain;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SubMenuDbType {
	MySql,
	MsSql,
	Mongo;
	
	public static String toPresetString() {
		return String.join("|", Stream.of(SubMenuDbType.values()).map(SubMenuDbType::name).collect(Collectors.toList()));
	}
}
