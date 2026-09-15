package net.luversof.web.gate.stock.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * 게이트가 api-stock 에 <b>보내는</b> 질의 키가 저쪽이 받는 이름인가.
 *
 * <p>응답 쪽({@link ApiStockWireFieldParityTest})의 짝이다. 이름이 어긋나면 <b>필터가 조용히 안 걸린다</b> &mdash; 요청은 200
 * 이고 화면도 멀쩡한데 좁혀야 할 범위가 안 좁혀져 <b>더 많은 데이터</b>가 그려진다. 이 저장소에서 이미 두 번 있었던 사고 유형이다(연도별 세금·비용이 계좌 필터를
 * 무시한 건, 주입 차단 코드가 계좌 필터까지 지운 건).
 *
 * <p>실측 2026-09-14: 게이트가 만드는 키 13 개가 모두 api 가 받는 이름이고, 질의를 조립하는 세 요청 객체는 상대 DTO 와 필드 집합이 정확히 같다.
 *
 * <p>반대 방향(api 가 받는데 게이트가 안 보내는 이름)은 보지 않는다 &mdash; 안 쓰는 조건을 안 보내는 것은 정상이다.
 */
class ApiStockRequestParamParityTest {

  private static final Path GATE_STOCK = Path.of("src/main/java/net/luversof/web/gate/stock");

  /** 형제 모듈. 워크스페이스를 통째로 받지 않았으면 없을 수 있다. */
  private static final Path API_SRC = Path.of("../../bluesky-api/bluesky-api-stock/src/main/java");

  /** 질의를 조립하는 게이트 요청 객체 &rarr; 그것을 받는 api DTO(이름이 다른 것이 있다). */
  private static final Map<String, String> SENDER_TO_API = new LinkedHashMap<>();

  static {
    SENDER_TO_API.put("DividendRequest", "DividendSearchRequest");
    SENDER_TO_API.put("TradeSearchRequest", "TradeSearchRequest");
    SENDER_TO_API.put("TradeProfitRequest", "TradeProfitRequest");
  }

  private static final Pattern ADDED_KEY =
      Pattern.compile("[.](?:add|set|addAll|put)[(]" + q() + "([A-Za-z][\\w.]*)" + q());

  private static String q() {
    return String.valueOf((char) 34);
  }

  private static String strip(String text) {
    return text.replaceAll("(?s)/[*].*?[*]/", " ").replaceAll("//[^\\n]*", " ");
  }

