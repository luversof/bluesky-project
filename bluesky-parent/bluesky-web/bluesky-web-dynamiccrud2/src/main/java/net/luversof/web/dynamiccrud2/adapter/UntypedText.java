package net.luversof.web.dynamiccrud2.adapter;

/**
 * 타입을 정하지 않고 보낼 글자 값: 컬럼이 표준 문자 타입이 아닐 때(jsonb · inet · enum · interval · 배열 등). 어댑터가 DB 에 맞게 바꾼다 -
 * PostgreSQL 은 타입 없이(Types.OTHER) 보내 서버가 컬럼 타입으로 바꾸게 하고(글자로 보내면 "column is of type X but expression
 * is of type character varying"), 그 밖의 DB 는 글자 그대로.
 */
public record UntypedText(String value) {}
