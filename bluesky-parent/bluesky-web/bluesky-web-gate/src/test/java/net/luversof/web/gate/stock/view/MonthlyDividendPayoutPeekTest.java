package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
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
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;

/**
 * 관리 &gt; 월배당 기준: 프로필 행 안에서 지급 이력 펼치기 &mdash; 사용자 요청 2026-09-29(A안). 예전에는 "이 종목 보기" 가 페이지를 다시 불러 맨
 * 위로 돌아갔고 지급 이력은 프로필 목록 아래 2,019px(1440px) · 2,542px(휴대폰)에 있었다.
 */
class MonthlyDividendPayoutPeekTest {

  private static final String PEEK = "stock/fragments/monthlyDividendPayoutPeek.jte";

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

  private static MonthlyDividendPayoutResponse payout(String recordDate, String amount) {
    LocalDate record = recordDate == null ? null : LocalDate.parse(recordDate);
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "481050",
        "종목",
        record,
        record == null ? null : record.plusDays(2),
        null,
        new BigDecimal(amount),
        BigDecimal.ONE,
        null);
  }

  private String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(PEEK, model, output);
    return output.toString();
  }

  /** 최근순(기준일 · 지급일)으로 자르고, 원본 순서에 기대지 않는다. 날짜 없는 행은 뒤. */
  @Test
  void 최근_지급_이력만_최근순으로() {
    List<MonthlyDividendPayoutResponse> all = new ArrayList<>();
    all.add(payout("2025-01-15", "1"));
    all.add(payout(null, "9"));
    for (int month = 2; month <= 12; month++) {
      all.add(payout(String.format("2025-%02d-15", month), String.valueOf(month)));
    }
    all.add(payout("2026-01-15", "13"));

    var recent = MonthlyDividendReferenceSupport.recentPayouts(all, 12);

    assertThat(recent).hasSize(12);
    assertThat(recent.get(0).recordDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    assertThat(recent.get(11).recordDate())
        .as("2025-01 과 날짜 없는 행이 잘린다")
        .isEqualTo(LocalDate.of(2025, 2, 15));
    assertThat(MonthlyDividendReferenceSupport.recentPayouts(null, 12)).isEmpty();
  }

  @Test
  void 조각은_표와_전체_건수와_전체_보기_닻을_싣는다() {
    Map<String, Object> model = new HashMap<>();
    model.put("peekSymbol", "481050");
    model.put("peekPayouts", List.of(payout("2026-09-15", "125"), payout("2026-08-14", "120")));
    model.put("peekTotalCount", 30);
    String html = render(model);

    assertThat(html.split("data-payout-peek-item", -1)).hasSize(2 + 1);
    assertThat(html).contains("125원").contains("2026-09-15");
    assertThat(html).contains("data-payout-peek-more").contains("30").contains("2");
    assertThat(html)
        .contains("/stock/admin?tab=monthly-reference&symbol=481050#monthly-payout-list");
    assertThat(html).doesNotContain("stock.page.dividend.monthly.reference.payout.peek");
  }

  @Test
  void 비었거나_실패하면_그렇게_말한다() {
    Map<String, Object> empty = new HashMap<>();
    empty.put("peekSymbol", "481050");
    assertThat(render(empty)).contains("data-payout-peek-empty").doesNotContain("<table");

    Map<String, Object> failed = new HashMap<>();
    failed.put("peekSymbol", "481050");
    failed.put("peekFailed", true);
    assertThat(render(failed)).contains("data-payout-peek-failed").contains("role=\"alert\"");
  }

  /** 프로필 행마다 펼치기 단추(조각 주소 · 펼침 상태 · 낭독기 이름)와 "편집"(폼 자리로 가는 닻)이 있고, 두 닻 자리가 화면에 있다. */
  @Test
  void 프로필_행에_펼치기와_편집이_있다() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);

    assertThat(source)
        .contains("data-payout-peek-toggle")
        .contains("data-peek-url=\"/stock/dividend/monthly-reference/payout/peek?symbol=")
        .contains("aria-expanded=\"false\"")
        .contains("aria-controls=\"payout-peek-${row.stockItemSymbol()}\"")
        .contains("#monthly-reference-editor\" class=\"btn btn-xs btn-outline\"")
        .contains("id=\"monthly-reference-editor\"")
        .contains("id=\"monthly-payout-list\"");
    assertThat(Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8))
        .contains(
            "<script type=\"module\" src=\"/js/stock/monthlyDividendPayoutPeek.js\"></script>");
    // 끌어서 순서 바꾸기는 "바로 다음 줄" 로 행을 옮긴다 - 펼친 줄이 끼어 있으면 엉뚱한 자리에 떨어진다.
    assertThat(
            Files.readString(
                Path.of("src/main/resources/static/js/stock/monthlyDividendPayoutPeek.js"),
                StandardCharsets.UTF_8))
        .contains("dragstart");
  }
}
