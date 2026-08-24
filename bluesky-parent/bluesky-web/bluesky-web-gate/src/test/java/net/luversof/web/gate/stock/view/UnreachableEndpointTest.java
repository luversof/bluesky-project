package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면의 GET 엔드포인트가 실제로 닿을 수 있는 곳인지 본다.
 *
 * <p>닿을 수 있는 경로는 세 가지다 — 메뉴({@code application.properties} 의 {@code menu.stock[n].url}), 템플릿의
 * 링크/htmx 속성, 그리고 브라우저 스크립트의 요청. 셋 중 어디에도 없으면 사용자는 그 화면에 도달할 수 없다.
 *
 * <p>왜 필요한가(실측 2026-08-23): 28 개 중 5 개가 그런 상태였다. 죽은 라우트 자체가 해를 끼치지는 않지만 <b>살아 있는 코드로 오해</b>하게 만든다.
 * 실제로 이 저장소에서 {@code /stock/htmx/asset-growth/period-return} 쪽 실패 처리를 고쳤는데, 그 경로는 화면에서 호출되지 않아
 * 사용자에게 아무 효과가 없었다(실제 화면은 {@code /asset-growth/view} 가 요약을 함께 렌더한다).
 *
 * <p>알려진 것은 사유와 함께 목록에 두고 새로 생기는 것만 실패시킨다. 반대로 목록의 경로가 다시 쓰이거나 사라지면 목록이 낡은 것이므로 그것도 알린다.
 */
class UnreachableEndpointTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static final Path PROPERTIES = Path.of("src/main/resources/application.properties");

  private static final List<Path> REFERENCE_ROOTS =
      List.of(Path.of("src/main/jte"), Path.of("src/main/frontend/src"));

  private static final Pattern CLASS_MAPPING =
      Pattern.compile("@RequestMapping\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");

  private static final Pattern GET_MAPPING =
      Pattern.compile("@GetMapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"[^)]*\\))?");

  private static final Pattern MENU_URL = Pattern.compile("menu\\.stock\\[\\d+\\]\\.url=(\\S+)");

  /** 닿을 수 없는 것이 확인된 경로와 그 사유. */
  /** 큰따옴표 한 글자. 소스에 역슬래시 이스케이프를 남기지 않기 위한 것. */
  private static final String MARKER_QUOTE = String.valueOf((char) 34);

  private static final Map<String, String> KNOWN_UNREACHABLE = new LinkedHashMap<>();

  static {
    KNOWN_UNREACHABLE.put(
        "/api/stock/tradeProfit/calculateProfit", "게이트 JSON 통과 엔드포인트인데 부르는 화면/스크립트가 없다");
    KNOWN_UNREACHABLE.put("/stock/dashboard", "본문 없이 redirect:/stock 만 한다(메뉴는 /stock 을 가리킨다)");
    KNOWN_UNREACHABLE.put("/stock/htmx/dashboard", "hx-get 자리표시자 껍데기인데 그 껍데기를 부르는 화면이 없다");
    KNOWN_UNREACHABLE.put(
        "/stock/htmx/asset-growth/period-return",
        "기간 요약은 /asset-growth/view 가 함께 렌더한다. 이 라우트는 따로 호출되지 않는다");
    KNOWN_UNREACHABLE.put("/stock/realized-profit", "redirect:/stock/trade 로 남겨 둔 옛 주소");
    KNOWN_UNREACHABLE.put(
        "/stock/htmx/portfolio",
        "서버 왕복 정렬 시절의 잔재. 참조는 tabsPortfolio.jte 정렬 헤더 8 개가 자기 자신을 다시 부르는 것뿐이고,"
            + " 그 헤더가 겨냥하는 #tab-content 는 이제 어디에도 없다. 지금 보유 표는 assetStatus.jte 가"
            + " data-sort-key 로 클라이언트 정렬한다");
  }

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private Set<String> declaredGetPaths() throws IOException {
    Set<String> paths = new TreeSet<>();
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = read(file);
        int classIndex = source.indexOf("public class");
        String head = classIndex > 0 ? source.substring(0, classIndex) : "";
        String body = classIndex > 0 ? source.substring(classIndex) : source;
        String base = "";
        Matcher classMatcher = CLASS_MAPPING.matcher(head);
        while (classMatcher.find()) {
          base = classMatcher.group(1);
        }
        Matcher matcher = GET_MAPPING.matcher(body);
        while (matcher.find()) {
          String path = matcher.group(1) == null ? "" : matcher.group(1);
          String full = base + path;
          if (full.isEmpty()) {
            full = "/";
          }
          if (!full.contains("{")) {
            paths.add(full);
          }
        }
      }
    }
    return paths;
  }

  private Set<String> menuUrls() throws IOException {
    Set<String> urls = new TreeSet<>();
    Matcher matcher = MENU_URL.matcher(read(PROPERTIES));
    while (matcher.find()) {
      urls.add(matcher.group(1));
    }
    return urls;
  }

  /**
   * 참조 파일을 <b>경로와 함께</b> 읽는다.
   *
   * <p>예전에는 본문만 모아 "어딘가에 이 경로 문자열이 있으면 닿을 수 있다"고 봤다. 그래서 조각이 <b>자기 자신의 엔드포인트</b>를 부르는 경우도 통과했다
   * &mdash; 실측: {@code /stock/htmx/portfolio} 는 {@code tabsPortfolio.jte} 의 정렬 헤더 8 개가 자기 자신을 다시
   * 부르는 것이 참조의 전부였고, 그 조각을 처음 실어 주는 화면은 어디에도 없었다. 닫힌 고리라 사용자는 영원히 도달하지 못한다.
   */
  private Map<Path, String> referenceTexts() throws IOException {
    Map<Path, String> texts = new LinkedHashMap<>();
    for (Path root : REFERENCE_ROOTS) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jte") || p.toString().endsWith(".ts"))
                .toList()) {
          texts.put(file, Files.readString(file, StandardCharsets.UTF_8));
        }
      }
    }
    return texts;
  }

  /** 각 GET 경로가 렌더하는 뷰 파일. 그 파일 안의 참조는 자기 자신이므로 도달 근거가 못 된다. */
  private Map<String, Set<Path>> viewFilesByPath() throws IOException {
    Map<String, Set<Path>> views = new LinkedHashMap<>();
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = read(file);
        int classIndex = source.indexOf("public class");
        String head = classIndex > 0 ? source.substring(0, classIndex) : "";
        String body = classIndex > 0 ? source.substring(classIndex) : source;
        String base = "";
        Matcher classMatcher = CLASS_MAPPING.matcher(head);
        while (classMatcher.find()) {
          base = classMatcher.group(1);
        }
        Matcher matcher = GET_MAPPING.matcher(body);
        while (matcher.find()) {
          String path = matcher.group(1) == null ? "" : matcher.group(1);
          String full = base + path;
          if (full.isEmpty() || full.contains("{")) {
            continue;
          }
          for (String view : returnedViews(methodBody(body, matcher.end()))) {
            views
                .computeIfAbsent(full, key -> new java.util.LinkedHashSet<>())
                .add(Path.of("src/main/jte", view + ".jte"));
          }
        }
      }
    }
    return views;
  }

  /** 메서드 본문에서 {@code return "stock/..."} 형태의 뷰 이름만 모은다. */
  private Set<String> returnedViews(String methodBody) {
    Set<String> views = new java.util.LinkedHashSet<>();
    // 정규식을 쓰지 않는다 - 이 파일을 셸 heredoc 으로 쓰면 역슬래시가 한 겹 줄어 컴파일이 깨진 적이 있다.
    String marker = "return " + MARKER_QUOTE;
    for (int at = methodBody.indexOf(marker); at >= 0; at = methodBody.indexOf(marker, at + 1)) {
      int from = at + marker.length();
      int end = methodBody.indexOf(MARKER_QUOTE.charAt(0), from);
      if (end < 0) {
        break;
      }
      String value = methodBody.substring(from, end);
      if (value.startsWith("stock/") || value.startsWith("_components/")) {
        views.add(value);
      }
    }
    return views;
  }

  /** 매핑 애노테이션 뒤 첫 '{' 부터 짝이 맞는 '}' 까지. */
  private String methodBody(String source, int from) {
    int open = source.indexOf('{', from);
    if (open < 0) {
      return "";
    }
    int depth = 1;
    int i = open + 1;
    while (i < source.length() && depth > 0) {
      char c = source.charAt(i++);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return source.substring(open, i);
  }

  private List<String> unreachable() throws IOException {
    Set<String> declared = declaredGetPaths();
    Set<String> menu = menuUrls();
    Map<Path, String> texts = referenceTexts();
    Map<String, Set<Path>> ownViews = viewFilesByPath();
    List<String> unreachable = new ArrayList<>();
    for (String path : declared) {
      if (menu.contains(path)) {
        continue;
      }
      Set<Path> own = ownViews.getOrDefault(path, Set.of());
      boolean referencedFromElsewhere =
          texts.entrySet().stream()
              .filter(entry -> !own.contains(entry.getKey()))
              .anyMatch(entry -> entry.getValue().contains(path));
      if (referencedFromElsewhere) {
        continue;
      }
      unreachable.add(path);
    }
    return unreachable;
  }

  @Test
  void 선언된_엔드포인트는_메뉴나_화면에서_닿을_수_있다() throws IOException {
    // 파서가 조용히 0건이 되면 검사가 무력해진다(현재 28개).
    assertThat(declaredGetPaths()).as("엔드포인트를 찾지 못했다").hasSizeGreaterThan(20);
    assertThat(menuUrls()).as("메뉴 설정을 읽지 못했다").hasSizeGreaterThan(3);

    List<String> unexpected =
        unreachable().stream().filter(path -> !KNOWN_UNREACHABLE.containsKey(path)).toList();

    assertThat(unexpected)
        .as(
            "메뉴에도 없고 템플릿/스크립트에서도 부르지 않는 새 엔드포인트다. 화면에 연결하거나, 쓰지 않는 것이면"
                + " KNOWN_UNREACHABLE 에 사유와 함께 등록할 것")
        .isEmpty();
  }

  @Test
  void 목록에_남은_경로가_아직도_닿을_수_없다() throws IOException {
    List<String> declared = List.copyOf(declaredGetPaths());
    List<String> stillUnreachable = unreachable();

    List<String> revived =
        KNOWN_UNREACHABLE.keySet().stream()
            .filter(declared::contains)
            .filter(path -> !stillUnreachable.contains(path))
            .toList();
    assertThat(revived).as("다시 쓰이기 시작한 경로가 목록에 남아 있다").isEmpty();

    List<String> gone =
        KNOWN_UNREACHABLE.keySet().stream().filter(path -> !declared.contains(path)).toList();
    assertThat(gone).as("목록의 경로가 더 이상 선언되지 않는다").isEmpty();
  }
}
