package net.luversof.web.dynamiccrud2.adapter;

/**
 * 대상 DB 의 테이블(Mongo 는 컬렉션) 위치. DB 마다 없는 칸은 null.
 *
 * @param catalog catalog(MySQL 은 DB 이름)
 * @param schema schema
 * @param name 테이블 이름
 */
public record TableRef(String catalog, String schema, String name) {}
