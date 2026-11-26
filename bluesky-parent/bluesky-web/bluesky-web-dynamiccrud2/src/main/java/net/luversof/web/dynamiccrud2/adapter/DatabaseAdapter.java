package net.luversof.web.dynamiccrud2.adapter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * 대상 DB 한 연결에 대한 작업. DB 종류(JDBC 3종, 2차에 Mongo)마다 구현이 하나다.
 *
 * <p>쿼리는 DB 종류의 원문(SQL · Mongo JSON) 그대로 주고받고, 파라미터는 이름 붙은 {@code :name} 으로 쓴다.
 */
public interface DatabaseAdapter {

  /** 화면에 보일 DB 종류 이름(PostgreSQL · MySQL · MariaDB · SQL Server, 2차에 MongoDB) */
  String databaseType();

  /** 데이터를 담는 단위의 이름(화면 문구: "테이블 필드 관리" · "테이블 인덱스"). RDB 는 테이블, 2차 Mongo 어댑터는 "컬렉션" 을 돌려준다 */
  default String targetNoun() {
    return Msg.t("target.table");
  }

  /** 고를 수 있는 테이블(컬렉션) 목록 */
  List<TableRef> listTables();

  /** 컬럼 · PK · unique · 인덱스 정보 */
  TableMeta readTable(TableRef ref);

  /** 기본 쿼리 생성(조회는 메뉴 등록 때, 입력 · 수정 · 삭제는 작업 추가 때) */
  String generate(QueryKind kind, TableMeta table);

  /** 저장 전 검사. 문제가 있으면 BlueskyException */
  void validate(String query, QueryKind kind);

  /**
   * 입력 · 수정 · 삭제 쿼리를 실행하지 않고 DB 에서 검사한다(파라미터는 모두 NULL). 문법 · 없는 테이블 · 컬럼이면 BadSqlGrammarException.
   */
  void check(String query);

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
  default ReadResult read(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      Sort sort,
      int page,
      int size) {
    return read(query, params, emptyOptionalParams, List.of(), sort, page, size);
  }

  /**
   * 한 페이지 조회 + 화면 검색 조건.
   *
   * @param filters 덧붙일 검색 조건(값은 params 에 같이 넣는다)
   * @throws IllegalArgumentException 검색 조건을 붙일 수 없는 쿼리
   */
  ReadResult read(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      int page,
      int size);

  /** 엑셀 내려받기용 전체 조회(첫 행부터 설정 한도 excelMaxRows 까지). 행을 모으지 않고 읽는 대로 sink 에 넘긴다 */
  void export(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      RowSink sink);

  /**
   * 행 키로 메뉴 테이블의 지금 값을 읽는다(동시 수정 확인용).
   *
   * @param columns 읽을 컬럼
   * @param keys 행 키 컬럼 → 값
   * @return columns 순서의 값, 행이 없거나 하나가 아니면 null
   */
  List<Object> currentRow(TableRef table, List<String> columns, Map<String, Object> keys);

  /**
   * 검색 셀렉트(검색 설정 "DB 값 전체")용 컬럼의 서로 다른 값. NULL 은 빼고 정렬한다. 읽기 전용으로 읽는다.
   *
   * @param limit 보일 최대 개수 - 이보다 많으면 limit + 1 개를 돌려준다(부른 쪽이 넘침을 안다)
   */
  List<Object> distinctValues(TableRef table, String column, int limit);

  /** 조회 결과를 한 행씩 받는다. labels 가 먼저 한 번 온다 */
  interface RowSink {
    void labels(List<String> labels);

    void row(List<Object> values);
  }

  /** 쿼리 화면 시험 실행 · 미리 보기: 첫 행부터 size 행, 건수는 세지 않는다(total = -1, 큰 테이블에서 COUNT 를 피하려고) */
  ReadResult sample(
      String query, Map<String, Object> params, Set<String> emptyOptionalParams, int size);

  /** 입력 · 수정 · 삭제를 행마다 실행. 전부 한 트랜잭션, 행마다 영향 행 수를 돌려준다 */
  int[] modify(String query, List<Map<String, Object>> rows);

  /**
   * 행을 그 테이블에 그대로 넣는다(지운 행 되돌리기). 행마다 컬럼 → 값, 전부 한 트랜잭션. 자동 증가 컬럼에 값을 넣지 못하는 DB(MSSQL IDENTITY ·
   * PostgreSQL GENERATED ALWAYS)는 실패한다 - 사유는 DB 오류 그대로.
   */
  int[] insertRows(TableRef table, List<Map<String, Object>> rows);

  /**
   * 실행 계획(쿼리 화면 "실행 계획"). 실행하지 않는다(ANALYZE 없음).
   *
   * @param lines 계획 줄
   * @param approximate 파라미터를 NULL 로 넣어 본 계획이라 실제(값이 있을 때)와 다를 수 있다
   */
  record Explain(List<String> lines, boolean approximate) {}

  /**
   * @throws UnsupportedOperationException 이 DB 는 실행 계획을 보이지 않는다
   */
  Explain explain(String query);

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
