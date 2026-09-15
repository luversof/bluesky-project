package net.luversof.web.gate.stock.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 게이트가 api-stock 에서 <b>받는</b> 응답 DTO 의 와이어 필드가 저쪽에 실제로 있는가.
 *
 * <p>게이트는 api-stock 의 응답 레코드를 <b>복사해서</b> 갖고 있다(HTTP 로만 이어져 있어 컴파일 의존이 없다). 그래서 저쪽이 필드 이름을 바꾸면 이쪽은
 * 컴파일도 되고 요청도 200 인데 그 값만 <b>항상 null</b> 이 된다 &mdash; 화면에서는 칸이 비거나 0 으로 보일 뿐이라 눈으로 못 잡는다. 모든 DTO 가
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} 라 예외도 안 난다.
 *
 * <p>실측 2026-09-14: 받는 DTO 18 개 · 와이어 필드 158 개, 어긋난 것 <b>0</b>. 지금 깨진 것은 없고 <b>깨졌을 때 조용한 것</b>을
 * 막는다.
 *
 * <p>비교는 <b>와이어 이름</b>으로 한다 &mdash; {@code @JsonProperty} 로 이름을 바꾼 필드는 그 이름이, {@code @JsonIgnore} 인
 * 필드는 애초에 오가지 않으므로 뺀다. 반대 방향(api 에만 있는 필드)은 보지 않는다. 안 쓰는 값을 안 받는 것은 정상이고 {@code ignoreUnknown} 이 받아
 * 준다.
 */
class ApiStockWireFieldParityTest {

  private static final String GATE_PACKAGE = "net.luversof.web.gate.stock.dto.response.";

  private static final Path GATE_DTO_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/dto/response");

  private static final Path CLIENT_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/httpexchange");

  /** 형제 모듈. 워크스페이스를 통째로 받지 않았으면 없을 수 있다. */
  private static final Path API_SRC = Path.of("../../bluesky-api/bluesky-api-stock/src/main/java");

  /** 클라이언트 인터페이스에 이름이 보이는 DTO = 실제로 역직렬화되는 것. */
  private static List<String> receivedDtoNames() throws IOException {
    StringBuilder clients = new StringBuilder();
    try (Stream<Path> files = Files.list(CLIENT_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        clients.append(Files.readString(file, StandardCharsets.UTF_8)).append(' ');
      }
    }
    String text = clients.toString();
    List<String> names = new ArrayList<>();
    try (Stream<Path> files = Files.list(GATE_DTO_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String name = file.getFileName().toString().replace(".java", "");
        if (Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find()) {
          names.add(name);
        }
      }
    }
    return names;
  }

  /**
   * 게이트 쪽은 리플렉션으로 읽는다 - 소스를 파싱할 이유가 없다.
   *
   * <p>⚠ 레코드 컴포넌트에 적은 애너테이션을 {@link RecordComponent#getAnnotation} 으로 찾으면 <b>안 나온다</b>.
   * {@code @JsonProperty} 의 {@code @Target} 에 {@code RECORD_COMPONENT} 가 없어서 javac 가 필드·접근자·생성자
   * 파라미터 쪽으로만 흘려보내기 때문이다. 실제로 이걸 놓쳐 변이 실험({@code @JsonProperty("tradeFirstDay")} 부착)이 통과했다 &mdash;
   * 이름 바꿔치기야말로 이 가드가 잡아야 할 것인데 그걸 못 봤다. 세 군데를 다 본다.
   */
  private static List<String> gateWireFields(String name) throws ClassNotFoundException {
    Class<?> type = Class.forName(GATE_PACKAGE + name);
    List<String> fields = new ArrayList<>();
    for (RecordComponent component : type.getRecordComponents()) {
      if (find(type, component, JsonIgnore.class) != null) {
        continue;
      }
      JsonProperty named = find(type, component, JsonProperty.class);
      fields.add(named != null && !named.value().isBlank() ? named.value() : component.getName());
    }
    return fields;
  }

  /** 컴포넌트 &rarr; 접근자 &rarr; 필드 순으로 찾는다(어디로 흘러갔는지는 애너테이션의 @Target 이 정한다). */
  private static <A extends java.lang.annotation.Annotation> A find(
      Class<?> type, RecordComponent component, Class<A> annotation) {
    A found = component.getAnnotation(annotation);
    if (found != null) {
      return found;
    }
    found = component.getAccessor().getAnnotation(annotation);
    if (found != null) {
      return found;
    }
    try {
      return type.getDeclaredField(component.getName()).getAnnotation(annotation);
    } catch (NoSuchFieldException ex) {
      return null;
    }
  }

