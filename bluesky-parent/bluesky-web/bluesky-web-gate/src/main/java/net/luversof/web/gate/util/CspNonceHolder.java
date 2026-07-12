package net.luversof.web.gate.util;

/**
 * 요청 단위 CSP nonce 를 JTE 템플릿에서 정적 호출로 읽기 위한 홀더. MessageUtil 과 같은 정적 접근 패턴을 따른다. CspNonceFilter 가 요청
 * 시작 시 set, 종료 시 clear 한다.
 */
public final class CspNonceHolder {

  private static final ThreadLocal<String> NONCE = new ThreadLocal<>();

  private CspNonceHolder() {}

  public static void set(String nonce) {
    NONCE.set(nonce);
  }

  /** 필터를 거치지 않은 렌더링(테스트 등)에서도 안전하도록 미설정 시 빈 문자열을 반환한다. */
  public static String getNonce() {
    String nonce = NONCE.get();
    return nonce != null ? nonce : "";
  }

  public static void clear() {
    NONCE.remove();
  }
}
