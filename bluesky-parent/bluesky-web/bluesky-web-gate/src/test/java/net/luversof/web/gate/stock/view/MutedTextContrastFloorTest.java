package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 흐린 글자의 알파 바닥.
 *
 * <p>{@code text-base-content/NN} 은 배경 위에 알파로 얹는 색이라 NN 이 곧 대비다. 실측 2026-09-14(캔버스 합성, 자가검사 검정/흰
 * 21:1 통과)로 각 단계의 대비는 이렇다 &mdash; 라이트가 항상 더 나쁘다:
 *
 * <pre>
 *   /30  2.01   /40  2.62   /50  3.55   /55  4.17   /60  4.95   /70  7.12
 * </pre>
 *
 * <p>WCAG AA 는 소형 텍스트에 4.5:1 을 요구한다. <b>/60 이 그 선을 넘는 가장 흐린 단계</b>이고 /55 는 넘지 못한다. 그래서 글자에 쓰는 알파의
 * 바닥을 /60 으로 못박는다.
 *
 * <p>예외는 {@code aria-hidden="true"} 를 단 장식뿐이다 &mdash; 가운뎃점·세로줄 같은 구분 기호는 낭독기가 건너뛰고 뜻도 나르지 않는다.
 *
 * <p>이 검사가 브라우저 없이 성립하는 이유: 값은 배경과 알파만으로 정해지고 주식 화면의 배경은 base-100/base-200 둘뿐이라 위 표가 그대로 적용된다. 브라우저
 * 실측은 {@code ~/.bluesky-qa/contrast-audit3.js} 가 맡는다(수정 후 보이는 미달 0).
 *
 * <p>PoE 화면은 뺀다. 거긴 인게임 색(예: 매직 아이템 {@code #8888ff})을 그대로 쓰기로 한 화면이라 같은 잣대를 들이대면 게임과 달라진다 &mdash;
 * 별개 판단 사항이다.
 */
class MutedTextContrastFloorTest {

  /** 4.5:1 을 넘는 가장 흐린 단계. */
  private static final int FLOOR = 60;

  private static final Pattern MUTED = Pattern.compile("text-base-content/(\\d+)");

  private static final List<String> ROOTS =
      List.of("src/main/jte/stock", "src/main/jte/_components", "src/main/frontend/src/stock");

  private static final List<String> FILES = List.of("src/main/jte/dev.jte");

  private record Hit(String file, int line, int alpha, String text) {}

  /**
   * 주석은 지우고 본다.
   *
   * <p>지난 결정을 적어 둔 주석에 옛 클래스 이름이 그대로 남아 있다(실제로 걸렸다: {@code amountCell.jte} 의 "2026-09-10 까지
   * text-base-content/30 이었는데"). 줄 수는 그대로 둬야 위치를 짚을 수 있으므로 <b>공백으로 덮는다</b>.
   */
  private static String maskComments(String source) {
    char[] chars = source.toCharArray();
    maskBlocks(chars, "<%--", "--%>");
    maskBlocks(chars, "/*", "*/");
    return new String(chars);
  }

  private static void maskBlocks(char[] chars, String open, String close) {
    String text = new String(chars);
    int at = text.indexOf(open);
    while (at >= 0) {
      int end = text.indexOf(close, at + open.length());
      int stop = end < 0 ? chars.length : end + close.length();
      for (int i = at; i < stop; i++) {
        if (chars[i] != (char) 10 && chars[i] != (char) 13) {
          chars[i] = ' ';
        }
      }
      if (end < 0) {
        return;
      }
      at = text.indexOf(open, stop);
    }
  }

  private static List<Hit> scan() throws IOException {
    List<Path> paths = new ArrayList<>();
    for (String root : ROOTS) {
      Path dir = Path.of(root);
      if (!Files.isDirectory(dir)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(dir)) {
        walk.filter(Files::isRegularFile).forEach(paths::add);
      }
    }
    FILES.stream().map(Path::of).filter(Files::isRegularFile).forEach(paths::add);

    List<Hit> hits = new ArrayList<>();
    for (Path path : paths) {
      String[] lines =
          maskComments(Files.readString(path, StandardCharsets.UTF_8))
              .split(String.valueOf((char) 10), -1);
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (line.trim().startsWith("//")) {
          continue;
        }
        Matcher matcher = MUTED.matcher(line);
        while (matcher.find()) {
          hits.add(
              new Hit(
                  path.toString().replace((char) 92, '/'),
                  i + 1,
                  Integer.parseInt(matcher.group(1)),
                  line.trim()));
        }
      }
    }
    return hits;
  }

  /** 훑기가 실제로 무언가를 찾는지 &mdash; 아무것도 못 찾는 검사는 "미달 0" 을 공짜로 낸다. */
  @Test
  void 훑기가_대상을_찾는다() throws IOException {
    List<Hit> hits = scan();

    assertThat(hits).as("주식 화면에 흐린 글자가 하나도 없을 리 없다 - 훑기가 무력하다").hasSizeGreaterThan(100);
    assertThat(hits.stream().map(Hit::file).distinct())
        .as("한 파일만 읽었다면 걷기가 안 된 것이다")
        .hasSizeGreaterThan(10);
    assertThat(hits.stream().filter(hit -> hit.alpha() < FLOOR).toList())
        .as("바닥 미만(장식)이 하나도 안 잡히면 예외 판정이 검사되지 않는다")
        .isNotEmpty();
  }

  /** 바닥 미만은 장식(aria-hidden)뿐이어야 한다. */
  @Test
  void 글자로_읽히는_흐린_색은_바닥_위다() throws IOException {
    List<String> offenders =
        scan().stream()
            .filter(hit -> hit.alpha() < FLOOR)
            .filter(hit -> !hit.text().contains("aria-hidden=\"true\""))
            .map(hit -> hit.file() + ":" + hit.line() + " (/" + hit.alpha() + ") " + hit.text())
            .toList();

    assertThat(offenders)
        .as("/%d 미만은 라이트 테마에서 4.5:1 을 못 넘는다 - 장식이 아니면 /%d 이상으로", FLOOR, FLOOR)
        .isEmpty();
  }
}
