package net.luversof.web.gate.poe;

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
 * PoE 화면의 흐린 보조 글자 알파 바닥(10-02) — 주식 화면 {@code MutedTextContrastFloorTest} 와 같은 잣대(/60 = 4.5:1 을 넘는
 * 가장 흐린 단계).
 *
 * <p>주식 쪽 시험은 "PoE 는 인게임 색을 쓰므로 별개 판단"이라 PoE 를 뺐다. 여기서 보는 건 인게임 색이 아니라 화면 장식 글자({@code
 * text-base-content/NN})뿐이다 — 고유 이름 주황 · 마법 파랑 같은 게임 색은 건드리지 않는다. 브라우저
 * 실측(~/.bluesky-qa/contrast-poe.js, 10-02 다크): /40 3.37:1 · /50 4.3:1 로 미달이던 314곳을 /60 으로 올렸다. 예외는
 * {@code aria-hidden="true"} 장식뿐.
 */
class PoeMutedTextContrastFloorTest {

  private static final int FLOOR = 60;

  private static final Pattern MUTED = Pattern.compile("text-base-content/(\\d+)");

  private static final List<String> ROOTS =
      List.of(
          "src/main/jte/poe",
          "src/main/jte/poe2",
          "src/main/frontend/src/poe",
          "src/main/frontend/src/poe2");

  private record Hit(String file, int line, int alpha, String text) {}

  /** 주석은 공백으로 덮는다(옛 클래스 이름을 적어 둔 주석이 걸리지 않게, 줄 번호는 유지). */
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

  /** 훑기가 실제로 무언가를 찾는지 — 아무것도 못 찾는 검사는 "미달 0" 을 공짜로 낸다. */
  @Test
  void 훑기가_대상을_찾는다() throws IOException {
    List<Hit> hits = scan();
    assertThat(hits).as("PoE 화면에 흐린 글자가 하나도 없을 리 없다 - 훑기가 무력하다").hasSizeGreaterThan(100);
    assertThat(hits.stream().map(Hit::file).distinct())
        .as("한 파일만 읽었다면 걷기가 안 된 것이다")
        .hasSizeGreaterThan(10);
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
    assertThat(offenders).as("/%d 미만은 4.5:1 을 못 넘는다 - 장식이 아니면 /%d 이상으로", FLOOR, FLOOR).isEmpty();
  }
}
