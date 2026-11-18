package net.luversof.web.dynamiccrud2.adapter;

import java.util.List;
import java.util.Set;

/**
 * DB 에서 읽은 테이블 정보. 저장하지 않고 쓸 때마다 읽는다(필드 메타만 DbField 로 저장).
 *
 * @param ref 위치
 * @param columns 컬럼(순서대로)
 * @param primaryKey PK 컬럼(순서대로, 없으면 빈 목록)
 * @param uniqueKeys PK 가 아닌 unique 키들(부분 · 식 인덱스 제외)
 * @param indexLeadingColumns 어떤 인덱스의 첫 컬럼(정렬 허용 초기값)
 * @param indexes 인덱스 전체(메뉴 설정 화면 인덱스 표 · 검색칸 인덱스 경고)
 */
public record TableMeta(
    TableRef ref,
    List<ColumnMeta> columns,
    List<String> primaryKey,
    List<List<String>> uniqueKeys,
    Set<String> indexLeadingColumns,
    List<IndexInfo> indexes) {

  /** 인덱스 목록 없이 만든다(인덱스를 따로 읽지 않는 DB · 테스트) */
  public TableMeta(
      TableRef ref,
      List<ColumnMeta> columns,
      List<String> primaryKey,
      List<List<String>> uniqueKeys,
      Set<String> indexLeadingColumns) {
    this(ref, columns, primaryKey, uniqueKeys, indexLeadingColumns, List.of());
  }

  /**
   * 인덱스 하나.
   *
   * @param name 인덱스 이름
   * @param columns 컬럼(순서대로). 식 인덱스의 식은 DB 가 알려 준 글자 그대로(모르면 "(식)")
   * @param primary PK 인덱스
   * @param unique 값이 겹치지 않는 인덱스
   * @param partial 조건이 붙은 부분 인덱스(WHERE ...) - 조건에 맞는 행만 담는다
   * @param expression 컬럼이 아닌 식이 든 인덱스
   */
  public record IndexInfo(
      String name,
      List<String> columns,
      boolean primary,
      boolean unique,
      boolean partial,
      boolean expression) {}

  /** 자동 생성 UPDATE · DELETE 의 행 키: PK, 없으면 첫 unique 키, 둘 다 없으면 빈 목록 */
  public List<String> rowKey() {
    if (!primaryKey.isEmpty()) {
      return primaryKey;
    }
    return uniqueKeys.isEmpty() ? List.of() : uniqueKeys.get(0);
  }

  /** 행을 가리키는 컬럼(PK · unique 키에 든 것) - 수정 불가 초기값 */
  public boolean isKeyColumn(String column) {
    return primaryKey.contains(column) || uniqueKeys.stream().anyMatch(key -> key.contains(column));
  }
}
