package net.luversof.web.gate.stock.view;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 조각을 렌더해 <b>화면에 보이는 글자</b>에 프로그래머의 값이 새지 않는지 본다.
 *
 * <p>{@code EmptyRowsRenderTest} 는 "예외 없이 그려지는가" 만 본다. 예외 없이 {@code null} 이나 {@code NaN} 을 그대로 찍어도
 * 통과한다 &mdash; JTE 는 {@code ${someBigDecimal}} 이 null 이면 문자열 "null" 을 출력한다. 여기서는 그려 낸 결과에서
 * 스크립트/스타일/주석/태그를 걷어 내고 <b>남은 텍스트</b>만 본다.
 *
 * <p>실측 2026-08-24: 조각 52 개가 그려지고 금지 글자는 0 건이다.
 *
 * <p><b>무엇을 못 잡는지</b> &mdash; JTE 는 {@code ${someValue}} 가 null 이면 <b>빈 문자열</b>을 낸다. "null" 이라고 찍지
 * 않는다(주입해서 확인했다). 그래서 이 검사가 잡는 것은 <b>문자열로 바꿔 버린</b> 경우다: {@code String.valueOf(x)}, 문자열 이어붙이기,
 * {@code MessageFormat} 미치환({@code {0}}), 그리고 {@code NaN}/{@code Infinity} 같은 실수 연산 결과. 이 셋은 주입해서
 * 실제로 잡히는 것을 확인했다.
 *
 * <p>덤으로 확인한 것 &mdash; 금액 성분을 전부 null 로 채우면 {@code dividendYieldAnalytics.jte} 가 {@code
 * IllegalArgumentException: Cannot format given Object as a Number} 를 낸다.
 * {@code @if(periodPrincipalDelta != null)} 하나로 감싼 블록 안에서 {@code periodStartPrincipal} 과 {@code
 * periodEndPrincipal} 을 가드 없이 포맷하기 때문이다. <b>지금은 도달할 수 없다</b> &mdash; 컨트롤러가 delta 를 start/end 가 둘 다
 * 있을 때만 채운다(확인함). 템플릿이 컨트롤러의 그 성질에 기대고 있다는 뜻이라 여기 적어 둔다.
 */
class RenderedTextSanityTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte/stock");

  private static final Object NULL_STATE = new Object();

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private Object emptyValueFor(String type) {
    String t = type.trim();
    if (t.startsWith("List<") || t.startsWith("java.util.List<")) return List.of();
    if (t.startsWith("Set<") || t.startsWith("java.util.Set<")) return java.util.Set.of();
    if (t.startsWith("Map<") || t.startsWith("java.util.Map<")) return Map.of();
    if (t.startsWith("java.util.function.Function<"))
      return (java.util.function.Function<Object, String>) v -> "";
    Object simple =
        switch (t) {
          case "String" -> "";
          case "int" -> 0;
          case "long" -> 0L;
          case "double" -> 0.0d;
          case "boolean" -> false;
          case "BigDecimal", "java.math.BigDecimal" -> BigDecimal.ZERO;
          case "DecimalFormat", "java.text.DecimalFormat" -> new DecimalFormat("#,##0");
          case "ZoneId", "java.time.ZoneId" -> ZoneId.of("Asia/Seoul");
          case "UUID",
              "java.util.UUID",
              "Instant",
              "java.time.Instant",
              "LocalDate",
              "java.time.LocalDate",
              "Object" ->
              NULL_STATE;
          default -> null;
        };
    if (simple != null) return simple;
    if (t.endsWith("Pagination")) {
      return new net.luversof.web.common.menu.domain.Pagination(
          new org.springframework.data.domain.PageImpl<>(
              List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
    }
    try {
      Class<?> resolved = resolveType(t);
      if (resolved != null && resolved.isRecord()) return emptyRecord(resolved);
    } catch (Exception ignore) {
      return null;
    }
    return null;
  }

  private Class<?> resolveType(String simpleName) {
    for (String p :
        List.of(
            "net.luversof.web.gate.stock.dto.view.",
            "net.luversof.web.gate.stock.dto.response.",
            "net.luversof.web.gate.stock.domain.")) {
      try {
        return Class.forName(p + simpleName);
      } catch (ClassNotFoundException ignore) {
        // 다음 패키지
      }
    }
    return null;
  }

  private Object emptyRecord(Class<?> recordType) throws Exception {
    var components = recordType.getRecordComponents();
    Class<?>[] types = new Class<?>[components.length];
    Object[] values = new Object[components.length];
    for (int i = 0; i < components.length; i++) {
      types[i] = components[i].getType();
      values[i] = emptyForClass(components[i].getType());
    }
    var c = recordType.getDeclaredConstructor(types);
    c.setAccessible(true);
    return c.newInstance(values);
  }

  private Object emptyForClass(Class<?> type) {
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0.0d;
    if (type == boolean.class) return false;
    if (type == String.class) return "";
    if (type == BigDecimal.class) return BigDecimal.ZERO;
    if (List.class.isAssignableFrom(type)) return List.of();
    return null;
  }

  /** open 으로 시작해 close 로 끝나는 구간을 전부 지운다. */
  private static String stripBlocks(String text, String open, String close) {
    StringBuilder kept = new StringBuilder();
    int at = 0;
    while (true) {
      int start = text.indexOf(open, at);
      if (start < 0) {
        kept.append(text, at, text.length());
        return kept.toString();
      }
      kept.append(text, at, start).append(' ');
      int end = text.indexOf(close, start);
      if (end < 0) {
        return kept.toString();
      }
      at = end + close.length();
    }
  }

  /** 태그를 지우고 텍스트만 남긴다. */
  private static String stripTags(String text) {
    StringBuilder kept = new StringBuilder();
    boolean inside = false;
    for (int index = 0; index < text.length(); index++) {
      char c = text.charAt(index);
      if (c == '<') {
        inside = true;
        kept.append(' ');
      } else if (c == '>') {
        inside = false;
      } else if (!inside) {
        kept.append(c);
      }
    }
    return kept.toString();
  }

  @Test
  void 렌더_결과를_훑는다() throws IOException {
    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> report = new ArrayList<>();
    int rendered = 0;
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Map<String, Object> params = new HashMap<>();
        boolean ok = true;
        for (String line : lines) {
          if (!line.startsWith("@param ") || line.contains("=")) continue;
          String decl = line.substring("@param ".length()).trim();
          int split = decl.lastIndexOf(' ');
          Object value = emptyValueFor(decl.substring(0, split));
          if (value == null) {
            ok = false;
            break;
          }
          params.put(decl.substring(split + 1), value == NULL_STATE ? null : value);
        }
        if (!ok) continue;
        String name =
            JTE_ROOT
                .getParent()
                .relativize(file)
                .toString()
                .replace(java.io.File.separatorChar, '/');
        StringOutput out = new StringOutput();
        try {
          engine.render(name, params, out);
        } catch (Exception ex) {
          StringBuilder detail = new StringBuilder("THROW " + name);
          for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            detail
                .append(" -> ")
                .append(cause.getClass().getSimpleName())
                .append(": ")
                .append(cause.getMessage());
          }
          report.add(detail.toString());
          continue;
        }
        rendered++;
        String html = out.toString();
        // 스크립트/스타일/주석은 사용자에게 보이지 않는다. 걷어 내고 남은 것만 본다.
        // 정규식을 쓰지 않는다 - 이 파일을 쓰며 역슬래시 이스케이프를 또 틀렸다.
        html = stripBlocks(html, "<script", "</script>");
        html = stripBlocks(html, "<style", "</style>");
        html = stripBlocks(html, "<!--", "-->");
        // 속성값도 화면 글자가 아니다. 태그를 통째로 지우고 남은 텍스트만 본다.
        html = stripTags(html);
        for (String token : List.of("null", "NaN", "Infinity", "undefined", "{0}", "@param")) {
          int at = html.indexOf(token);
          while (at >= 0) {
            int from = Math.max(0, at - 60);
            int to = Math.min(html.length(), at + 60);
            String snippet = html.substring(from, to).replaceAll("\\s+", " ");
            report.add(name + " | " + token + " | ..." + snippet + "...");
            at = html.indexOf(token, at + 1);
            if (report.size() > 400) break;
          }
        }
      }
    }
    // 스캔이 조용히 0 건이 되면 이 검사는 늘 통과한다.
    org.assertj.core.api.Assertions.assertThat(rendered)
        .as("조각을 하나도 그리지 못했다 - 검사가 무력해진다")
        .isGreaterThanOrEqualTo(45);
    org.assertj.core.api.Assertions.assertThat(report)
        .as("화면 글자에 프로그래머의 값이 샜다(스크립트/속성이 아니라 눈에 보이는 자리다)")
        .isEmpty();
  }
}
