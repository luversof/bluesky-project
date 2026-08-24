package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * 렌더 검사 둘이 <b>모든</b> 조각을 덮는지 본다.
 *
 * <p>{@code EmptyDataRenderTest} 는 "{@code @param} 이 전부 기본값인 조각"을, {@code EmptyRowsRenderTest} 는
 * "필수 {@code @param} 이 있고 빈 값으로 채울 수 있는 조각"을 훑는다. 둘 다 자기 몫에 하한(≥20, ≥25)만 두므로, <b>어느 쪽 조건에도 걸리지 않는
 * 조각</b>은 조용히 아무 검사도 받지 않는다.
 *
 * <p>{@code @param} 이 하나도 없는 조각이 그렇다 &mdash; 두 검사 모두 시작하자마자 {@code continue} 한다. 실측 2026-08-24:
 * stock 조각 52 개 중 3 개가 그랬다({@code cardSkeleton}, {@code loadError}, {@code dividendCharts}). 하필
 * {@code loadError} 는 원격 호출이 실패했을 때 사용자가 보는 조각이라, 그것이 깨지면 실패 경로가 다시 실패한다.
 *
 * <p>여기서는 두 가지를 못박는다 &mdash; 빠진 조각을 실제로 그려 보고, 앞으로 어느 조각도 두 그물 밖으로 나가지 못하게 한다.
 */
class TemplateRenderCoverageTest {

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

  /** {@code @param} 이 하나도 없는 조각. 두 렌더 검사가 모두 건너뛴다. */
  private List<String> templatesWithoutAnyParameter() throws IOException {
    List<String> names = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        boolean hasParam =
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .anyMatch(line -> line.startsWith("@param "));
        if (!hasParam) {
          names.add(
              JTE_ROOT
                  .getParent()
                  .relativize(file)
                  .toString()
                  .replace(java.io.File.separatorChar, '/'));
        }
      }
    }
    return names;
  }

  @Test
  void 파라미터가_없는_조각도_예외를_내지_않는다() throws IOException {
    List<String> templates = templatesWithoutAnyParameter();
    assertThat(templates).as("대상을 하나도 못 찾았다 - 검사가 무력해진다").isNotEmpty();

    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> failures = new ArrayList<>();
    for (String template : templates) {
      try {
        engine.render(template, new HashMap<String, Object>(), new StringOutput());
      } catch (Exception ex) {
        Throwable cause = ex;
        StringBuilder chain = new StringBuilder(ex.getClass().getSimpleName());
        while (cause.getCause() != null) {
          cause = cause.getCause();
          chain.append(" -> ").append(cause.getClass().getSimpleName());
        }
        failures.add(template + " -> " + chain + ": " + cause.getMessage());
      }
    }

    assertThat(failures).as("조각이 깨지면 그 조각이 아니라 페이지 전체가 500 이 된다").isEmpty();
  }

  @Test
  void 파라미터가_없는_조각도_무언가를_그린다() throws IOException {
    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> blank = new ArrayList<>();
    for (String template : templatesWithoutAnyParameter()) {
      StringOutput output = new StringOutput();
      engine.render(template, new HashMap<String, Object>(), output);
      if (output.toString().isBlank()) {
        blank.add(template);
      }
    }

    assertThat(blank).as("전부 빈 문자열이면 위 검사는 아무것도 지키지 못한다").isEmpty();
  }

  /**
   * 모든 조각이 세 그물 중 하나에는 든다.
   *
   * <p>세 그물의 조건은 서로 배타적이고 합치면 전체다 &mdash; {@code @param} 없음 / 전부 기본값 / 필수 있음. 조건이 바뀌어 어느 쪽에도 들지 않는
   * 조각이 생기면 여기서 드러난다.
   */
  @Test
  void 모든_조각이_어느_한_그물에는_든다() throws IOException {
    List<String> uncovered = new ArrayList<>();
    int total = 0;
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        total++;
        List<String> params =
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith("@param "))
                .toList();
        boolean noParameter = params.isEmpty();
        boolean allDefaulted = !params.isEmpty() && params.stream().allMatch(l -> l.contains("="));
        boolean hasRequired = params.stream().anyMatch(l -> !l.contains("="));
        if (!noParameter && !allDefaulted && !hasRequired) {
          uncovered.add(file.toString());
        }
      }
    }

    assertThat(total).as("조각을 하나도 못 찾았다 - 검사가 무력해진다").isGreaterThanOrEqualTo(50);
    assertThat(uncovered).as("어느 렌더 검사에도 들지 않는 조각이 생겼다").isEmpty();
  }
}
