package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView;

/**
 * 과세표준을 모르는(null) 지급 이력은 "확인 안 됨" 으로 보여야 한다(사용자 요청 2026-10-07: "확인안된 경우 처리를 해야 다음에 갱신 시 갱신처리도 할수
 * 있을거 같네"). 실데이터에는 아직 모르는 행이 없어 화면으로 못 봤다 - 여기서 null 을 넣어 그려 본다. 0 원 · 0% 로 그리면 진짜 0(전액 비과세)과 구분되지
 * 않는다.
 */
class TaxableUnknownRenderTest {

  private static final String UNKNOWN = "확인 안 됨";

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

  private static String render(String template, Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  private static MonthlyDividendPayoutResponse payout(String recordDate, String taxable) {
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "0210E0",
        "PLUS 200커버드콜액티브",
        LocalDate.parse(recordDate),
        LocalDate.parse(recordDate).plusDays(2),
        null,
        new BigDecimal("152"),
        taxable == null ? null : new BigDecimal(taxable),
        Instant.parse("2026-10-08T00:00:00Z"));
  }

  /** 두 행(하나는 모름, 하나는 진짜 0)을 그린 표에서 "확인 안 됨" 이 한 번, 0 원이 한 번이어야 한다. */
  @Test
  void 지급_이력_미리보기는_모르는_과세표준을_확인_안_됨으로() {
    Map<String, Object> model = new HashMap<>();
    model.put("peekSymbol", "0210E0");
    model.put("peekPayouts", List.of(payout("2026-09-30", null), payout("2026-08-31", "0")));
    model.put("peekTotalCount", 2);
    String html = render("stock/fragments/monthlyDividendPayoutPeek.jte", model);

    assertThat(html).contains("data-taxable-unknown");
    assertThat(count(html, UNKNOWN)).as("모르는 행 하나").isEqualTo(1);
  }

  @Test
  void 관리_지급_이력_표와_요약_카드도_모르면_확인_안_됨() {
    Map<String, Object> model = new HashMap<>();
    model.put("selectedMonthlyDividendSymbol", "0210E0");
    model.put(
        "monthlyDividendPayouts", List.of(payout("2026-09-30", null), payout("2026-08-31", "0")));
    model.put("monthlyDividendPayoutExists", true);
    model.put(
        "monthlyDividendReferenceSummary",
        new MonthlyDividendReferenceSummaryView(
            "0210E0",
            2,
            new BigDecimal("152"),
            new BigDecimal("152"),
            null,
            2,
            0,
            LocalDate.parse("2026-09-30"),
            LocalDate.parse("2026-10-02")));
    String html = render("stock/fragments/monthlyDividendReference.jte", model);

    // 표의 모르는 행 하나 + 요약 카드(비중을 아는 행이 없다) 하나
    assertThat(count(html, UNKNOWN)).isGreaterThanOrEqualTo(2);
    assertThat(html)
        .as("요약 카드가 모르는 비중을 0.00% 로 그리면 안 된다")
        .doesNotContainPattern("tabular-nums\">\\s*0\\.00%</p>");
  }

  private static int count(String haystack, String needle) {
    int n = 0;
    for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
      n++;
    }
    return n;
  }
}
