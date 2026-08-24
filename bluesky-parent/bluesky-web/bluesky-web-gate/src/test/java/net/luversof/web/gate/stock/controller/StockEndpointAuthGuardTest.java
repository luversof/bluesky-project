package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 컨트롤러의 모든 엔드포인트가 스스로 로그인 여부를 확인하는지 본다.
 *
 * <p>이 앱은 URL 단위 인가가 없다 — 시큐리티 체인이 {@code anyRequest().permitAll()} 이고 CSRF 도 꺼져 있으며,
 * {@code @BlueskyPreAuthorize}({@code @PreAuthorize("hasRole('USER')")}) 는 메서드 보안이 켜져 있지 않아 실효가
 * 없다(실측: 로그인 없이 {@code GET /api/stock/stockItem/search/findByName/…} 가 200 과 종목 정보를 돌려줬다). 즉 보호는
 * 전적으로 컨트롤러 안의 수동 검사에 달려 있어서, 새 엔드포인트에서 한 번 빠뜨리면 그대로 공개된다. 그 실수를 빌드에서 잡는다.
 *
 * <p>소스를 직접 읽는 이유: 검사 여부가 메서드 '본문'에 있어 리플렉션으로는 보이지 않는다.
 */
class StockEndpointAuthGuardTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static final Pattern MAPPING =
      Pattern.compile(
          "@(Get|Post|Put|Delete|Patch)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"[^)]*\\))?");

  /** 수동 검사로 인정하는 표현. */
  private static final List<String> GUARDS =
      List.of(
          "isNotAuthenticated()",
          "getUserId() == null",
          "userId == null",
          "loginRequiredView",
          "status(401)");

  /**
   * 사용자 데이터를 다루지 않아 검사가 없어도 되는 예외 목록.
   *
   * <p>dashboard 는 hx-get 자리표시자만 있는 껍데기(내부 조각이 각자 안내를 낸다), realized-profit 은 /stock/trade 로 보내는
   * 리다이렉트다. 실측으로 비로그인 응답에 사용자 데이터가 없음을 확인했다.
   */
  private static final Set<String> ALLOWED_WITHOUT_GUARD =
      Set.of("StockSummaryHtmxController:/dashboard", "StockViewController:/realized-profit");

  private record Endpoint(String file, String path, String verb, boolean guarded) {}

  private List<Endpoint> scan() throws IOException {
    List<Endpoint> endpoints = new ArrayList<>();
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        String simpleName = file.getFileName().toString().replace(".java", "");
        Matcher matcher = MAPPING.matcher(source);
        while (matcher.find()) {
          String path = matcher.group(2) == null ? "" : matcher.group(2);
          String body = methodBody(source, matcher.end());
          boolean guarded = GUARDS.stream().anyMatch(body::contains);
          endpoints.add(new Endpoint(simpleName, path, matcher.group(1), guarded));
        }
      }
    }
    return endpoints;
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

  @Test
  void 모든_주식_엔드포인트는_로그인_여부를_직접_확인한다() throws IOException {
    List<Endpoint> endpoints = scan();
    // 파서가 조용히 0건을 반환하면 검사가 무력해지므로 하한을 둔다(현재 45개).
    assertThat(endpoints).hasSizeGreaterThan(40);

    List<String> unguarded =
        endpoints.stream()
            .filter(endpoint -> !endpoint.guarded())
            .map(endpoint -> endpoint.file() + ":" + endpoint.path())
            .filter(key -> !ALLOWED_WITHOUT_GUARD.contains(key))
            .toList();

    assertThat(unguarded)
        .as(
            "로그인 확인이 없는 엔드포인트다. 컨트롤러 안에서 세션 사용자를 확인하고 (조각은 loginRequiredView, JSON 은 401,"
                + " 화면은 로그인 리다이렉트) 돌려주거나, 사용자 데이터를 다루지 않는다면 ALLOWED_WITHOUT_GUARD 에 근거와 함께 등록할 것")
        .isEmpty();
  }

  /**
   * 애노테이션에 속성이 더 붙은 엔드포인트의 경로를 제대로 읽는지.
   *
   * <p>예전 정규식은 {@code @GetMapping("...")} 형태만 읽어 {@code @GetMapping(value = "/timeSeries", produces
   * = ...)} 의 경로를 빈 문자열로 취급했다. 검사 자체는 본문을 보므로 통과했지만, 같은 사각이 게이트 익명접근 검사에서는 실제 사고를 냈다 &mdash; 경로를
   * {@code /stock/api} 로 잘못 만들어 <b>{@code /stock/api/timeSeries} 를 아무도 확인하지 않은 채</b> 404 를 정상으로 넘기고
   * 있었다.
   */
  @Test
  void 속성이_붙은_애노테이션도_경로를_읽는다() throws IOException {
    List<String> keys =
        scan().stream().map(endpoint -> endpoint.file() + ":" + endpoint.path()).toList();
    assertThat(keys)
        .as("@GetMapping(value = \"/timeSeries\", produces = ...) 의 경로를 읽지 못하고 있다")
        .contains("StockApiController:/timeSeries");
  }

  @Test
  void 예외로_등록한_엔드포인트는_실제로_남아있다() throws IOException {
    List<String> keys =
        scan().stream().map(endpoint -> endpoint.file() + ":" + endpoint.path()).toList();
    assertThat(keys).containsAll(ALLOWED_WITHOUT_GUARD);
  }
}
