package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 카드 라벨 · 보조줄은 한글 단어 안에서 줄을 바꾸지 않고, 단어 하나가 카드보다 넓을 때만 그 안에서 끊는다.
 *
 * <p>2026-09-17 종목 상세 카드에 "-7.1% · 보유 원가 대비" 같은 기준 문구를 붙이자(사용자 선택) 좁은 카드에서 "보유 원가 대/비" 로 끊겼다. 한글은
 * 음절 사이에서도 줄을 바꿀 수 있어서다. {@code word-break: keep-all} 은 한글 단어를 띄어쓰기에서만 나눈다. 실측(18 화면 x 5 조합 320/200
 * · 375 · 414 · 768 · 1440): 보조줄 단어 중간 끊김 17 곳 -> 0.
 *
 * <p>같은 날 계좌 상세에도 같은 카드를 넣자 두 가지가 더 보였다(22 화면 x 5 조합). 연평균 카드 라벨이 375 · 414px 에서 "매/수" 로 끊겼고, 기준
 * 문구와 "최초 매수 2025-03-28" 이 붙은 보조줄은 320px + 글꼴 200% 에서 끊을 수 없는 날짜 · 비율이 페이지를 가로로 밀었다. keep-all 만 주면
 * 라벨 넘침이 13 -> 49 로 는다("평가 금액" 조차 카드보다 넓은 폭) - {@code overflow-wrap: anywhere} 를 함께 줘야 넘칠 자리에서만
 * 끊긴다. 결과: 라벨 단어 중간 끊김 63 -> 49(남은 것은 모두 320px + 200%), 라벨 넘침 13 -> 8(아이콘 칩 · 화살표 몫), 문서 넘침 367 ->
 * 322px(계좌 상세 여섯 곳 45 -> 0), 다른 폭은 변화 없음.
 *
 * <p>브라우저가 읽는 것은 산출물이라 원본과 산출물을 함께 보고, 원본은 주석을 지운 뒤 판정한다.
 */
class StatCardSubKeepsKoreanWordsTest {

  private static final List<String> CLASSES = List.of("stat-card-label", "stat-card-sub");

  @Test
  void 원본의_라벨과_보조줄_규칙이_한글_단어를_지키고_넘칠_때만_끊는다() throws IOException {
    String css =
        Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8)
            .replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("\\s+", " ");
    for (String name : CLASSES) {
      assertThat(css)
          .as(name + " - 한글 단어는 띄어쓰기에서만 나눈다")
          .containsPattern(Pattern.compile("\\." + name + " \\{[^}]*word-break: keep-all;"))
          .as(name + " - keep-all 만 두면 좁은 폭에서 넘친다")
          .containsPattern(Pattern.compile("\\." + name + " \\{[^}]*overflow-wrap: anywhere;"));
    }
  }

  @Test
  void 산출물도_같다() throws IOException {
    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);
    for (String name : CLASSES) {
      assertThat(built)
          .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다 (" + name + ")")
          .containsPattern(Pattern.compile("\\." + name + "\\{[^}]*word-break:keep-all"))
          .containsPattern(Pattern.compile("\\." + name + "\\{[^}]*overflow-wrap:anywhere"));
    }
  }
}
