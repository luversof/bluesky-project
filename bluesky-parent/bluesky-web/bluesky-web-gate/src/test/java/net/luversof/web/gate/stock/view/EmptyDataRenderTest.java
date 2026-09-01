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
 * 데이터가 하나도 없을 때도 화면이 그려지는지 본다.
 *
 * <p>신규 사용자, 필터 결과가 빈 경우, 백엔드가 빈 목록을 준 경우가 모두 이 상태다. 여기서 예외가 나면 화면 조각이 아니라 <b>페이지 전체가 500</b> 이 된다
 * &mdash; 이 앱은 실제로 그런 사고를 겪었다(선컴파일 산출물이 없어 뷰 resolve 가 실패했고 JSON 으로 폴백됐다).
 *
 * <p>검사 대상은 <b>@param 이 전부 기본값인 조각</b>이다. 그런 조각은 파라미터 없이 그릴 수 있고, 그 결과가 곧 "데이터 없음" 화면이다. 실측
 * 2026-08-23: 주식 조각 49 개 중 24 개가 여기 해당한다(나머지는 필수 파라미터가 있어 호출부가 값을 준다).
 *
 * <p>API 쪽에는 이미 "신규 사용자 계약" 불변식이 있는데(전부 200 · 개인 데이터는 비어 있음) 화면 쪽에는 같은 검사가 없었다.
 */
class EmptyDataRenderTest {

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

  /** {@code @param} 이 전부 기본값을 가진 조각의 템플릿 이름. */
  private List<String> templatesRenderableWithoutParameters() throws IOException {
    List<String> names = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        List<String> params =
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith("@param "))
                .toList();
        if (params.isEmpty() || params.stream().anyMatch(line -> !line.contains("="))) {
          continue;
        }
        names.add(JTE_ROOT.getParent().relativize(file).toString().replace('\\', '/'));
      }
    }
    return names;
  }

  /**
   * 데이터가 없으면 <b>일부러</b> 아무것도 그리지 않는 조각.
   *
   * <p>처음엔 "본문이 비면 실패" 로 뒀다가 이 셋이 걸렸다. 확인해 보니 셋 다 의도된 조건부다 &mdash; 비로그인이면 관리 버튼을 감추고
   * ({@code @if(isAuthenticated)}), 연차가 없으면 표를 감추고, 필터가 없으면 배지를 감춘다. 비는 것이 옳으므로 기준을 고쳤다.
   *
   * <p>그래도 목록으로 남긴다. 여기 없는 조각이 갑자기 비면 그건 이유가 있는 것이다.
   */
  private static final List<String> INTENTIONALLY_BLANK_WITHOUT_DATA =
      List.of(
          "stock/htmx/fragments/adminActions.jte",
          "stock/htmx/fragments/assetGrowthYearlySummary.jte",
          // 구간이 하나뿐이면 바로 위의 '합산 손익' 을 그대로 되풀이할 뿐이라 그리지 않는다.
          // 같은 값을 두 번 적으면 읽는 사람이 둘을 견주려다 시간을 쓴다.
          "stock/htmx/fragments/stockItemPeriodBreakdown.jte",
          // 갈 곳이 하나뿐(=지금 보는 것)이거나 없으면 전환기가 할 일이 없다.
          "stock/htmx/fragments/detailNavSwitcher.jte",
          "stock/htmx/fragments/components/filterBadge.jte");

  @Test
  void 데이터가_없어도_조각이_예외를_내지_않는다() throws IOException {
    List<String> templates = templatesRenderableWithoutParameters();
    // 실측 2026-08-23: 24 개. 하한을 둬야 목록이 비어도 통과하는 일이 없다.
    assertThat(templates).hasSizeGreaterThanOrEqualTo(20);

    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> failures = new ArrayList<>();
    for (String template : templates) {
      try {
        engine.render(template, new HashMap<String, Object>(), new StringOutput());
      } catch (Exception ex) {
        failures.add(template + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
      }
    }

    assertThat(failures).as("데이터가 없을 때 조각이 깨지면 조각이 아니라 페이지 전체가 500 이 된다").isEmpty();
  }

  /** 비는 것이 정상인 조각 말고는 실제로 무언가를 그리는지. 전부 빈 문자열이면 위 검사는 아무것도 지키지 못한다. */
  @Test
  void 비는_것이_정상인_조각_말고는_무언가를_그린다() throws IOException {
    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    List<String> unexpectedlyBlank = new ArrayList<>();
    int rendered = 0;
    for (String template : templatesRenderableWithoutParameters()) {
      StringOutput output = new StringOutput();
      engine.render(template, new HashMap<String, Object>(), output);
      if (output.toString().isBlank()) {
        if (!INTENTIONALLY_BLANK_WITHOUT_DATA.contains(template)) {
          unexpectedlyBlank.add(template);
        }
        continue;
      }
      rendered++;
    }

    assertThat(unexpectedlyBlank)
        .as("데이터가 없을 때 조용히 빈 화면이 되는 조각이 늘었다 - 의도한 것이면 목록에 근거와 함께 넣을 것")
        .isEmpty();
    // 실측 2026-08-23: 24 개 중 21 개가 무언가를 그린다.
    assertThat(rendered).as("아무 조각도 그리지 못했다 - 검사가 무력하다").isGreaterThanOrEqualTo(18);
  }
}
