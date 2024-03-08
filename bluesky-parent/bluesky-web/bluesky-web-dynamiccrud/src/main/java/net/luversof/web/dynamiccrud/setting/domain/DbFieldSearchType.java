package net.luversof.web.dynamiccrud.setting.domain;

import org.springframework.util.StringUtils;

public enum DbFieldSearchType {
	EQUALS,
	LIKE_RIGHT,
	LIKE_CONTAINS,
	MINOR_THAN,
	GREATER_THAN;

	public static DbFieldSearchType convertValue(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		} else {
			return valueOf(value.toUpperCase());
		}
	}

}
