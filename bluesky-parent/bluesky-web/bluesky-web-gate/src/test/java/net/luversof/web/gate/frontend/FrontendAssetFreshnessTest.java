package net.luversof.web.gate.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 프런트엔드 소스와 커밋된 산출물이 어긋나면 빌드를 세운다.
 *
 * <p>왜 필요한가(실측): 이 프로젝트의 메이븐 빌드는 프런트엔드 빌드를 전혀 호출하지 않는다({@code pom.xml} 에 node/npm 연결 없음). 그래서
 * {@code src/main/resources/static} 에 커밋된 {@code .js}/{@code .css} 가 곧 배포본이다. 그런데 {@code npm run
 * build} 는 첫 단계 {@code check:nonce} 에서 죽어 있었고, 아무도 그 사실을 몰랐다.
 *
 * <p>그 사이 배포본 게이트가 실제로 놓친 것(배포 이미지에서 직접 확인):
 *
 * <ul>
 *   <li>{@code js/stock/tableSort.js} 1,793B — 표 정렬의 키보드 지원 전체(aria-sort/Enter/tabindex, htmx
 *       재바인딩)가 빠진 판. 소스 반영본은 2,990B.
 *   <li>{@code js/stock-charts.js} — 매수/매도 라벨 현지화 누락.
 *   <li>{@code js/common.js} — 활동 패널 로딩 누락.
 *   <li>{@code js/stock/stockSimulator.js} — 대비 수정 7곳 누락(라이트 테마 {@code text-xs} 라벨이 3.55:1, 4.17:1
 *       로 AA 4.5 미달인 채 서빙).
 * </ul>
 *
 * <p>지문은 {@code build-manifest.mjs} 가 빌드 마지막 단계에서 남긴다. 소스를 고치고 {@code npm run build} 를 하지 않으면 여기서
 * 깨진다.
 */
class FrontendAssetFreshnessTest {

  private static final Path FRONTEND = Path.of("src/main/frontend");
  private static final Path MANIFEST = Path.of("src/main/resources/frontend-build.json");

  /** {@code "경로": "해시"} 만 담긴 평평한 JSON 이라 정규식으로 충분하다. */
  private static final Pattern ENTRY = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([0-9a-f]{64})\"");

  private Map<String, String> readManifest() throws IOException {
    Map<String, String> recorded = new TreeMap<>();
    Matcher matcher = ENTRY.matcher(Files.readString(MANIFEST, StandardCharsets.UTF_8));
    while (matcher.find()) {
      recorded.put(matcher.group(1), matcher.group(2));
    }
    return recorded;
  }

  /** 개행은 체크아웃 설정(CRLF/LF)에 따라 달라지므로 지문에서 제외한다. build-manifest.mjs 와 같은 규칙이다. */
  private String digest(Path path) throws IOException {
    String text = Files.readString(path, StandardCharsets.UTF_8).replace("\r", "");
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder
            .append(Character.forDigit((b >> 4) & 0xf, 16))
            .append(Character.forDigit(b & 0xf, 16));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private Map<String, String> currentSources() throws IOException {
    Map<String, String> current = new TreeMap<>();
    try (Stream<Path> sources = Files.walk(FRONTEND.resolve("src"))) {
      for (Path path : sources.filter(Files::isRegularFile).toList()) {
        current.put(key(path), digest(path));
      }
    }
    for (String name : List.of("main.css", "tailwind.config.js", "tsconfig.json", "package.json")) {
      current.put(name, digest(FRONTEND.resolve(name)));
    }
    return current;
  }

  private String key(Path path) {
    return FRONTEND.relativize(path).toString().replace('\\', '/');
  }

  @Test
  void 커밋된_정적_산출물은_프런트엔드_소스와_같은_시점이다() throws IOException {
    assertThat(MANIFEST).as("지문 파일이 없다. src/main/frontend 에서 npm run build 를 실행할 것").exists();

    Map<String, String> recorded = readManifest();
    Map<String, String> current = currentSources();
    // 지문이 조용히 비면 검사가 무력해지므로 하한을 둔다(현재 26개).
    assertThat(recorded).as("지문이 비어 있다").hasSizeGreaterThan(20);

    List<String> drifted = new ArrayList<>();
    Map<String, String> merged = new LinkedHashMap<>(current);
    recorded.forEach(merged::putIfAbsent);
    for (String name : merged.keySet()) {
      String was = recorded.get(name);
      String now = current.get(name);
      if (was == null) {
        drifted.add(name + " (새 소스, 지문에 없음)");
      } else if (now == null) {
        drifted.add(name + " (사라진 소스, 지문에 남아 있음)");
      } else if (!was.equals(now)) {
        drifted.add(name + " (내용 변경)");
      }
    }

    assertThat(drifted)
        .as(
            "프런트엔드 소스가 바뀌었는데 산출물이 갱신되지 않았다. src/main/frontend 에서 npm run build 를 실행하고"
                + " src/main/resources/static 산출물과 frontend-build.json 을 함께 커밋할 것 —"
                + " 메이븐은 프런트엔드를 빌드하지 않으므로 커밋된 산출물이 곧 배포본이다")
        .isEmpty();
  }
}
