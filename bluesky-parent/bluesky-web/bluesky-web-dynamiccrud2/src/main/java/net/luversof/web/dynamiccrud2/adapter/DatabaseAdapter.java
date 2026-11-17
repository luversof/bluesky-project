package net.luversof.web.dynamiccrud2.adapter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * 대상 DB 한 연결에 대한 작업. DB 종류(JDBC 3종, 2차에 Mongo)마다 구현이 하나다.
 *
 * <p>쿼리는 DB 종류의 원문(SQL · Mongo JSON) 그대로 주고받고, 파라미터는 이름 붙은 {@code :name} 으로 쓴다.
 */
public interface DatabaseAdapter {

  /** 고를 수 있는 테이블(컬렉션) 목록 */
  List<TableRef> listTables();

  /** 컬럼 · PK · unique · 인덱스 정보 */
  TableMeta readTable(TableRef ref);

  /** 기본 쿼리 생성(조회는 메뉴 등록 때, 입력 · 수정 · 삭제는 작업 추가 때) */
  String generate(QueryKind kind, TableMeta table);

  /** 저장 전 검사. 문제가 있으면 BlueskyException */
  void validate(String query, QueryKind kind);

  /** 쿼리가 쓰는 파라미터 이름(나온 순서) */
  List<String> parameterNames(String query);

  /** 조회 쿼리 분석(검색 조건 · 결과 컬럼) */
  ReadAnalysis analyzeRead(String query);

  /**
   * 한 페이지 조회.
   *
   * @param params 바인딩 값(검색 조건 등)
   * @param emptyOptionalParams 값이 비어 조건을 뺄 선택 파라미터
   * @param sort 정렬(null 이면 쿼리 그대로)
   * @param page 0 부터
   * @param size 한 페이지 행 수(설정 한도로 자른다)
   */
  ReadResult read(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      Sort sort,
      int page,
      int size);

  /** 엑셀 내려받기용 전체 조회(첫 행부터 설정 한도 excelMaxRows 까지, 건수는 세지 않아 total = -1) */
  ReadResult export(
      String query, Map<String, Object> params, Set<String> emptyOptionalParams, Sort sort);

  /** 입력 · 수정 · 삭제를 행마다 실행. 전부 한 트랜잭션, 행마다 영향 행 수를 돌려준다 */
  int[] modify(String query, List<Map<String, Object>> rows);

  /**
   * 정렬.
   *
   * @param label 결과 컬럼 이름. 호출하는 쪽이 정렬 허용 컬럼인지 먼저 확인한다
   * @param descending 내림차순
   */
  record Sort(String label, boolean descending) {}

  /**
   * 조회 결과. 결과 이름이 겹칠 수 있어(a.id, u.id) 행을 맵이 아닌 값 목록으로 준다.
   *
   * @param labels 결과 컬럼 이름(순서대로)
   * @param rows 행마다 labels 순서의 값
   * @param total 전체 건수
   */
  record ReadResult(List<String> labels, List<List<Object>> rows, long total) {}
}
