package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 가로 스크롤 래퍼는 안쪽 절대 배치의 기준이어야 한다.
 *
 * <p>{@code .sr-only} 는 폭 1px 의 <b>절대 배치</b>다. 래퍼가 {@code position: static} 이면 이 요소들의 기준이 래퍼가 아니라
 * 바깥의 positioned 조상이 되어, 표 안쪽 좌표 그대로 화면 밖에 놓이고 <b>문서 폭</b>을 넓힌다.
 *
 * <p>실측 2026-09-11(뷰포트 375px · 320px 모두 같은 값): 배당 내역 389px · 매매 내역 638px 로 페이지 전체가 가로로 스크롤됐다. 절대 배치
 * 요소를 전부 숨기면 정확히 375px 로 돌아왔고, 스크롤 래퍼 안의 절대 배치는 {@code .sr-only} 60 개뿐이었다(드롭다운 등 없음). 표 자체는 이미 래퍼
 * 안에서 정상적으로 잘리고 있었다 &mdash; 새어 나간 것은 까닭 설명뿐이다.
 */
class ScrollWrapperContainingBlockTest {

  // 스스로 떠 있는 요소는 뺀다(2026-10-02) - 이 규칙은 레이어 밖이라 .absolute 를 이겨, 상세 검색 선택 상자 패널(absolute +
  // overflow-auto)이 흐름에 끼고 260px 아래로 떠서 열렸다.
  private static final String RULE =
      ".overflow-x-auto, .overflow-auto):not(.absolute, .fixed, .sticky) { position: relative; }";

  @Test
  void 소스_css_에_규칙이_있다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);

    assertThat(css).as("가로 스크롤 래퍼를 포함 블록으로 두는 규칙").contains(RULE);
  }

  /** 메이븐은 프론트엔드를 빌드하지 않는다 - 배포본은 커밋된 산출물이라 함께 확인한다. */
  @Test
  void 빌드된_css_에도_규칙이_있다() throws IOException {
    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);

    assertThat(built)
        .as("npm run build 를 돌리지 않으면 배포본에는 규칙이 없다")
        .contains(
            ":is(.overflow-x-auto,.overflow-auto):not(.absolute,.fixed,.sticky){position:relative}");
  }

  /** 인쇄 규칙은 래퍼의 overflow 를 풀기 때문에 이 규칙과 충돌하지 않아야 한다. */
  @Test
  void 인쇄에서는_여전히_스크롤을_푼다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);

    assertThat(css).contains("overflow: visible !important;");
  }
}
