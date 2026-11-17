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
 */
public record TableMeta(
    TableRef ref,
    List<ColumnMeta> columns,
    List<String> primaryKey,
    List<List<String>> uniqueKeys,
    Set<String> indexLeadingColumns) {

  /** 자동 생성 UPDATE · DELETE 의 행 키: PK, 없으면 첫 unique 키, 둘 다 없으면 빈 목록 */
  public List<String> rowKey() {
    if (!primaryKey.isEmpty()) {
      return primaryKey;
    }
    return uniqueKeys.isEmpty() ? List.of() : uniqueKeys.get(0);
  }
}
