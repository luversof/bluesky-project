package net.luversof.web.dynamiccrud2.adapter;

import java.util.List;

/**
 * 조회 쿼리 분석 결과. 쿼리 등록 화면이 아래 컬럼 목록 · 검색 조건 설정을 그릴 때 쓴다.
 *
 * @param parsed 구조를 해석했는지. false 면 검색 조건은 모두 필수로 고정되고 페이징은 감싸기로 대신한다
 * @param parseError 해석 실패 사유(parsed=false 일 때)
 * @param conditions WHERE 의 파라미터 조건
 * @param columns SELECT 결과 컬럼
 * @param hasOwnPaging 쿼리에 이미 LIMIT · TOP · FETCH 또는 :_limit 이 있다
 * @param hasOrderBy 최상위 ORDER BY 가 있다
 */
public record ReadAnalysis(
    boolean parsed,
    String parseError,
    List<SearchCondition> conditions,
    List<ResultColumn> columns,
    boolean hasOwnPaging,
    boolean hasOrderBy) {

  /**
   * WHERE 에 쓰인 파라미터 조건.
   *
   * @param paramName 파라미터 이름
   * @param expression 조건 원문(예: a.status = :status)
   * @param optionalCapable 선택(값이 비면 조건 제거)으로 바꿀 수 있는지. 최상위 AND 의 단순 조건만 true
   */
  public record SearchCondition(String paramName, String expression, boolean optionalCapable) {}

  /**
   * SELECT 결과 컬럼.
   *
   * @param label 결과 이름(별칭이 있으면 별칭)
   * @param alias 별칭이 있을 때만 값
   * @param tableSchema 원래 테이블 스키마(알 때만)
   * @param tableName 원래 테이블(알 때만)
   * @param columnName 원래 컬럼(알 때만)
   * @param kind 결과 종류
   */
  public record ResultColumn(
      String label,
      String alias,
      String tableSchema,
      String tableName,
      String columnName,
      Kind kind) {

    /** COLUMN = 실제 컬럼, EXPRESSION = 계산 · 함수, ALL = *, ALL_TABLE = a.* */
    public enum Kind {
      COLUMN,
      EXPRESSION,
      ALL,
      ALL_TABLE
    }
  }
}
