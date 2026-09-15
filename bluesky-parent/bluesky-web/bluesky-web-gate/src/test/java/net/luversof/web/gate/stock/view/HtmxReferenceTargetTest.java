package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * htmx 조각이 가리키는 것이 실제로 있는가 &mdash; 엔드포인트 · {@code hx-include} 폼 · {@code hx-target} 자리.
 *
 * <p>{@code UnreachableEndpointTest} 의 <b>반대 방향</b>이다. 그쪽은 "열어 놓고 아무도 안 부르는 경로" 를 찾고, 여기서는 "부르는데 없는
 * 경로" 를 찾는다. 셋 다 조용히 어긋난다:
 *
 * <ul>
 *   <li>없는 경로 &rarr; 404. 조각 자리에 오류 박스만 뜬다.
 *   <li>{@code hx-include} 대상이 없다 &rarr; <b>폼 값이 안 실린다</b>. 요청은 200 이고 화면도 멀쩡한데 <b>필터가 안 걸린 채</b> 더
 *       많은 데이터가 그려진다 &mdash; 이 저장소에서 이미 두 번 있었던 사고 유형이다.
 *   <li>{@code hx-target} 자리가 없다 &rarr; htmx 가 트리거 자신을 바꿔 내용이 엉뚱한 자리에 들어간다.
 * </ul>
 *
 * <p>실측 2026-09-14: 조각 148 개 · hx URL 66 개 · {@code hx-include} 10 개 · {@code hx-target} 26 개 · id
 * 259 개, 어긋난 것 <b>0</b>.
 *
 * <p>⚠ 이 훑기를 짜면서 거짓 양성을 세 번 냈다. 고칠 때 되풀이하지 말 것:
 *
 * <ul>
 *   <li>{@code @GetMapping} 만 모으면 {@code hx-post}/{@code hx-delete} 가 전부 "없는 경로" 가 된다 &mdash; 동사를
 *       맞출 것.
 *   <li>{@code /a/b/${id}} 는 경로 변수 자리다 &mdash; 변수 앞에서 자르면 {@code /a/b/} 가 되어 안 맞는다.
 *   <li>매핑 애너테이션은 {@code @org.springframework...DeleteMapping} 처럼 <b>완전한 이름</b>으로 적혀 있기도 하다.
 * </ul>
 */
class HtmxReferenceTargetTest {

  private static final Path JTE = Path.of("src/main/jte");

  private static final Path TS = Path.of("src/main/frontend/src");

  private static final Path CONTROLLERS = Path.of("src/main/java/net/luversof/web/gate");

  private static final Pattern CLASS_MAPPING =
      Pattern.compile(
          "@(?:[\\w.]*[.])?RequestMapping[(]\\s*(?:value\\s*=\\s*)?"
              + q()
              + "([^"
              + q()
              + "]*)"
              + q());

  private static final Pattern VERB_MAPPING =
      Pattern.compile(
          "@(?:[\\w.]*[.])?(Get|Post|Put|Delete|Patch)Mapping"
              + "(?:[(]\\s*(?:value\\s*=\\s*)?"
              + q()
              + "([^"
              + q()
              + "]*)"
              + q()
              + "[^)]*[)])?");

  private static final Pattern HX_URL =
      Pattern.compile("hx-(get|post|put|delete|patch)\\s*=\\s*" + q() + "([^" + q() + "]*)" + q());

  private static final Pattern HX_INCLUDE =
      Pattern.compile("hx-include\\s*=\\s*" + q() + "([^" + q() + "]*)" + q());

  private static final Pattern HX_TARGET =
      Pattern.compile("hx-target\\s*=\\s*" + q() + "([^" + q() + "]*)" + q());

  private static final Pattern SYNC_URL =
      Pattern.compile("data-sync-url\\s*=\\s*" + q() + "([^" + q() + "]*)" + q());

  private static final Pattern ID_ATTR =
      Pattern.compile("\\bid\\s*=\\s*" + q() + "([^" + q() + "$]*)" + q());

  private static final Pattern ID_IN_SCRIPT =
      Pattern.compile("(?:\\bid|[.]id)\\s*=\\s*['" + q() + "]([\\w-]+)['" + q() + "]");

  private static String q() {
    return String.valueOf((char) 34);
  }

