package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간을 바꾼 뒤 "뒤로" 는 들어올 때의 기간으로 한 번 돌아간다 &mdash; 사용자 결정 2026-09-23.
 *
 * <p>예전에는 조건을 주소에 반영할 때 언제나 {@code replaceState} 라 뒤로 한 번에 화면을 떠났다(탭 링크는 히스토리를 쌓아 한 화면에 규칙이 둘이었다).
 * 이제 <b>사람이 기간 선택기를 처음 바꿀 때만</b> 한 칸 쌓고, 나머지는 덮어쓴다. 세 가지가 함께 맞아야 한다:
 *
 * <ol>
 *   <li>한 번만 쌓는다 &mdash; 프리셋을 여러 번 눌러 봐도 화면을 뜨는 데 뒤로를 그만큼 누르지 않게.
 *   <li>사람이 누른 것만 &mdash; 화면을 열 때의 기간 복원도 같은 길을 지나므로 {@code isTrusted} 로 가른다.
 *   <li>뒤로/앞으로 때 다시 그린다 &mdash; 같은 문서라 주소만 바뀌고 표는 마지막 기간 그대로 남는다.
 * </ol>
 */
class RangeHistoryPushOnceTest {

  private static final Path SOURCE = Path.of("src/main/frontend/src/common.ts");

  private static final Path BUILT = Path.of("src/main/resources/static/js/common.js");

  private static String squash(String text) {
    return text.replaceAll("\\s+", "");
  }

  @Test
  void 첫_사용자_변경만_히스토리를_한_칸_쌓는다() throws IOException {
    String source = squash(Files.readString(SOURCE, StandardCharsets.UTF_8));

    assertThat(source)
        .as("한 번만 - 쌓은 뒤에는 덮어써야 한다")
        .contains(squash("if (userChange && !rangeHistoryPushed && nextUrl !=="))
        .contains(squash("rangeHistoryPushed = true;"))
        .contains(squash("globalThis.history.pushState({ gateSyncUrl: true }, \"\", nextUrl);"));
    assertThat(source)
        .as("사람이 기간 선택기를 누른 것만 - 스크립트가 흉내 낸 클릭(기간 복원)은 아니다")
        .contains(squash("if (ev.isTrusted && target?.closest?.(\"[data-picker]\")) {"))
        .contains(
            squash(
                "if (xhr) xhr[RANGE_USER_CHANGE_FLAG] = Date.now() - rangePickerTouchedAt < RANGE_PICKER_INTENT_MS;"));
    // 받을 때가 아니라 보낼 때 잰다 - 실측 2026-09-23: 응답을 9 초 늦추자 받을 때 재는 창(8 초)을 넘겨 첫 변경이 덮어쓰기로 끝났다.
    assertThat(source)
        .as("보낼 때 적은 표시로 가른다")
        // 파일에 htmx:beforeRequest 듣는 곳이 여럿이다 - 표시를 적는 줄과 한 덩이로 본다(첫 줄만 보면 다른 이벤트로 옮겨도 통과했다).
        .contains(squash("document.addEventListener(\"htmx:beforeRequest\", (event: any) => { const xhr = event.detail?.xhr; if (xhr) xhr[RANGE_USER_CHANGE_FLAG] = Date.now() - rangePickerTouchedAt < RANGE_PICKER_INTENT_MS;"))
        .contains(squash("const userChange = event.detail.xhr?.[RANGE_USER_CHANGE_FLAG] === true;"))
        .doesNotContain(squash("const userChange = Date.now() - rangePickerTouchedAt"));
  }

  @Test
  void 뒤로_앞으로_때는_주소대로_다시_그린다() throws IOException {
    assertThat(squash(Files.readString(SOURCE, StandardCharsets.UTF_8)))
        .as("같은 문서라 다시 부르지 않으면 주소만 들어올 때 기간이고 표는 마지막 기간 그대로다")
        .contains(squash("globalThis.addEventListener(\"popstate\", () => {"))
        .contains(squash("if (rangeHistoryPushed) { globalThis.location.reload(); }"));
  }

  @Test
  void 배포본에도_들어_있다() throws IOException {
    // 메이븐은 프론트엔드를 빌드하지 않는다 - 커밋된 산출물이 배포본이다.
    assertThat(Files.readString(BUILT, StandardCharsets.UTF_8))
        .contains("pushState({gateSyncUrl:!0},\"\",nextUrl)")
        .contains("popstate\",()=>{rangeHistoryPushed&&globalThis.location.reload()")
        .contains("isTrusted&&")
        .contains("[RANGE_USER_CHANGE_FLAG])===!0&&!rangeHistoryPushed")
        .contains("xhr[RANGE_USER_CHANGE_FLAG]=Date.now()-rangePickerTouchedAt<RANGE_PICKER_INTENT_MS");
  }
}
