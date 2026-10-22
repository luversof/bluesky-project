package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * 배당 내역의 전기 비교 카드에 "비교 기간 진행 중(N/M일)" 이 실제로 나오는가.
 *
 * <p>2026-10-21 정리 조사에서 드러났다: 컨트롤러는 경과일 · 총일수를 모델에 넣고 카드 조각도 표시 분기를 갖고 있었는데, 그 사이의 {@code
 * tabsDividendHistory.jte} 가 두 값을 받지도 넘기지도 않아 분기에 한 번도 닿지 않았다(판정 함수 시험 {@code InProgressPeriodTest}
 * 만 있었고 화면을 그리는 시험이 없었다). 판정이 아니라 <b>배선</b>을 본다.
 */
class InProgressPeriodRenderTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

  private static String render(int elapsedDays, int totalDays) {
    Map<String, Object> params = new HashMap<>();
    params.put("dividendList", List.of());
    params.put("totalItems", 0);
    params.put("accountList", List.of());
    params.put("stockItemList", List.of());
    params.put("selectedAccountId", null);
    params.put("selectedStockItemId", null);
    params.put("startDate", LocalDate.parse("2026-10-01").atStartOfDay(KST).toInstant());
    params.put("endDate", LocalDate.parse("2026-10-22").atStartOfDay(KST).toInstant());
    params.put("sort", null);
    params.put("totalGrossAmount", BigDecimal.ZERO);
    params.put("totalNetAmount", BigDecimal.ZERO);
    params.put("totalTax", BigDecimal.ZERO);
    params.put("totalTaxableAmount", BigDecimal.ZERO);
    params.put("rangeMode", "mtd");
    // 전기 값이 있어야 비교 카드가 나온다.
    params.put("prevPeriodNetAmount", new BigDecimal("120000"));
    params.put("prevStartDate", LocalDate.parse("2026-09-01"));
    params.put("prevEndDate", LocalDate.parse("2026-09-30"));
    params.put("currentPeriodElapsedDays", elapsedDays);
    params.put("currentPeriodTotalDays", totalDays);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/tabsDividendHistory.jte", params, output);
    return output.toString();
  }

  private static String note(int elapsedDays, int totalDays) {
    return MessageUtil.getMessage("stock.dividend.compare.period.in.progress")
        .replace("{0}", String.valueOf(elapsedDays))
        .replace("{1}", String.valueOf(totalDays));
  }

  @Test
  void 진행_중인_달이면_비교_카드에_경과일을_밝힌다() {
    assertThat(render(21, 31)).contains(note(21, 31));
  }

  @Test
  void 진행_중이_아니면_붙이지_않는다() {
    // 문구에서 숫자 앞까지만 본다 - 로케일마다 숫자 자리 모양이 다르다.
    String prefix =
        MessageUtil.getMessage("stock.dividend.compare.period.in.progress").split("\\{0\\}")[0];
    assertThat(prefix).isNotBlank();
    assertThat(render(0, 0)).doesNotContain(prefix);
  }
}