  private static List<Path> files(Path root, String suffix) throws IOException {
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(suffix))
          .toList();
    }
  }

  /** 동사 &rarr; 열려 있는 경로. */
  private static Map<String, Set<String>> routes() throws IOException {
    Map<String, Set<String>> routes = new LinkedHashMap<>();
    for (Path path : files(CONTROLLERS, ".java")) {
      String text =
          Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)/[*].*?[*]/", " ");
      if (!text.contains("@Controller") && !text.contains("@RestController")) {
        continue;
      }
      Matcher base = CLASS_MAPPING.matcher(text);
      String prefix = base.find() ? base.group(1) : "";
      Matcher verb = VERB_MAPPING.matcher(text);
      while (verb.find()) {
        String sub = verb.group(2) == null ? "" : verb.group(2);
        String full = (prefix + sub).isEmpty() ? "/" : prefix + sub;
        routes.computeIfAbsent(verb.group(1).toUpperCase(), key -> new LinkedHashSet<>()).add(full);
      }
    }
    return routes;
  }

  private static boolean routeExists(Map<String, Set<String>> routes, String verb, String path) {
    for (String candidate : routes.getOrDefault(verb, Set.of())) {
      if (candidate.equals(path)) {
        return true;
      }
      if (candidate.indexOf('{') >= 0) {
        String regex = candidate.replaceAll("[{][^}]*[}]", "@@SEG@@");
        regex = Pattern.quote(regex).replace("@@SEG@@", "\\E[^/]+\\Q");
        if (Pattern.matches(regex, path)) {
          return true;
        }
      }
    }
    return false;
  }

  /** {@code ?} 와 {@code ${} 앞에서 자른 고정 부분. 마지막 칸이 통째로 변수면 한 칸을 채운다. */
  private static String staticPath(String url) {
    String path = url.split("[?]")[0];
    int at = path.indexOf("${");
    boolean hadVariable = at >= 0;
    if (hadVariable) {
      path = path.substring(0, at);
    }
    if (hadVariable && path.endsWith("/")) {
      path = path + "X";
    }
    return path;
  }

  @Test
  void 가리키는_것이_모두_있다() throws IOException {
    Map<String, Set<String>> routes = routes();
    int mappingCount = routes.values().stream().mapToInt(Set::size).sum();

    Map<String, Set<String>> urls = new LinkedHashMap<>();
    Set<String> includes = new LinkedHashSet<>();
    Set<String> targets = new LinkedHashSet<>();
    Set<String> ids = new LinkedHashSet<>();
    List<Path> templates = files(JTE, ".jte");
    for (Path path : templates) {
      String text =
          Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", " ");
      Matcher url = HX_URL.matcher(text);
      while (url.find()) {
        urls.computeIfAbsent(
                url.group(1).toUpperCase() + " " + url.group(2), k -> new LinkedHashSet<>())
            .add(path.getFileName().toString());
      }
      Matcher sync = SYNC_URL.matcher(text);
      while (sync.find()) {
        urls.computeIfAbsent("GET " + sync.group(1), k -> new LinkedHashSet<>())
            .add(path.getFileName().toString());
      }
      Matcher include = HX_INCLUDE.matcher(text);
      while (include.find()) {
        includes.add(include.group(1));
      }
      Matcher target = HX_TARGET.matcher(text);
      while (target.find()) {
        targets.add(target.group(1));
      }
      Matcher id = ID_ATTR.matcher(text);
      while (id.find()) {
        ids.add(id.group(1));
      }
    }
    for (Path path : files(TS, ".ts")) {
      Matcher id = ID_IN_SCRIPT.matcher(Files.readString(path, StandardCharsets.UTF_8));
      while (id.find()) {
        ids.add(id.group(1));
      }
    }

    // 자가검사 - 아무것도 못 모았다면 아래 0 건은 근거가 못 된다.
    assertThat(templates).as("조각을 못 읽었다").hasSizeGreaterThan(50);
    assertThat(mappingCount).as("매핑을 못 읽었다").isGreaterThan(50);
    assertThat(urls).as("hx URL 을 못 읽었다").hasSizeGreaterThan(30);
    assertThat(ids).as("id 를 못 읽었다").hasSizeGreaterThan(50);
    assertThat(includes).as("hx-include 를 못 읽었다").isNotEmpty();
    assertThat(targets).as("hx-target 을 못 읽었다").isNotEmpty();

    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : urls.entrySet()) {
      String[] parts = entry.getKey().split(" ", 2);
      String url = parts[1];
      if (!url.startsWith("/")) {
        continue;
      }
      String path = staticPath(url);
      if (path.isEmpty() || path.contains("${")) {
        continue;
      }
      if (!routeExists(routes, parts[0], path)) {
        problems.add("없는 경로: " + parts[0] + " " + path + " <- " + entry.getValue());
      }
    }
    for (String selector : includes) {
      for (String id : idsIn(selector)) {
        if (!ids.contains(id)) {
          problems.add("hx-include 대상 없음: #" + id + " (" + selector + ")");
        }
      }
    }
    for (String selector : targets) {
      if (selector.contains("${")) {
        continue;
      }
      for (String id : idsIn(selector)) {
        if (!ids.contains(id)) {
          problems.add("hx-target 대상 없음: #" + id + " (" + selector + ")");
        }
      }
    }

    assertThat(problems).as("가리키는 것이 없으면 조용히 어긋난다").isEmpty();
  }

  private static List<String> idsIn(String selector) {
    List<String> found = new ArrayList<>();
    Matcher matcher = Pattern.compile("#([\\w-]+)").matcher(selector);
    while (matcher.find()) {
      found.add(matcher.group(1));
    }
    return found;
  }
}
