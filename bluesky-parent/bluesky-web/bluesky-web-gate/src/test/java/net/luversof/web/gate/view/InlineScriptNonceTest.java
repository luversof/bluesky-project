package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 인라인 {@code <script>} 에 CSP nonce 가 붙어 있는지 본다.
 *
 * <p>{@code CspNonceFilter} 는 {@code /stock} 아래에서 CSP 를 <b>강제</b>한다(그 밖은 Report-Only). 정책이 {@code
 * script-src 'self' 'nonce-…'} 이므로, nonce 없는 인라인 스크립트는 브라우저가 <b>조용히 차단</b>한다 &mdash; 서버 로그에도 안 남고
 * 화면도 멀쩡해 보이며 그 기능만 죽는다.
 *
 * <p>같은 규칙을 보는 {@code src/main/frontend/check-jte-script-nonce.mjs} 가 이미 있는데, 그것은 {@code npm run
 * build} 의 {@code check:nonce} 단계에서만 돈다. <b>메이븐은 프론트엔드를 빌드하지 않는다.</b> 그래서 {@code .jte} 만 고치면 그 검사가
 * 아예 돌지 않는다.
 *
 * <p>실측 2026-08-24로 확인했다 &mdash; {@code adminActions.jte} 에 nonce 없는 인라인 스크립트를 넣고 {@code node
 * check-jte-script-nonce.mjs} 는 종료 코드 1 로 잡았지만, {@code mvn test} 는 기준선 그대로 (370 · 실패 0) 지나갔다. 그 구멍을
 * 여기서 막는다.
 *
 * <p>JTE 주석({@code <%-- --%>})은 먼저 걷어낸다. 걷어내지 않으면 주석 안에서 규칙을 <b>설명하는</b> 문장의 {@code <script>} 를
 * 위반으로 읽는다(실제로 그렇게 오탐을 냈다 &mdash; {@code defaultLayout.jte} 의 htmx nonce 설명 주석).
 */
class InlineScriptNonceTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");

  /** JTE 주석 구간을 지운다. */
  private static String stripJteComments(String source) {
    StringBuilder kept = new StringBuilder();
    int at = 0;
    while (true) {
      int start = source.indexOf("<%--", at);
      if (start < 0) {
        kept.append(source, at, source.length());
        return kept.toString();
      }
      kept.append(source, at, start);
      int end = source.indexOf("--%>", start);
      if (end < 0) {
        return kept.toString();
      }
      // 줄 번호가 밀리지 않게 주석 안의 줄바꿈은 남긴다.
      for (int index = start; index < end; index++) {
        if (source.charAt(index) == '\n') {
          kept.append('\n');
        }
      }
      at = end + "--%>".length();
    }
  }

  private record Script(String file, int line, String attributes) {}

  /** 여는 {@code <script ...>} 태그를 모은다. 외부 스크립트({@code src=})는 nonce 대상이 아니다. */
  private List<Script> inlineScripts() throws IOException {
    List<Script> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        String source = stripJteComments(Files.readString(file, StandardCharsets.UTF_8));
        int at = source.indexOf("<script");
        while (at >= 0) {
          int close = source.indexOf('>', at);
          if (close < 0) {
            break;
          }
          String attributes = source.substring(at + "<script".length(), close);
          if (!attributes.contains("src=")) {
            found.add(
                new Script(
                    JTE_ROOT.relativize(file).toString().replace(java.io.File.separatorChar, '/'),
                    source.substring(0, at).split("\n", -1).length,
                    attributes.trim()));
          }
          at = source.indexOf("<script", close);
        }
      }
    }
    return found;
  }

  @Test
  void 인라인_스크립트는_모두_CSP_nonce_를_갖는다() throws IOException {
    List<String> offenders =
        inlineScripts().stream()
            .filter(script -> !script.attributes().contains("nonce="))
            .map(script -> script.file() + ":" + script.line())
            .toList();

    assertThat(offenders)
        .as(
            "/stock 아래는 CSP 가 강제라 nonce 없는 인라인 스크립트는 브라우저가 조용히 차단한다."
                + " <script nonce=\"${CspNonceHolder.getNonce()}\"> 로 쓸 것")
        .isEmpty();
  }

  /** 검사가 실제로 훑는지. 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_인라인_스크립트를_훑는다() throws IOException {
    // 실측 2026-08-24: 주석을 걷어낸 인라인 스크립트 40 개.
    assertThat(inlineScripts()).as("인라인 스크립트를 하나도 찾지 못했다").hasSizeGreaterThanOrEqualTo(25);
  }
}
