package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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
 * 자산 성장의 '핵심 보유 제외' 토글이 <b>그려지는지</b>.
 *
 * <p>어느 종목이 핵심인지는 종목의 '핵심' 태그가 정한다(2026-09-08 에 평가액 1 위 자동 판정에서 바꿨다). 태그를 단 종목이 없으면 토글 자체가 없어야 하고,
 * 태그가 있으면 한 종목일 때는 이름을, 여럿일 때는 개수를 적어야 한다. 문자열만 보는 검사는 이 분기를 지나치므로 실제로 렌더해서 본다.
 */
class CoreHoldingToggleRenderTest {

  private static final String TEMPLATE = "stock/htmx/asset-growth.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String render(Map<String, Object> extra) {
    Map<String, Object> model = new HashMap<>();
    model.put("timeSeries", null);
    model.put("rangeMode", "all");
    model.putAll(extra);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 태그를 단 종목이 없으면 토글이 아예 없다 - 실측 2026-09-08 의 현재 상태다. */
  @Test
  void 태그를_단_종목이_없으면_토글이_없다() {
    assertThat(render(Map.of())).doesNotContain("data-exclude-core");
  }

  /** 한 종목이면 그 이름으로 부른다("삼성전자 제외"). */
  @Test
  void 핵심이_한_종목이면_이름으로_부른다() {
    String html =
        render(
            Map.of(
                "coreHoldingTagged",
                true,
                "coreHoldingName",
                "삼성전자",
                "coreHoldingCount",
                1,
                "coreHoldingWeightPct",
                "83.9"));

    assertThat(html)
        .contains("data-exclude-core=\"available\"")
        .contains("data-exclude-core-apply");
    assertThat(html).as("이름으로 부른다").contains("삼성전자");
    assertThat(html).as("비중을 곁들여 왜 빼는지 알린다").contains("83.9");
    assertThat(html)
        .as("아직 제외 중이 아니라 hidden 이 없다")
        .doesNotContain("name=\"excludeCore\" value=\"true\"");
  }

  /** 여럿이면 개수로 부른다 - 이름을 늘어놓으면 버튼이 길어진다. */
  @Test
  void 핵심이_여럿이면_개수로_부른다() {
    String html =
        render(
            Map.of(
                "coreHoldingTagged", true, "coreHoldingCount", 3, "coreHoldingWeightPct", "70.0"));

    assertThat(html).contains("data-exclude-core-apply");
    assertThat(html).as("개수를 적는다").containsPattern("(?s)data-exclude-core-apply.{0,600}3");
  }

  /** 제외 중이면 해제 버튼과 hidden 이 함께 나온다 - hidden 이 있어야 기간을 바꿔도 제외가 유지된다. */
  @Test
  void 제외_중이면_해제_버튼과_hidden_을_낸다() {
    String html =
        render(
            Map.of(
                "coreHoldingTagged",
                true,
                "coreHoldingExcluded",
                true,
                "coreHoldingName",
                "삼성전자",
                "coreHoldingCount",
                1));

    assertThat(html).contains("data-exclude-core=\"active\"").contains("data-exclude-core-clear");
    assertThat(html)
        .as("기간을 바꿔도 제외가 유지되려면 폼에 hidden 이 있어야 한다")
        .contains("<input type=\"hidden\" name=\"excludeCore\" value=\"true\">");
    assertThat(html).doesNotContain("data-exclude-core-apply");
  }

  /** 제외 중이면 그 종목이 스냅샷에서 빠져 비중을 낼 수 없다 - 비중 없이도 그려져야 한다. */
  @Test
  void 비중을_모르면_비중_없이_그린다() {
    String html =
        render(Map.of("coreHoldingTagged", true, "coreHoldingName", "삼성전자", "coreHoldingCount", 1));

    assertThat(html).contains("data-exclude-core-apply");
  }
}