  /** api 쪽은 소스에서 읽는다 - 클래스패스에 없다(HTTP 로만 이어져 있다). */
  private static Set<String> apiWireFields(Path file, String name) throws IOException {
    String text =
        Files.readString(file, StandardCharsets.UTF_8)
            .replaceAll("(?s)/\\*.*?\\*/", " ")
            .replaceAll("//[^\n]*", " ");
    Matcher head = Pattern.compile("record\\s+" + Pattern.quote(name) + "\\s*\\(").matcher(text);
    if (!head.find()) {
      return Set.of();
    }
    List<String> parts = new ArrayList<>();
    StringBuilder buffer = new StringBuilder();
    int depth = 1;
    for (int i = head.end(); i < text.length() && depth > 0; i++) {
      char ch = text.charAt(i);
      if (ch == '(' || ch == '<' || ch == '[') {
        depth++;
      } else if (ch == ')' || ch == '>' || ch == ']') {
        depth--;
        if (depth == 0) {
          break;
        }
      }
      if (depth == 1 && ch == ',') {
        parts.add(buffer.toString());
        buffer.setLength(0);
      } else {
        buffer.append(ch);
      }
    }
    parts.add(buffer.toString());

    Set<String> fields = new LinkedHashSet<>();
    for (String part : parts) {
      if (part.contains("@JsonIgnore") && !part.contains("@JsonIgnoreProperties")) {
        continue;
      }
      Matcher named = Pattern.compile("@JsonProperty\\(\\s*\"([^\"]+)\"").matcher(part);
      if (named.find()) {
        fields.add(named.group(1));
        continue;
      }
      String[] tokens = part.trim().split("\\s+");
      if (tokens.length == 0 || tokens[tokens.length - 1].isBlank()) {
        continue;
      }
      Matcher word = Pattern.compile("[A-Za-z_]\\w*").matcher(tokens[tokens.length - 1]);
      String last = null;
      while (word.find()) {
        last = word.group();
      }
      if (last != null) {
        fields.add(last);
      }
    }
    return fields;
  }

  private static Path apiFile(String name) throws IOException {
    try (Stream<Path> files = Files.walk(API_SRC)) {
      return files
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().equals(name + ".java"))
          .filter(p -> p.toString().replace('\\', '/').contains("dto"))
          .findFirst()
          .orElse(null);
    }
  }

  @Test
  void 받는_필드가_전부_api_에_있다() throws Exception {
    assumeTrue(Files.isDirectory(API_SRC), "형제 모듈 api-stock 이 없다 - 이 검사는 건너뛴다");

    List<String> names = receivedDtoNames();
    assertThat(names).as("클라이언트에서 DTO 를 하나도 못 찾았다면 이 검사는 공짜로 통과한다").hasSizeGreaterThanOrEqualTo(15);

    List<String> problems = new ArrayList<>();
    int checked = 0;
    int fieldCount = 0;
    for (String name : names) {
      Path file = apiFile(name);
      if (file == null) {
        problems.add(name + " : api-stock 에 같은 이름의 DTO 가 없다");
        continue;
      }
      Set<String> api = apiWireFields(file, name);
      if (api.isEmpty()) {
        problems.add(name + " : api 쪽 레코드를 못 읽었다(형태가 바뀌었나)");
        continue;
      }
      checked++;
      List<String> gate = gateWireFields(name);
      fieldCount += gate.size();
      for (String field : gate) {
        if (!api.contains(field)) {
          problems.add(name + "." + field + " : api 가 안 보내는 이름 - 항상 null 이 된다");
        }
      }
    }

    assertThat(checked).as("짝지은 레코드가 너무 적다 - 파서가 무력하다").isGreaterThanOrEqualTo(15);
    assertThat(fieldCount).as("필드를 거의 못 뽑았다 - 파서가 무력하다").isGreaterThanOrEqualTo(100);
    assertThat(problems).as("게이트만 아는 이름은 요청이 200 이어도 값이 안 온다").isEmpty();
  }
}
