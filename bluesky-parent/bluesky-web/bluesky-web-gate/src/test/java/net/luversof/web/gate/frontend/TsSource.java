package net.luversof.web.gate.frontend;

/**
 * 프런트엔드 TS 원본을 보는 가드의 비교 모양.
 *
 * <p>사용자는 TS 를 prettier(watch:format)로 정리한다. 실측 2026-09-23: 복사본의 TS 20 개에 prettier 를 돌리자 원본 TS 를 보던
 * 가드 9 개가 깨졌다 - 줄 접힘, 작은따옴표 &rarr; 큰따옴표, 줄을 접으며 붙인 끝 쉼표 때문이다. 원본과 기대 문자열 양쪽에 이 함수를 똑같이 씌운다.
 */
public final class TsSource {

  private TsSource() {}

  /** 공백을 전부 지우고, 작은따옴표를 큰따옴표로, 닫는 괄호 앞 끝 쉼표를 지운다. */
  public static String n(String text) {
    return text.replaceAll("\\s+", "").replace('\'', '"').replaceAll(",([)\\]}])", "$1");
  }
}
