package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * htmx 가 겨냥하는 id 가 실제로 어딘가에 만들어지는지 본다.
 *
 * <p>{@code hx-target="#없는id"} 는 조용히 깨진다 &mdash; 서버 로그에도 화면에도 아무 표시가 없고, 그 요소를 눌러도 아무 일이 일어나지 않는다.
 *
 * <p>실측 2026-08-23: {@code tabsPortfolio.jte} 의 정렬 헤더 8 개가 모두 {@code #tab-content} 를 겨냥하는데, 그 id 를
 * 만드는 곳이 소스 전체에 없었다. 그 조각 자체가 이미 쓰이지 않는 잔재였고(=&gt; {@link UnreachableEndpointTest} 의
 * KNOWN_UNREACHABLE), 끊어진 타깃이 그 사실을 가리키는 증거였다.
 */
class HtmxTargetResolvesTest {

  /** 경로 구분자. 소스에 역슬래시 이스케이프를 남기지 않기 위한 것. */
  private static final char BACKSLASH = (char) 92;

  private static final List<Path> ROOTS = List.of(Path.of("src/main/jte"));

  /** 이미 죽은 조각으로 확인돼 목록에 오른 것. 사유는 UnreachableEndpointTest 에 있다. */
  private static final Map<String, String> KNOWN_BROKEN =
      Map.of("tab-content", "tabsPortfolio.jte 는 쓰이지 않는 잔재다(UnreachableEndpointTest 참고)");

  /** id 로 인정하는 형태: 리터럴 id="x", 그리고 컴포넌트 인자 id = "x". */
  private Set<String> definedIds() throws IOException {
    Set<String> ids = new LinkedHashSet<>();
    for (String source : sources()) {
      collect(source, "id=\"", ids);
      collect(source, "id = \"", ids);
    }
    return ids;
  }

  private void collect(String source, String marker, Set<String> into) {
    for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
      int from = at + marker.length();
      int end = source.indexOf('"', from);
      if (end < 0) {
        break;
      }
      String value = source.substring(from, end);
      if (!value.isEmpty() && !"null".equals(value)) {
        into.add(value);
      }
    }
  }

  private List<String> sources() throws IOException {
    List<String> texts = new ArrayList<>();
    for (Path root : ROOTS) {
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jte"))
                .toList()) {
          texts.add(Files.readString(file, StandardCharsets.UTF_8));
        }
      }
    }
    return texts;
  }

  /** 값이 {@code ${...}} 를 포함하면 항목마다 달라지므로 검사 대상이 아니다. */
  private static boolean isDynamic(String value) {
    return value.contains("${");
  }

  private List<String> brokenTargets() throws IOException {
    Set<String> defined = definedIds();
    List<String> broken = new ArrayList<>();
    for (Path root : ROOTS) {
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jte"))
                .filter(p -> p.toString().replace(BACKSLASH, '/').contains("/stock/"))
                .toList()) {
          String source = Files.readString(file, StandardCharsets.UTF_8);
          for (String attr : List.of("hx-target=\"", "hx-select=\"")) {
            for (int at = source.indexOf(attr); at >= 0; at = source.indexOf(attr, at + 1)) {
              int from = at + attr.length();
              int end = source.indexOf('"', from);
              if (end < 0) {
                break;
              }
              String value = source.substring(from, end).trim();
              if (!value.startsWith("#") || isDynamic(value)) {
                continue;
              }
              String id = value.substring(1);
              if (!defined.contains(id) && !KNOWN_BROKEN.containsKey(id)) {
                broken.add(file.getFileName() + " -> #" + id);
              }
            }
          }
        }
      }
    }
    return broken;
  }

  @Test
  void htmx_타깃이_가리키는_id_가_실제로_존재한다() throws IOException {
    // 스캔이 조용히 0건이 되면 검사가 무력해진다.
    assertThat(definedIds()).as("id 를 하나도 찾지 못했다").hasSizeGreaterThan(100);

    assertThat(brokenTargets()).as("hx-target 이 없는 id 를 가리킨다 — 눌러도 아무 일이 일어나지 않는다").isEmpty();
  }

  @Test
  void 알려진_끊어진_타깃이_아직도_끊겨_있다() throws IOException {
    Set<String> defined = definedIds();
    List<String> revived = KNOWN_BROKEN.keySet().stream().filter(defined::contains).toList();
    assertThat(revived).as("다시 만들어지기 시작한 id 가 목록에 남아 있다").isEmpty();
  }
}