  private static List<Path> javaFiles(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java"))
          .toList();
    }
  }

  private static Set<String> keysAddedIn(String source) {
    Set<String> keys = new LinkedHashSet<>();
    Matcher matcher = ADDED_KEY.matcher(source);
    while (matcher.find()) {
      keys.add(matcher.group(1));
    }
    return keys;
  }

  /** api DTO 가 받는 이름: 필드 또는 레코드 컴포넌트. */
  private static Set<String> apiDtoFields(String name) throws IOException {
    Set<String> fields = new LinkedHashSet<>();
    for (Path path : javaFiles(API_SRC)) {
      if (!path.getFileName().toString().equals(name + ".java")) {
        continue;
      }
      String text = strip(Files.readString(path, StandardCharsets.UTF_8));
      Matcher field =
          Pattern.compile("private\\s+[\\w.<>,\\[\\]?\\s]+?\\s+([a-z]\\w*)\\s*[;=]").matcher(text);
      while (field.find()) {
        fields.add(field.group(1));
      }
      Matcher head = Pattern.compile("record\\s+" + name + "\\s*[(]([^)]*)[)]").matcher(text);
      if (head.find()) {
        for (String part : head.group(1).split(",")) {
          Matcher word = Pattern.compile("[A-Za-z_]\\w*").matcher(part);
          String last = null;
          while (word.find()) {
            last = word.group();
          }
          if (last != null) {
            fields.add(last);
          }
        }
      }
    }
    return fields;
  }

  /**
   * api 가 어디서든 받는 이름 전부.
   *
   * <p>⚠ {@code @RequestParam} 정규식만으로는 <b>절반을 놓친다</b>. 실측 2026-09-14: 그렇게 짰더니 {@code accountId ·
   * breakdown · date · dates · granularity} 다섯을 "api 가 모른다" 고 보고했는데 전부 저쪽이 받는 이름이었다 &mdash;
   * {@code @RequestParam @DateTimeFormat(...) LocalDate date} 처럼 애너테이션이 끼어 있거나, {@code String
   * granularity} 처럼 애너테이션 없이 단순 타입으로 받는 경우다. 그래서 <b>핸들러 시그니처를 통째로 잘라</b> 파라미터마다 마지막 식별자를 취한다.
   */
  private static Set<String> apiAcceptedNames() throws IOException {
    Set<String> names = new LinkedHashSet<>();
    for (Path path : javaFiles(API_SRC)) {
      String text = strip(Files.readString(path, StandardCharsets.UTF_8));
      String file = path.getFileName().toString();

      Matcher named =
          Pattern.compile(
                  "@RequestParam[(]\\s*(?:name\\s*=\\s*|value\\s*=\\s*)?"
                      + q()
                      + "([A-Za-z][\\w.]*)"
                      + q())
              .matcher(text);
      while (named.find()) {
        names.add(named.group(1));
      }

      if (path.toString().replace((char) 92, '/').contains("/dto/request/")) {
        names.addAll(apiDtoFields(file.replace(".java", "")));
      }

      if (file.endsWith("Controller.java")) {
        for (String signature : handlerSignatures(text)) {
          names.addAll(parameterNames(signature));
        }
      }
    }
    return names;
  }

  /** {@code @...Mapping} 이 붙은 메서드의 괄호 안. */
  private static List<String> handlerSignatures(String text) {
    List<String> signatures = new ArrayList<>();
    Matcher mapping = Pattern.compile("@(?:Get|Post|Put|Delete|Patch)Mapping").matcher(text);
    while (mapping.find()) {
      int open = text.indexOf('(', mapping.end());
      // 매핑 자신의 괄호는 건너뛴다
      if (open >= 0 && text.substring(mapping.end(), open).trim().isEmpty()) {
        open = text.indexOf('(', matching(text, open) + 1);
      }
      if (open < 0) {
        continue;
      }
      int close = matching(text, open);
      if (close > open) {
        signatures.add(text.substring(open + 1, close));
      }
    }
    return signatures;
  }

  /** {@code text[open]} 의 짝이 되는 닫는 괄호 위치. */
  private static int matching(String text, int open) {
    int depth = 0;
    for (int i = open; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '(') {
        depth++;
      } else if (ch == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  /** 파라미터 목록에서 이름들. 경로 변수·본문은 질의가 아니므로 뺀다. */
  private static Set<String> parameterNames(String signature) {
    Set<String> names = new LinkedHashSet<>();
    for (String part : splitTopLevel(signature)) {
      if (part.contains("@PathVariable")
          || part.contains("@RequestBody")
          || part.contains("@RequestHeader")
          || part.isBlank()) {
        continue;
      }
      Matcher word = Pattern.compile("[A-Za-z_]\\w*").matcher(part);
      String last = null;
      while (word.find()) {
        last = word.group();
      }
      if (last != null && Character.isLowerCase(last.charAt(0))) {
        names.add(last);
      }
    }
    return names;
  }

  /** 중첩 괄호·꺾쇠 안의 쉼표는 자르지 않는다. */
  private static List<String> splitTopLevel(String text) {
    List<String> parts = new ArrayList<>();
    StringBuilder buffer = new StringBuilder();
    int depth = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '(' || ch == '<' || ch == '[') {
        depth++;
      } else if (ch == ')' || ch == '>' || ch == ']') {
        depth--;
      }
      if (depth == 0 && ch == ',') {
        parts.add(buffer.toString());
        buffer.setLength(0);
      } else {
        buffer.append(ch);
      }
    }
    parts.add(buffer.toString());
    return parts;
  }

  /** 질의를 조립하는 요청 객체는 상대 DTO 와 이름이 정확히 맞아야 한다. */
  @Test
  void 요청_객체가_보내는_이름이_상대_DTO_에_있다() throws IOException {
    assumeTrue(Files.isDirectory(API_SRC), "형제 모듈 api-stock 이 없다 - 이 검사는 건너뛴다");

    List<String> problems = new ArrayList<>();
    int checked = 0;
    for (Map.Entry<String, String> entry : SENDER_TO_API.entrySet()) {
      Path sender =
          javaFiles(GATE_STOCK).stream()
              .filter(p -> p.getFileName().toString().equals(entry.getKey() + ".java"))
              .findFirst()
              .orElse(null);
      assertThat(sender).as(entry.getKey() + " 를 못 찾았다 - 이름이 바뀌었나").isNotNull();

      Set<String> sent = keysAddedIn(strip(Files.readString(sender, StandardCharsets.UTF_8)));
      assertThat(sent).as(entry.getKey() + " 가 만드는 키가 0 개다 - 파서가 무력하다").isNotEmpty();

      Set<String> accepted = apiDtoFields(entry.getValue());
      assertThat(accepted).as(entry.getValue() + " 의 필드를 못 읽었다").isNotEmpty();

      checked++;
      for (String key : sent) {
        if (!accepted.contains(key)) {
          problems.add(entry.getKey() + " -> " + entry.getValue() + " : " + key + " 를 저쪽이 안 받는다");
        }
      }
    }

    assertThat(checked).isEqualTo(SENDER_TO_API.size());
    assertThat(problems).as("이름이 어긋나면 필터가 조용히 안 걸린다").isEmpty();
  }

  /** 게이트가 어디서든 만드는 키는 api 가 어디선가는 받는 이름이어야 한다(오타 잡기). */
  @Test
  void 게이트가_만드는_키가_api_어휘_안에_있다() throws IOException {
    assumeTrue(Files.isDirectory(API_SRC), "형제 모듈 api-stock 이 없다 - 이 검사는 건너뛴다");

    Set<String> sent = new LinkedHashSet<>();
    for (Path path : javaFiles(GATE_STOCK)) {
      if (path.toString().replace((char) 92, '/').contains("/httpexchange/")) {
        continue;
      }
      sent.addAll(keysAddedIn(strip(Files.readString(path, StandardCharsets.UTF_8))));
    }
    Set<String> accepted = apiAcceptedNames();

    assertThat(sent).as("보내는 키를 하나도 못 찾았다면 이 검사는 공짜로 통과한다").hasSizeGreaterThanOrEqualTo(10);
    assertThat(accepted).as("api 어휘를 못 읽었다").hasSizeGreaterThanOrEqualTo(10);

    List<String> unknown = sent.stream().filter(key -> !accepted.contains(key)).sorted().toList();
    assertThat(unknown).as("api 가 모르는 이름은 조용히 버려진다").isEmpty();
  }
}
