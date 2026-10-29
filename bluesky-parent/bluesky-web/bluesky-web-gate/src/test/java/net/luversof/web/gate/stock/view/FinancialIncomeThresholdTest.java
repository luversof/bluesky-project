package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView;
import net.luversof.web.gate.stock.service.FinancialIncomeThresholdSupport;

/**
 * 배당 내역의 '올해 금융소득 종합과세 기준'(2026-10-20 자율 개선).
 *
 * <p>합은 과세 계좌의 과세표준이고(과세이연 계좌 · 미래 지급일 · 과세표준 빈 행은 빠진다), 연말 예상은 지금까지 + 최근 90 일 하루 평균 x 남은 날이다. 판정은
 * 순수 함수 {@link FinancialIncomeThresholdSupport#compute} 의 동작으로 본다(글자 대조 아님).
 */
class FinancialIncomeThresholdTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
  private static final UUID TAXABLE = UUID.randomUUID();
  private static final UUID ISA = UUID.randomUUID();

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

  private static DividendResponse dividend(UUID account, String payDay, String taxable) {
    return dividend(account, UUID.randomUUID(), payDay, taxable);
  }

  private static DividendResponse dividend(
      UUID account, UUID stockItem, String payDay, String taxable) {
    return new DividendResponse(
        UUID.randomUUID(),
        account,
        stockItem,
        "종목",
        "CASH",
        10,
        BigDecimal.TEN,
        null,
        new BigDecimal("100000"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        taxable == null ? null : new BigDecimal(taxable),
        new BigDecimal("100000"),
        null,
        LocalDate.parse(payDay).atStartOfDay(ZONE).toInstant());
  }

  private static FinancialIncomeThresholdView compute(List<DividendResponse> rows) {
    return FinancialIncomeThresholdSupport.compute(rows, Set.of(ISA), TODAY, ZONE);
  }

  /** 달마다 300,000 원(07-22 ~ 10-20, 30 일 간격) - 최근 90 일(07-23 ~ 10-20)엔 네 번. */
  private static List<DividendResponse> monthlyPayer(UUID item) {
    return List.of(
        dividend(TAXABLE, item, "2026-07-22", "300000"),
        dividend(TAXABLE, item, "2026-08-21", "300000"),
        dividend(TAXABLE, item, "2026-09-20", "300000"),
        dividend(TAXABLE, item, "2026-10-20", "300000"),
        dividend(TAXABLE, item, "2026-06-22", "300000"));
  }

  /** 분기 배당(삼성전자처럼 11 · 2 · 5 · 8 월) - 작년 11 월 2,000,000, 올해 8 월이 최근 1,800,000. */
  private static List<DividendResponse> quarterlyPayer(UUID item) {
    return List.of(
        dividend(TAXABLE, item, "2025-11-20", "2000000"),
        dividend(TAXABLE, item, "2026-02-20", "2000000"),
        dividend(TAXABLE, item, "2026-05-20", "2000000"),
        dividend(TAXABLE, item, "2026-08-20", "1800000"));
  }

  @Test
  void 올해_과세_계좌의_과세표준만_더하고_과세이연_미래_빈_행은_뺀다() {
    List<DividendResponse> rows = new ArrayList<>();
    rows.add(dividend(TAXABLE, "2026-01-02", "1000000"));
    rows.add(dividend(TAXABLE, "2026-10-20", "500000")); // 오늘 지급분은 넣는다
    rows.add(dividend(ISA, "2026-05-01", "9000000")); // 과세이연 계좌는 빠진다
    rows.add(dividend(TAXABLE, "2026-10-21", "700000")); // 미리 넣은 내일 지급분은 빠진다
    rows.add(dividend(TAXABLE, "2026-03-03", null)); // 과세표준 빈 행은 0 이 아니라 건수로
    rows.add(dividend(TAXABLE, "2025-12-31", "300000")); // 작년 끝 날은 작년으로

    FinancialIncomeThresholdView view = compute(rows);

    assertThat(view.year()).isEqualTo(2026);
    assertThat(view.ytdTaxable()).isEqualByComparingTo("1500000");
    assertThat(view.unknownTaxableCount()).isEqualTo(1);
    assertThat(view.lastYearTaxable()).isEqualByComparingTo("300000");
    assertThat(view.remaining()).isEqualByComparingTo("18500000");
    assertThat(view.exceeded()).isFalse();
    assertThat(view.usedPct()).isEqualByComparingTo("7.5");
  }

  @Test
  void 달마다_나오는_종목은_최근_90일_하루_평균_곱하기_남은_날() {
    FinancialIncomeThresholdView view = compute(monthlyPayer(UUID.randomUUID()));

    // 07-22 는 창 밖(최근 90 일 = 07-23 ~ 10-20), 08-21 · 09-20 · 10-20 세 번 = 900,000.
    assertThat(view.recentTaxable()).isEqualByComparingTo("900000");
    assertThat(view.recentDailyAverage()).isEqualByComparingTo("10000");
    assertThat(view.remainingDays()).isEqualTo(72); // 10-21 ~ 12-31
    assertThat(view.monthlyRest()).isEqualByComparingTo("720000");
    assertThat(view.periodicCount()).isZero();
    assertThat(view.projectedRest()).isEqualByComparingTo("720000");
  }

  /**
   * 분기 배당 덩어리를 하루 평균으로 늘리지 않는다(2026-10-20 검토): 8 월 1,800,000 을 90 일로 나눠 72 일을 곱하면 1,440,000 이 '달마다'
   * 처럼 붙고, 11 월에 실제로 올 배당은 따로 없었다. 이제는 작년 이맘때(10-21 ~ 12-31) 횟수 1 x 최근 금액 1,800,000.
   */
  @Test
  void 분기_배당은_작년_이맘때_횟수_곱하기_최근_금액() {
    FinancialIncomeThresholdView view = compute(quarterlyPayer(UUID.randomUUID()));

    assertThat(view.recentTaxable()).as("분기 배당은 하루 평균에 안 넣는다").isEqualByComparingTo("0");
    assertThat(view.periodicCount()).isEqualTo(1);
    assertThat(view.periodicRest()).isEqualByComparingTo("1800000");
    assertThat(view.projectedRest()).isEqualByComparingTo("1800000");
  }

  @Test
  void 과세_계좌에서_판_종목은_예상에서_뺀다() {
    UUID monthly = UUID.randomUUID();
    UUID quarterly = UUID.randomUUID();
    List<DividendResponse> rows = new ArrayList<>(monthlyPayer(monthly));
    rows.addAll(quarterlyPayer(quarterly));

    FinancialIncomeThresholdView both =
        FinancialIncomeThresholdSupport.compute(rows, Set.of(ISA), TODAY, ZONE, null);
    assertThat(both.projectedRest()).as("보유를 모르면 빼지 않는다").isEqualByComparingTo("2520000");

    FinancialIncomeThresholdView soldQuarterly =
        FinancialIncomeThresholdSupport.compute(rows, Set.of(ISA), TODAY, ZONE, Set.of(monthly));
    assertThat(soldQuarterly.projectedRest()).isEqualByComparingTo("720000");
    assertThat(soldQuarterly.ytdTaxable())
        .as("이미 받은 것은 그대로")
        .isEqualByComparingTo(both.ytdTaxable());

    FinancialIncomeThresholdView soldAll =
        FinancialIncomeThresholdSupport.compute(rows, Set.of(ISA), TODAY, ZONE, Set.of());
    assertThat(soldAll.projectedRest()).isEqualByComparingTo("0");
  }

  /** 과세 지급이 아직 한 번뿐인 새 종목(최근 90 일 안)은 달마다로 보고 최근 흐름에 넣는다(2026-10-20 검토 - 예전엔 0 이었다). */
  @Test
  void 첫_지급만_있는_새_종목도_예상에_넣는다() {
    FinancialIncomeThresholdView view =
        compute(List.of(dividend(TAXABLE, UUID.randomUUID(), "2026-10-19", "300000")));
    assertThat(view.recentTaxable()).isEqualByComparingTo("300000");
    assertThat(view.monthlyRest()).isEqualByComparingTo("240000"); // 300,000 / 90 x 72

    FinancialIncomeThresholdView old =
        compute(List.of(dividend(TAXABLE, UUID.randomUUID(), "2026-03-02", "300000")));
    assertThat(old.projectedRest())
        .as("90 일보다 오래된 한 번은 주기를 몰라 분기 · 연으로(작년 같은 때가 없으면 0)")
        .isEqualByComparingTo("0");
  }

  /** 분기 배당 차례를 작년 날짜와 올해 날짜로 짝짓는다(2026-10-20 검토): 작년 11-20, 올해 11-19 에 이미 받았으면 다시 세지 않는다. */
  @Test
  void 올해_같은_차례를_이미_받았으면_다시_세지_않는다() {
    UUID item = UUID.randomUUID();
    List<DividendResponse> rows =
        List.of(
            dividend(TAXABLE, item, "2025-11-20", "2000000"),
            dividend(TAXABLE, item, "2026-02-20", "2000000"),
            dividend(TAXABLE, item, "2026-05-20", "2000000"),
            dividend(TAXABLE, item, "2026-08-20", "1800000"),
            dividend(TAXABLE, item, "2026-11-19", "1800000"));
    FinancialIncomeThresholdView paid =
        FinancialIncomeThresholdSupport.compute(
            rows, Set.of(ISA), LocalDate.of(2026, 11, 19), ZONE);
    assertThat(paid.periodicCount()).isZero();

    // 올해 지급이 작년보다 늦어 아직 안 들어왔으면(11-25, 작년 11-20 + 5 일) 늦는 것으로 보고 센다.
    FinancialIncomeThresholdView late =
        FinancialIncomeThresholdSupport.compute(
            rows.subList(0, 4), Set.of(ISA), LocalDate.of(2026, 11, 25), ZONE);
    assertThat(late.periodicCount()).isEqualTo(1);
    assertThat(late.periodicRest()).isEqualByComparingTo("1800000");
  }

  /** 12 월 31 일엔 늦는 차례도 올해 더 들어올 날이 없다(검토 2026-10-20: 더 받을 예상 0 아래 "분기 1 회 = X" 가 나왔다). */
  @Test
  void 마지막_날엔_늦는_분기_차례도_세지_않는다() {
    UUID item = UUID.randomUUID();
    FinancialIncomeThresholdView view =
        FinancialIncomeThresholdSupport.compute(
            List.of(
                dividend(TAXABLE, item, "2025-06-24", "500000"),
                dividend(TAXABLE, item, "2025-12-24", "500000")),
            Set.of(ISA),
            LocalDate.of(2026, 12, 31),
            ZONE);
    assertThat(view.periodicCount()).isZero();
    assertThat(view.projectedYearEnd()).isEqualByComparingTo(view.ytdTaxable());
  }

  @Test
  void 그만둔_월배당은_최근_합이_0이라_저절로_빠진다() {
    UUID item = UUID.randomUUID();
    FinancialIncomeThresholdView view =
        compute(
            List.of(
                dividend(TAXABLE, item, "2025-11-20", "200000"),
                dividend(TAXABLE, item, "2025-12-20", "200000"),
                dividend(TAXABLE, item, "2026-01-20", "200000")));
    assertThat(view.projectedRest()).isEqualByComparingTo("0");
  }

  @Test
  void 넘을_것으로_보이면_예상_경고_이미_넘었으면_넘은_금액() {
    FinancialIncomeThresholdView near =
        compute(
            List.of(
                dividend(TAXABLE, "2026-02-01", "15000000"),
                dividend(TAXABLE, "2026-10-01", "9000000")));
    // 지금 24,000,000 > 기준 - 이미 넘었다.
    assertThat(near.exceeded()).isTrue();
    assertThat(near.over()).isEqualByComparingTo("4000000");
    assertThat(near.remaining()).isEqualByComparingTo("0");
    assertThat(near.projectedToExceed()).as("이미 넘었으면 '넘을 수 있다' 가 아니다").isFalse();

    // 지금 19,600,000(분기 5,800,000 + 월배당 1,500,000 + 12,300,000, 기준 아래) + 더 받을 예상(분기 1,800,000 + 월배당
    // 720,000)
    // -> 연말 22,120,000.
    UUID quarterly = UUID.randomUUID();
    List<DividendResponse> rows = new ArrayList<>(quarterlyPayer(quarterly));
    rows.addAll(monthlyPayer(UUID.randomUUID()));
    rows.add(dividend(TAXABLE, "2026-01-05", "12300000"));
    FinancialIncomeThresholdView approaching = compute(rows);
    assertThat(approaching.ytdTaxable()).isEqualByComparingTo("19600000");
    assertThat(approaching.exceeded()).isFalse();
    assertThat(approaching.projectedYearEnd()).isEqualByComparingTo("22120000");
    assertThat(approaching.projectedToExceed()).isTrue();

    FinancialIncomeThresholdView exactly =
        compute(List.of(dividend(TAXABLE, "2026-02-01", "20000000")));
    assertThat(exactly.exceeded()).as("2,000 만 원 '초과' 부터 합산된다 - 같으면 넘지 않았다").isFalse();
  }

  @Test
  void 마지막_날엔_남은_날이_0이라_예상이_지금과_같다() {
    UUID quarterly = UUID.randomUUID();
    FinancialIncomeThresholdView view =
        FinancialIncomeThresholdSupport.compute(
            List.of(
                dividend(TAXABLE, quarterly, "2025-12-31", "900000"),
                dividend(TAXABLE, quarterly, "2026-12-01", "900000")),
            Set.of(),
            LocalDate.of(2026, 12, 31),
            ZONE);
    assertThat(view.remainingDays()).isZero();
    assertThat(view.projectedYearEnd()).isEqualByComparingTo(view.ytdTaxable());
  }

  private static String render(FinancialIncomeThresholdView view) {
    Map<String, Object> model = new HashMap<>();
    model.put("view", view);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/financialIncomeThreshold.jte", model, output);
    return output.toString();
  }

  @Test
  void 조회에_실패하면_구역을_숨긴다() {
    assertThat(render(null)).doesNotContain("data-financial-income");
  }

  @Test
  void 지금까지_더_받을_예상_연말_예상_순으로_그린다() {
    List<DividendResponse> rows = new ArrayList<>(quarterlyPayer(UUID.randomUUID()));
    rows.addAll(monthlyPayer(UUID.randomUUID()));
    rows.add(dividend(TAXABLE, "2026-01-05", "12300000"));
    rows.add(dividend(TAXABLE, "2026-03-03", null));
    String html = render(compute(rows));

    int ytd = html.indexOf("data-financial-income-ytd");
    int rest = html.indexOf("data-financial-income-rest>");
    int projected = html.indexOf("data-financial-income-projected>");
    assertThat(ytd).isPositive();
    assertThat(rest).isGreaterThan(ytd);
    assertThat(projected).isGreaterThan(rest);
    assertThat(html)
        .contains("19,600,000")
        .contains("2,520,000")
        .contains("22,120,000")
        .contains("data-financial-income-rest-periodic")
        .contains("1,800,000")
        .contains("data-financial-income-remaining")
        .contains("data-financial-income-projected-exceed")
        .contains("data-financial-income-unknown")
        .contains("data-bar-tone=\"warning\"")
        .doesNotContain("data-financial-income-over");
    // 지금까지 98% 뒤에 연말 예상(110% -> 100 에서 자름)을 옅게 2% 잇는다.
    assertThat(html)
        .contains("data-financial-income-projected-bar=\"100\"")
        .contains("style=\"width: 2%\" title=");
  }
}
