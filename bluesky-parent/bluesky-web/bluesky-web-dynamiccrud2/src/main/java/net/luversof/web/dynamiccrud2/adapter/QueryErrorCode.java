package net.luversof.web.dynamiccrud2.adapter;

import io.github.luversof.boot.exception.BlueskyException;

/**
 * 쿼리 저장 · 실행 검사 오류 코드(BlueskyException 으로 던진다). 화면 문구는 .properties 대신 여기 둔다(한글이 편집기 코드페이지로 깨지는 문제를
 * 피한다).
 */
public enum QueryErrorCode {
  EMPTY_QUERY("쿼리가 비어 있습니다."),
  MULTIPLE_STATEMENTS("한 쿼리에는 문장을 하나만 쓸 수 있습니다(; 로 나눈 여러 문장 불가)."),
  UNSUPPORTED_STATEMENT("SELECT · WITH · INSERT · UPDATE · DELETE 만 쓸 수 있습니다(받은 것: %s)."),
  STATEMENT_KIND_MISMATCH("%s 작업에 %s 문장을 저장할 수 없습니다."),
  UNSUPPORTED_PARAMETER_FORM("파라미터는 :name 형식만 씁니다. :{name} 과 &name(공백 없는 a&b 포함)은 쓸 수 없습니다."),
  NO_PARAMETER_IN_MODIFY(
      "WHERE 조건에 파라미터가 없는 UPDATE · DELETE 는 전체 행을 바꿀 수 있어 저장할 수 없습니다(예: WHERE id = :id)."),
  NO_ROW_KEY("%s 테이블에 PK · unique 키가 없어 기본 쿼리를 만들 수 없습니다. 직접 작성하세요."),
  UNSUPPORTED_DATABASE("지원하지 않는 DB 종류입니다: %s"),
  CONNECTION_NOT_FOUND("connection 을 찾을 수 없습니다: %s"),
  HAS_CHILDREN("하위 항목이 있어 삭제할 수 없습니다. 하위 항목을 먼저 지우세요."),
  INVALID_CODE("주소 이름(code)은 영문 소문자 · 숫자 · - 로 1~50자, 첫 글자는 영문 소문자나 숫자여야 합니다: %s"),
  RESERVED_CODE("%s 는 앱이 쓰는 주소라 프로젝트 주소 이름으로 쓸 수 없습니다."),
  DUPLICATE_CODE("주소 이름 %s 는 같은 곳에 이미 있습니다."),
  READ_QUERY_REQUIRED("조회 작업은 메뉴마다 하나 있어야 해서 지울 수 없습니다."),
  NOT_OPTIONAL_CAPABLE("%s 조건은 OR · 서브쿼리 안에 있거나 해석할 수 없는 쿼리라 선택으로 바꿀 수 없습니다.");

  private final String message;

  QueryErrorCode(String message) {
    this.message = message;
  }

  /** 화면에 보일 문구. 이 enum 의 오류가 아니면 null */
  public static String messageOf(BlueskyException e) {
    var code = e.getErrorCode();
    var prefix = QueryErrorCode.class.getSimpleName() + ".";
    if (code == null || !code.startsWith(prefix)) {
      return null;
    }
    var args = e.getErrorMessageArgs() == null ? new Object[0] : e.getErrorMessageArgs();
    return String.format(valueOf(code.substring(prefix.length())).message, args);
  }
}
