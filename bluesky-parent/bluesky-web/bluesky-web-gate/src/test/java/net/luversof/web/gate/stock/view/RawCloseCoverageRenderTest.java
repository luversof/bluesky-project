package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import net.luversof.web.gate.stock.dto.response.DataStatusResponse;

/**
 * 관리 화면 데이터 상태의 원주가 줄(2026-10-02). 평가액은 원주가 x 실제 주식 수라, 원주가가 빈 날은 직전 날 배율로 메운 추정값이 쓰인다 - 몇 날인지 보여야
 * 시세 갱신을 다시 할지 정할 수 있다. 실측: 보유 기간 13,665 일 중 빈 날 0.
 */
class RawCloseCoverageRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/adminActions.jte";

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

  private String render(Long dayCount, Long missingDayCount) {
    DataStatusResponse dataStatus =
        new DataStatusResponse(
            null,
            0L,
            null,
            0L,
            LocalDate.parse("2026-10-01"),
            86L,
            LocalDate.parse("2026-09-30"),
            9L,
            0L,
            0L,
            0L,
            57477L,
            1352L,
            9L,
            0L,
            0L,
            List.of(),
            null,
            List.of(),
            0L,
            List.of(),
            List.of(),
            dayCount,
            missingDayCount);
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("dataStatus", dataStatus);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 그 줄 안의 글자만 - 페이지 전체에서 숫자를 찾으면 아무 숫자나 걸린다. */
  private static String line(String html) {
    int at = html.indexOf("data-raw-close-missing=");
    if (at < 0) {
      return "";
    }
    return html.substring(html.indexOf('>', at) + 1, html.indexOf("</div>", at)).trim();
  }

  @Test
  void 빈_날이_없으면_날수만_적는다() {
    String text = line(render(13665L, 0L));
    assertThat(text).contains("13,665");
    assertThat(text)
        .as("빈 날이 없으면 메운 값 안내는 군더더기다")
        .doesNotContain(MessageUtil.getMessage("stock.admin.price.raw.missing"));
  }

  @Test
  void 빈_날이_있으면_추정값이라고_밝힌다() {
    String text = line(render(13665L, 3L));
    assertThat(text)
        .contains("13,665")
        .contains(MessageUtil.getMessage("stock.admin.price.raw.missing"));
  }

  @Test
  void 원주가_열이_없으면_줄을_그리지_않는다() {
    assertThat(render(null, null)).doesNotContain("data-raw-close-missing=");
  }
}
