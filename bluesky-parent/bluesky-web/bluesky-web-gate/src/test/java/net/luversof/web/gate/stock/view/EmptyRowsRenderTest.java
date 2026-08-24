package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

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
 * 행이 하나도 없을 때 데이터 화면 조각이 깨지지 않는지 본다.
 *
 * <p>{@code EmptyDataRenderTest} 는 파라미터가 전부 기본값인 조각만 봤다. 여기서는 <b>파라미터를 요구하는</b> 조각을 빈 값으로 채워 그린다
 * &mdash; 목록은 빈 리스트, 문자열은 빈 문자열, 금액은 0. 필터 결과가 없거나 그 기간에 거래가 없으면 화면이 정확히 그 상태가 된다.
 *
 * <p>null 을 넣지 않는 이유: 호출부가 늘 값을 주는 자리에 null 을 넣으면 "일어나지 않는 실패" 를 잡게 된다. 여기서 잡고 싶은 것은 <b>실제로
 * 일어나는</b> 빈 상태다.
 *
 * <p>실측 2026-08-24: 필수 파라미터가 있는 조각 <b>25 개 전부</b>를 채워 그린다. UUID·Instant·LocalDate 는 null 이 곧 "고르지
 * 않음 / 기간 없음" 이라 그대로 넣고, Pagination 은 결과 0 건인 1 페이지, 포맷터(Function)는 빈 문자열을 돌려주는 함수, 레코드는 성분을 빈 값으로
 * 채워 만든다.
 *
 * <p>이 검사로 실제 결함을 하나 찾았다 &mdash; {@code activityList.jte} 의 {@code @param long buyCount = 0} 이
 * {@code ClassCastException: Integer cannot be cast to Long} 을 냈다. 기본값 {@code 0} 이 Integer 로 박싱되기
 * 때문이고, 운영에서도 JTE 는 모델을 Map 으로 받으므로 컨트롤러가 그 키를 빠뜨린 경로는 페이지 전체가 500 이 된다. 같은 형태가 4 개 파일 11 곳에 있었고 모두
 * {@code 0L} 로 고쳤다.
 */
class EmptyRowsRenderTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte/stock");

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

  /** 타입 이름에 맞는 "비어 있음" 값. 만들 수 없으면 null 을 돌려주고, 그런 조각은 대상에서 뺀다. */
  private Object emptyValueFor(String type) {
    String trimmed = type.trim();
    if (trimmed.startsWith("List<") || trimmed.startsWith("java.util.List<")) {
      return List.of();
    }
    if (trimmed.startsWith("Set<") || trimmed.startsWith("java.util.Set<")) {
      return java.util.Set.of();
    }
    if (trimmed.startsWith("Map<") || trimmed.startsWith("java.util.Map<")) {
      return Map.of();
    }
    if (trimmed.startsWith("java.util.function.Function<")) {
      // 화면이 값을 문자열로 바꿀 때 쓰는 포맷터. 빈 문자열을 돌려주는 것으로 충분하다.
      return (java.util.function.Function<Object, String>) value -> "";
    }
    Object simple =
        switch (trimmed) {
          case "String" -> "";
          case "int" -> 0;
          case "long" -> 0L;
          case "double" -> 0.0d;
          case "boolean" -> false;
          case "BigDecimal", "java.math.BigDecimal" -> BigDecimal.ZERO;
          case "DecimalFormat", "java.text.DecimalFormat" -> new DecimalFormat("#,##0");
          case "ZoneId", "java.time.ZoneId" -> ZoneId.of("Asia/Seoul");
          // 아래 넷은 "고르지 않음 / 기간 없음" 이 실제 화면 상태다. null 이 그 상태를 나타낸다.
          case "UUID", "java.util.UUID" -> NULL_IS_THE_EMPTY_STATE;
          case "Instant", "java.time.Instant" -> NULL_IS_THE_EMPTY_STATE;
          case "LocalDate", "java.time.LocalDate" -> NULL_IS_THE_EMPTY_STATE;
          case "Object" -> NULL_IS_THE_EMPTY_STATE;
          default -> null;
        };
    if (simple != null) {
      return simple;
    }
    return complexEmptyValue(trimmed);
  }

  /** null 자체가 "비어 있음" 인 타입을 표시하는 표식. 맵에는 진짜 null 로 넣는다. */
  private static final Object NULL_IS_THE_EMPTY_STATE = new Object();

  /** 빈 상태를 만들 수 있는 복합 타입. 못 만들면 null 을 돌려주고 그 조각은 대상에서 빠진다. */
  private Object complexEmptyValue(String type) {
    if (type.endsWith("Pagination")) {
      // 생성자가 Page 만 받는다. 결과가 0 건인 1 페이지가 곧 "결과 없음" 상태다.
      //
      // Page.empty() 는 쓸 수 없다 - unpaged 라 getPageNumber() 가 UnsupportedOperationException 을
      // 던진다(실측). 컨트롤러는 늘 페이지가 지정된 Page 를 주므로 여기서도 그렇게 만든다.
      return new net.luversof.web.common.menu.domain.Pagination(
          new org.springframework.data.domain.PageImpl<>(
              List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
    }
    try {
      Class<?> resolved = resolveType(type);
      if (resolved != null && resolved.isRecord()) {
        return emptyRecord(resolved);
      }
    } catch (Exception ignore) {
      return null;
    }
    return null;
  }

  private Class<?> resolveType(String simpleName) {
    for (String packageName :
        List.of(
            "net.luversof.web.gate.stock.dto.view.",
            "net.luversof.web.gate.stock.dto.response.",
            "net.luversof.web.gate.stock.domain.")) {
      try {
        return Class.forName(packageName + simpleName);
      } catch (ClassNotFoundException ignore) {
        // 다음 패키지를 본다.
      }
    }
    return null;
  }

  /** 레코드의 모든 성분을 빈 값으로 채워 만든다. 숫자는 0, 문자열은 빈 문자열, 나머지는 null. */
  private Object emptyRecord(Class<?> recordType) throws Exception {
    var components = recordType.getRecordComponents();
    Class<?>[] types = new Class<?>[components.length];
    Object[] values = new Object[components.length];
    for (int index = 0; index < components.length; index++) {
      Class<?> componentType = components[index].getType();
      types[index] = componentType;
      values[index] = emptyForClass(componentType);
    }
    var constructor = recordType.getDeclaredConstructor(types);
    constructor.setAccessible(true);
    return constructor.newInstance(values);
  }

  private Object emptyForClass(Class<?> type) {
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0.0d;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == String.class) {
      return "";
    }
    if (type == BigDecimal.class) {
      return BigDecimal.ZERO;
    }
    if (List.class.isAssignableFrom(type)) {
      return List.of();
    }
    return null;
  }

  private record Fillable(String template, Map<String, Object> params) {}

  private List<Fillable> fillableTemplates() throws IOException {
    List<Fillable> fillable = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        List<String> required =
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith("@param "))
                .filter(line -> !line.contains("="))
                .map(line -> line.substring("@param ".length()).trim())
                .toList();
        if (required.isEmpty()) {
          continue;
        }
        Map<String, Object> params = new HashMap<>();
        boolean allFillable = true;
        for (String declaration : required) {
          int split = declaration.lastIndexOf(' ');
          String type = declaration.substring(0, split);
          String name = declaration.substring(split + 1);
          Object value = emptyValueFor(type);
          if (value == null) {
            allFillable = false;
            break;
          }
          params.put(name, value == NULL_IS_THE_EMPTY_STATE ? null : value);
        }
        if (allFillable) {
          fillable.add(
              new Fillable(
                  JTE_ROOT
                      .getParent()
                      .relativize(file)
                      .toString()
                      .replace(java.io.File.separatorChar, '/'),
                  params));
        }
      }
    }
    return fillable;
  }

  @Test
  void 행이_없어도_데이터_조각이_예외를_내지_않는다() throws IOException {
    List<Fillable> templates = fillableTemplates();
    // 실측 2026-08-24: 필수 파라미터가 있는 조각 25 개 전부를 빈 값으로 채울 수 있다.
    // 하한을 두는 이유: 값 생성기가 한 타입을 못 만들면 그 조각이 조용히 대상에서 빠진다.
    assertThat(templates).hasSizeGreaterThanOrEqualTo(25);

    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> failures = new ArrayList<>();
    for (Fillable target : templates) {
      try {
        engine.render(target.template(), target.params(), new StringOutput());
      } catch (Exception ex) {
        // 원인 사슬까지 적는다. JTE 는 겉으로 TemplateException 만 던져서 무엇이 터졌는지 알 수 없다.
        StringBuilder detail = new StringBuilder(target.template());
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
          detail
              .append(" -> ")
              .append(cause.getClass().getSimpleName())
              .append(": ")
              .append(cause.getMessage());
        }
        failures.add(detail.toString());
      }
    }

    assertThat(failures).as("행이 없을 때 조각이 깨지면 페이지 전체가 500 이 된다").isEmpty();
  }
}
