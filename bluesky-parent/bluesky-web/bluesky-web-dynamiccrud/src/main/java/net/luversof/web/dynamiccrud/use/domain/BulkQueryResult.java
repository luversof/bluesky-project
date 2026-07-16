package net.luversof.web.dynamiccrud.use.domain;

import java.util.List;
import java.util.Map;

/**
 * 벌크 쿼리 실행 시 문장(statement) 1개의 실행 결과. type: "select"(rows 채움) | "update"(affectedRows 채움) |
 * "error"(error 채움)
 */
public record BulkQueryResult(
    String sql, String type, List<Map<String, Object>> rows, Integer affectedRows, String error) {}
