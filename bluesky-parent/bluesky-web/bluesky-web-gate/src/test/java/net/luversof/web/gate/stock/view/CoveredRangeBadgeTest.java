package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
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
import net.luversof.web.gate.stock.util.StockCoveredRangeUtil;

/**
 * 기간 막대에서 <b>"전체"</b>를 골랐을 때도 어느 구간인지 알 수 있는지 본다.
 *
 * <p>"전체" 는 기간을 거는 것이 아니라 <b>거는 것을 그만두는 것</b>이라 시작·종료를 아예 보내지 않는다({@code date-range-picker.ts}:
 * {@code months === 0} 이면 둘 다 빈 문자열). 그래서 오른쪽 구간 배지가 그릴 날짜가 없어, 다른 프리셋에는 다 있는 표기가 "전체" 에서만 사라졌다.
 *
 * <p>그 구간이 몇 년치인지는 화면 어디에도 없었다 &mdash; 종목마다 첫 거래일이 달라서(실측 2026-08-31: 삼성전자 2020-03, 최근 산 ETF
 * 2026-05) 짐작할 수도 없다. 그래서 <b>조회된 자료의 구간</b>을 대신 적는다.
 */
class CoveredRangeBadgeTest {

  private static final String TEMPLATE = "stock/htmx/fragments/components/dateRangeNavBar.jte";
  private static final Path JTE_ROOT = Path.of("src/main/jte");
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  /** 이 배지를 그리는 화면 전부. 한 화면만 고치면 어느 화면에서 되는지를 외워야 한다. */
  private static final List<String> SCREENS =
      List.of(
          "stock/stockItemDetail.jte",
          "stock/accountDetail.jte",
          "stock/htmx/asset-growth.jte",
          "stock/htmx/tradeList.jte",
          "stock/htmx/fragments/activityList.jte",
          "stock/htmx/fragments/tabsDividendHistory.jte");

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

  private String render(Map<String, Object> extra) {
    Map<String, Object> model = new HashMap<>();
    model.put("pickerName", "testPicker");
    model.put("btnClass", "date-range-btn");
    model.put("canPrev", false);
    model.put("canNext", false);
    model.putAll(extra);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 전체를_고르면_조회된_자료의_구간을_적는다() {
    Map<String, Object> model = new HashMap<>();
    model.put("rangeMode", "all");
    // '전체' 는 시작·종료를 보내지 않는다.
    model.put("coveredStartLocalUnused", null);
    model.put("coveredStartDate", LocalDate.parse("2020-03-04"));
    model.put("coveredEndDate", LocalDate.parse("2026-08-31"));

    String html = render(model);

    assertThat(html)
        .as("전체만 구간 표기가 사라져 몇 년치인지 알 수 없었다")
        .contains("2020-03-04")
        .contains("2026-08-31")
        .contains(MessageUtil.getMessage("stock.label.period.all"));
  }

  /** 기간을 고른 경우는 예전 그대로 그 기간을 적는다. 조회된 자료 구간이 그 자리를 빼앗으면 안 된다. */
  @Test
  void 기간을_고르면_고른_기간을_그대로_적는다() {
    Map<String, Object> model = new HashMap<>();
    model.put("rangeMode", "3");
    model.put("startDate", LocalDate.parse("2026-06-01"));
    model.put("endDate", LocalDate.parse("2026-09-01"));
    model.put("coveredStartDate", LocalDate.parse("2020-03-04"));
    model.put("coveredEndDate", LocalDate.parse("2026-08-31"));

    String html = render(model);

    // 종료일은 배타적이라 하루를 빼서 찍는다(예전 규칙 그대로).
    assertThat(html).contains("2026-06-01").contains("2026-08-31");
    assertThat(html).as("고른 기간이 있는데 조회 구간을 함께 적으면 어느 것이 필터인지 알 수 없다").doesNotContain("2020-03-04");
  }

  /** 자료가 없으면 적을 구간도 없다. "? ~ ?" 같은 자리표시자를 그리면 안 된다. */
  @Test
  void 자료가_없으면_아무것도_적지_않는다() {
    Map<String, Object> model = new HashMap<>();
    model.put("rangeMode", "all");

    assertThat(render(model)).doesNotContain("data-covered-range");
  }

  @Test
  void 조회된_자료에서_처음과_끝을_뽑는다() {
    record Row(Instant at) {}
    List<Row> rows = new ArrayList<>();
    rows.add(new Row(LocalDate.parse("2026-05-19").atStartOfDay(KST).toInstant()));
    rows.add(new Row(LocalDate.parse("2020-03-04").atStartOfDay(KST).toInstant()));
    rows.add(new Row(null)); // 시각이 없는 행은 셀 수 없다
    rows.add(new Row(LocalDate.parse("2026-08-31").atStartOfDay(KST).toInstant()));

    var covered = StockCoveredRangeUtil.covered(rows, Row::at, KST);

    assertThat(covered.startDate()).isEqualTo(LocalDate.parse("2020-03-04"));
    assertThat(covered.endDate()).isEqualTo(LocalDate.parse("2026-08-31"));
  }

  @Test
  void 자료가_비면_빈_구간이다() {
    record Row(Instant at) {}
    assertThat(StockCoveredRangeUtil.covered(List.<Row>of(), Row::at, KST).isEmpty()).isTrue();
    assertThat(StockCoveredRangeUtil.covered(null, Row::at, KST).isEmpty()).isTrue();
  }

  /** 배지를 쓰는 화면 전부가 구간을 넘겨야 한다. 한 곳만 고치면 어디서 되는지를 외워야 한다. */
  @Test
  void 이_배지를_쓰는_화면_전부가_구간을_넘긴다() throws IOException {
    for (String page : SCREENS) {
      Path path = JTE_ROOT.resolve(page);
      assertThat(path).as("화면이 옮겨졌다: " + page).exists();
      assertThat(Files.readString(path, StandardCharsets.UTF_8))
          .as(page + " 이 조회 구간을 넘기지 않는다 - 그 화면만 '전체' 에서 구간 표기가 빈다")
          .contains("coveredStartDate");
    }
  }
}
