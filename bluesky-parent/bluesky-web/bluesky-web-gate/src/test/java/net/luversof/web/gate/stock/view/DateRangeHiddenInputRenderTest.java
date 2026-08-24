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
 * 기간 선택이 붙는 화면이 <b>숨은 입력 네 개</b>를 함께 그리는지 본다.
 *
 * <p>기간은 화면에서 날짜로 고르지만 서버에는 instant 로 보내야 한다(타임존이 달라지면 하루가 어긋난다). 그래서 각 폼은 {@code
 * …StartInstantInput} / {@code …EndInstantInput} / {@code …TimeZoneInput} / {@code …RangeModeInput}
 * 네 개를 숨은 입력으로 함께 실어 보낸다. 하나라도 빠지면 폼이 그 값 없이 제출되고 서버는 기본 기간으로 되돌아간다 &mdash; 화면은 멀쩡해 보이는데 사용자가 고른
 * 기간이 조용히 무시된다.
 *
 * <p>예전에는 {@code dev-scripts/check-date-range-fragments.ps1} 이 떠 있는 서버에 물어 확인했는데, 두 가지 이유로 무력했다.
 * 첫째, 로그인 쿠키를 넘겨야만 조각을 볼 수 있어 사실상 아무도 돌리지 않았다. 둘째, 대상 주소 네 개 중 <b>두 개가 실재하지 않았다</b>(실측 2026-08-24:
 * {@code /stock/htmx/realized-profit} 과 {@code /stock/htmx/trade-list} 가 404. 실제 주소는 {@code
 * /stock/realized-profit} 과 {@code /stock/htmx/trade/list} 다). 스크립트는 실패해도 종료 코드를 내지 않아 그 사실도 드러나지
 * 않았다.
 *
 * <p>그래서 서버 대신 <b>조각을 그려서</b> 확인한다. 로그인이 필요 없고 빌드에서 돈다.
 *
 * <p>실측 2026-08-24: 아래 여섯 조각이 네 개를 모두 그린다.
 */
class DateRangeHiddenInputRenderTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");

  private static final Object NULL_STATE = new Object();

  /** 기간 선택을 실어 보내는 조각. 파일이 옮겨지면 렌더에서 바로 드러난다. */
  private static final List<String> TEMPLATES =
      List.of(
          "_layout/stockLayout.jte",
          "stock/htmx/asset-growth.jte",
          "stock/htmx/fragments/activityList.jte",
          "stock/htmx/fragments/components/detailDateFilter.jte",
          "stock/htmx/fragments/dividend/dividendSearchForm.jte",
          "stock/htmx/fragments/trade/tradeSearchForm.jte");

  private static final List<String> REQUIRED_SUFFIXES =
      List.of("StartInstantInput", "EndInstantInput", "TimeZoneInput", "RangeModeInput");

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
    if (t.startsWith("List<") || t.startsWith("java.util.List<")) {
      return List.of();
    }
    if (t.startsWith("Set<") || t.startsWith("java.util.Set<")) {
      return java.util.Set.of();
    }
    if (t.startsWith("Map<") || t.startsWith("java.util.Map<")) {
      return Map.of();
    }
    if (t.startsWith("java.util.function.Function<")) {
      return (java.util.function.Function<Object, String>) value -> "";
    }
    // 레이아웃은 본문을 Content 로 받는다. 여기서 보고 싶은 것은 레이아웃이 스스로 그리는 숨은 입력이라
    // 본문은 비워도 된다.
    if (t.equals("gg.jte.Content") || t.equals("Content")) {
      return (gg.jte.Content) output -> {};
    }
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
    if (simple != null) {
      return simple;
    }
    if (t.endsWith("Pagination")) {
      return new net.luversof.web.common.menu.domain.Pagination(
          new org.springframework.data.domain.PageImpl<>(
              List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
    }
    try {
      Class<?> resolved = resolveType(t);
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

  private Object emptyRecord(Class<?> recordType) throws Exception {
    var components = recordType.getRecordComponents();
    Class<?>[] types = new Class<?>[components.length];
    Object[] values = new Object[components.length];
    for (int index = 0; index < components.length; index++) {
      types[index] = components[index].getType();
      values[index] = emptyForClass(components[index].getType());
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

  private Map<String, Object> paramsFor(String template) throws IOException {
    Map<String, Object> params = new HashMap<>();
    for (String line : Files.readAllLines(JTE_ROOT.resolve(template), StandardCharsets.UTF_8)) {
      if (!line.startsWith("@param ") || line.contains("=")) {
        continue;
      }
      String declaration = line.substring("@param ".length()).trim();
      int split = declaration.lastIndexOf(' ');
      Object value = emptyValueFor(declaration.substring(0, split));
      if (value == null) {
        return null;
      }
      params.put(declaration.substring(split + 1), value == NULL_STATE ? null : value);
    }
    return params;
  }

  @Test
  void 기간_선택_조각은_숨은_입력_네_개를_함께_그린다() throws IOException {
    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> failures = new ArrayList<>();
    int rendered = 0;

    for (String template : TEMPLATES) {
      Map<String, Object> params = paramsFor(template);
      if (params == null) {
        failures.add(template + " — 파라미터를 채우지 못해 그리지 못했다(값 생성기를 늘릴 것)");
        continue;
      }
      StringOutput output = new StringOutput();
      try {
        engine.render(template, params, output);
      } catch (Exception ex) {
        failures.add(template + " — 렌더 실패: " + ex);
        continue;
      }
      rendered++;
      String html = output.toString();
      for (String suffix : REQUIRED_SUFFIXES) {
        if (!html.contains(suffix + "\"")) {
          failures.add(template + " — " + suffix + " 이 없다");
        }
      }
    }

    // 실패 목록을 먼저 본다 - 렌더 수부터 단언하면 어느 조각이 문제인지 메시지에 안 남는다.
    assertThat(failures).as("숨은 입력이 빠지면 사용자가 고른 기간이 조용히 무시된다").isEmpty();
    assertThat(rendered).as("조각을 하나도 그리지 못했다 - 검사가 무력해진다").isEqualTo(TEMPLATES.size());
  }
}
