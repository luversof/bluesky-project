package net.luversof.web.dynamiccrud.setting.domain;

import org.springframework.util.StringUtils;

import net.sf.jsqlparser.expression.operators.relational.LikeExpression;

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

	public static LikeExpression.KeyWord convertJsqlKeyword(DbFieldSearchType value) {
		if (DbFieldSearchType.LIKE_CONTAINS.equals(value)) {
			return LikeExpression.KeyWord.LIKE;
		} else if (DbFieldSearchType.LIKE_RIGHT.equals(value)) {
			return LikeExpression.KeyWord.RLIKE;
		} else {
			return null;
		}
	}
}
