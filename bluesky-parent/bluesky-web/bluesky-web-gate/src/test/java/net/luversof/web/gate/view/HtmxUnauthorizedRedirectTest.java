package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 레이아웃의 htmx 오류 처리기: 401 은 로그인으로, 동작(비 GET) 실패는 동작 실패 문구로.
 *
 * <p>실측 2026-09-09: 관리 화면의 갱신 버튼({@code hx-post}, 성공 시 새로고침)이 세션 만료로 401 을 받으면 "데이터 로드 중 오류가
 * 발생했습니다" 토스트만 뜨고 갈 길이 없었고, 500 이어도 같은 "로드" 문구였다. 페이지(리다이렉트)·조각(로그인 링크)·JSON(errorHandler) 과 같은
 * {@code /login?redirectUrl=} 규약으로 맞추고, 저장·갱신 실패는 전용 문구를 쓴다. 인라인 스크립트라 node 로 못 돌리므로 소스로 못박고, 동작은
 * Playwright 로 확인했다.
 */
class HtmxUnauthorizedRedirectTest {

  private static String layout() throws IOException {
    return Files.readString(
        Path.of("src/main/jte/_layout/defaultLayout.jte"), StandardCharsets.UTF_8);
  }

  private static String showLoadErrorBody(String layout) {
    int fn = layout.indexOf("function showLoadError(evt)");
    assertThat(fn).as("오류 처리기가 이벤트를 받는다").isPositive();
    return layout.substring(fn, layout.indexOf("\n\t\t}\n", fn));
  }

  @Test
  void 레이아웃_오류_처리기는_401_이면_로그인으로_보낸다() throws IOException {
    String layout = layout();
    assertThat(showLoadErrorBody(layout))
        .contains("xhr.status === 401")
        .contains("location.assign('/login?redirectUrl=' + encodeURIComponent(location.href))");
    assertThat(layout)
        .contains("addEventListener('htmx:responseError', showLoadError)")
        .contains("addEventListener('htmx:sendError', showLoadError)");
  }

  @Test
  void 동작_실패는_로드_문구가_아니라_동작_실패_문구다() throws IOException {
    String layout = layout();
    String body = showLoadErrorBody(layout);
    assertThat(body)
        .contains("requestConfig.verb")
        .contains("cfgMessage('commonMessageActionError'")
        .contains("cfgMessage('commonMessageLoadError'");
    assertThat(layout)
        .as("app-config 가 문구를 실어야 스크립트가 읽는다")
        .contains(
            "data-common-message-action-error=\"${MessageUtil.getMessage(\"common.message.action.error\")}\"");
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      assertThat(Files.readString(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("common.message.action.error");
    }
  }
}
