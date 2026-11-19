package net.luversof.web.dynamiccrud2.adapter;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.config.Msg;

/**
 * 쿼리 저장 · 실행 검사 오류 코드(BlueskyException 으로 던진다). 화면 문구는 다국어 메시지 {@code error.query.<이름>}
 * (messages.properties · messages_en.properties, UTF-8 - 깨짐은 MessagesPropertiesTest 가 막는다). 인자는 {0}
 * · {1}.
 */
public enum QueryErrorCode {
  EMPTY_QUERY,
  MULTIPLE_STATEMENTS,
  UNSUPPORTED_STATEMENT,
  STATEMENT_KIND_MISMATCH,
  UNSUPPORTED_PARAMETER_FORM,
  NO_PARAMETER_IN_MODIFY,
  NO_ROW_KEY,
  UNSUPPORTED_DATABASE,
  CONNECTION_NOT_FOUND,
  QUERY_CHECK_FAILED,
  HAS_CHILDREN,
  INVALID_CODE,
  RESERVED_CODE,
  DUPLICATE_CODE,
  READ_QUERY_REQUIRED,
  NOT_OPTIONAL_CAPABLE;

  /** 화면에 보일 문구(지금 요청 언어). 이 enum 의 오류가 아니면 null */
  public static String messageOf(BlueskyException e) {
    var code = e.getErrorCode();
    var prefix = QueryErrorCode.class.getSimpleName() + ".";
    if (code == null || !code.startsWith(prefix)) {
      return null;
    }
    var args = e.getErrorMessageArgs() == null ? new Object[0] : e.getErrorMessageArgs();
    // 인자는 문자열로(숫자 · enum 을 MessageFormat 이 꾸미지 않게)
    var texts = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      texts[i] = String.valueOf(args[i]);
    }
    return Msg.t("error.query." + valueOf(code.substring(prefix.length())).name(), texts);
  }
}
