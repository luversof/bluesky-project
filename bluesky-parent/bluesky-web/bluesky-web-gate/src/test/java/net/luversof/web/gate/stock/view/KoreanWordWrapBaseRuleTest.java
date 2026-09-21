package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 설명문 · 제목 · 단추 · 배지의 한글 단어는 띄어쓰기에서만 줄을 바꾸고, 단어 하나가 자리보다 넓을 때만 그 안에서 끊는다. 표 칸은 뺀다.
 *
 * <p>사용자 선택 2026-09-17. 기본 글꼴 375px 에서 한글 단어 안 끊김 706 곳 중 661 곳이 표 안, 45 곳이 표 밖(설명문 p 28 · 목록 li 12
 * 등)이었다. 실측(13 화면, 브라우저 기본 글꼴 설정) 표 밖 끊김 - 기본 글꼴 320 · 375 · 414 · 768 · 1440px: 52 · 45 · 39 · 14
 * · 15 -> 7 · 4 · 5 · 0 · 1, 글꼴 200%: 180 · 119 · 97 · 34 · 11 -> 103 · 37 · 25 · 3 · 0. 표 안 끊김 ·
 * 세로 글자 · 문서 넘침은 그대로, 글자 상자 넘침은 줄거나 그대로.
 *
 * <p>keep-all 만 주면 좁은 자리에서 넘친다(카드 라벨 실측 13 -> 49) - overflow-wrap: anywhere 를 함께 준다. {@code :where}
 * 로 우선순위 0 · base 층이라 컴포넌트 · 유틸리티 규칙이 이긴다 &mdash; 층 밖으로 나가면 break-all 같은 유틸리티까지 덮는다. 표를 넣으면 열이 넓어져
 * 상자 안 스크롤이 늘 수 있어 사용자가 뺐다.
 */
class KoreanWordWrapBaseRuleTest {

  private static final String SELECTOR_SOURCE =
      ":where(p, h1, h2, h3, h4, h5, h6, li, label, .label-text, .btn, .badge):not(:where(table *))";

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 원본_규칙이_base_층_안에_있고_표를_뺀다() throws IOException {
    String css =
        read("src/main/frontend/main.css")
            .replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("\\s+", " ");
    int base = css.indexOf("@layer base {");
    int components = css.indexOf("@layer components {");
    int rule = css.indexOf(SELECTOR_SOURCE + " { word-break: keep-all; overflow-wrap: anywhere; }");
    assertThat(base).as("base 층을 못 찾았다 - 검사 전제").isGreaterThan(0);
    assertThat(components).as("components 층을 못 찾았다 - 검사 전제").isGreaterThan(base);
    assertThat(rule).as("설명문 · 제목 · 단추 · 배지 단어 단위 줄바꿈 규칙이 없다(표 제외 포함)").isGreaterThan(0);
    assertThat(rule)
        .as("base 층 안이어야 컴포넌트 · 유틸리티 규칙이 이긴다")
        .isGreaterThan(base)
        .isLessThan(components);
  }

  @Test
  void 산출물도_base_층_안에_있다() throws IOException {
    String built = read("src/main/resources/static/main.css");
    int base = built.indexOf("@layer base{");
    int components = built.indexOf("@layer components{");
    int rule =
        built.indexOf(
            ":where(p,h1,h2,h3,h4,h5,h6,li,label,.label-text,.btn,.badge):not(:where(table *)){word-break:keep-all;overflow-wrap:anywhere}");
    assertThat(rule).as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다").isGreaterThan(0);
    assertThat(base).isGreaterThan(0);
    assertThat(rule).isGreaterThan(base).isLessThan(components);
  }
}
