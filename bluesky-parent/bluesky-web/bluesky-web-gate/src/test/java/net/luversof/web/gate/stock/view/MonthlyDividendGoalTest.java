package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.luversof.web.gate.stock.controller.StockViewController;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.service.MonthlyDividendGoalCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendGoalCalculator.Basis;
import net.luversof.web.gate.stock.service.MonthlyDividendGoalCalculator.Goal;

/**
 * 시뮬레이터 월배당 탭의 '목표 월배당까지'(2026-10-20 자율 개선) &mdash; 모자란 월배당 · 한 번에 더 살 금액 · 매달 적립 + 재투자로 걸리는 달.
 *
 * <p>판정은 순수 함수의 동작으로 본다. 기준: 분배율 10%(월배당 100 만 원 · 평가액 1 억 2 천만 원).
 */
class MonthlyDividendGoalTest {

  private static final Basis TEN_PCT =
      new Basis(new BigDecimal("1000000"), new BigDecimal("120000000"), 3);

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

  private static MonthlyDividendSnapshotResponse row(
      UUID stockItemId, String perShare, Integer heldQuantity, String price) {
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        stockItemId,
        "000000",
        "종목",
        LocalDate.parse("2026-10-15"),
        perShare == null ? null : new BigDecimal(perShare),
        null,
        null,
        heldQuantity,
        null,
        price == null ? null : new BigDecimal(price),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void 기준은_원장_지금_수량으로_세고_값이_빈_종목은_뺀다() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    List<MonthlyDividendSnapshotResponse> rows =
        List.of(
            row(a, "100", 10, "10000"), // 원장 수량 20 으로 센다
            row(b, "50", 100, "5000"), // 원장에 없으면 스냅샷 수량
            row(UUID.randomUUID(), null, 100, "5000"), // 분배금 없음
            row(UUID.randomUUID(), "50", 100, null)); // 현재가 없음

    Basis basis = MonthlyDividendGoalCalculator.basisOf(rows, Map.of(a, 20));

    assertThat(basis.rowCount()).isEqualTo(2);
    assertThat(basis.monthlyDividend()).isEqualByComparingTo("7000"); // 100x20 + 50x100
    assertThat(basis.marketValue()).isEqualByComparingTo("700000"); // 10000x20 + 5000x100
    assertThat(basis.annualYieldPct()).isEqualByComparingTo("12.00");
  }

  @Test
  void 모자란_월배당과_한_번에_더_살_금액() {
    Goal goal = MonthlyDividendGoalCalculator.compute(TEN_PCT, new BigDecimal("2000000"), null);
    assertThat(goal.reached()).isFalse();
    assertThat(goal.shortfall()).isEqualByComparingTo("1000000");
    // 100 만 원 x 12 / 10% = 1 억 2 천만 원
    assertThat(goal.additionalCapital()).isEqualByComparingTo("120000000");
  }

  @Test
  void 재투자만으로_두_배가_되는_달은_84() {
    // 월 0.8333...% 복리로 2 배 = ln2 / ln(1 + 0.1/12) = 83.5 -> 84 번째 달에 처음 닿는다.
    Goal goal = MonthlyDividendGoalCalculator.compute(TEN_PCT, new BigDecimal("2000000"), null);
    assertThat(goal.monthsToGoal()).isEqualTo(84);
    assertThat(goal.years()).isEqualTo(7);
    assertThat(goal.remainderMonths()).isZero();
    assertThat(goal.contributedAtGoal()).isEqualByComparingTo("0");
    assertThat(goal.capitalAtGoal()).isGreaterThanOrEqualTo(new BigDecimal("240000000"));
  }

  @Test
  void 적립하면_더_빨리_닿고_적립_합은_달_수만큼() {
    Goal goal =
        MonthlyDividendGoalCalculator.compute(
            TEN_PCT, new BigDecimal("2000000"), new BigDecimal("1000000"));
    assertThat(goal.monthsToGoal()).isBetween(1, 83);
    assertThat(goal.contributedAtGoal())
        .isEqualByComparingTo(new BigDecimal(1000000L * goal.monthsToGoal()));
  }

  @Test
  void 이미_넘었거나_분배율이_0이거나_못_닿는_경우() {
    Goal reached = MonthlyDividendGoalCalculator.compute(TEN_PCT, new BigDecimal("1000000"), null);
    assertThat(reached.reached()).isTrue();
    assertThat(reached.monthsToGoal()).isZero();

    Goal zeroYield =
        MonthlyDividendGoalCalculator.compute(
            new Basis(BigDecimal.ZERO, new BigDecimal("1000"), 1), new BigDecimal("1000"), null);
    assertThat(zeroYield.additionalCapital()).isNull();
    assertThat(zeroYield.monthsToGoal()).isNull();

    Goal never =
        MonthlyDividendGoalCalculator.compute(TEN_PCT, new BigDecimal("100000000000"), null);
    assertThat(never.monthsToGoal()).as("100 년 안에 못 닿는다").isNull();
  }

  @Test
  void 입력_칸_값_읽기() throws Exception {
    var parse = StockViewController.class.getDeclaredMethod("parseGoalAmount", String.class);
    parse.setAccessible(true);
    assertThat((BigDecimal) parse.invoke(null, "3,000,000")).isEqualByComparingTo("3000000");
    assertThat(parse.invoke(null, " 500000 ")).isEqualTo(new BigDecimal("500000"));
    assertThat(parse.invoke(null, "-1")).isNull();
    assertThat(parse.invoke(null, "abc")).isNull();
    assertThat(parse.invoke(null, "")).isNull();
    assertThat(parse.invoke(null, "9999999999999")).as("1 조 원 넘음").isNull();
    // 지수 표기는 받지 않는다 - setScale 이 몇 분씩 돌았다(2026-10-20 검토).
    assertThat(parse.invoke(null, "1E-600000000")).isNull();
    assertThat(parse.invoke(null, "3000000.5")).isNull();
  }

  @Test
  void 목표_칸은_결과_구역_밖에서_자기_구역만_바꾼다() throws IOException {
    Map<String, Object> model = new HashMap<>();
    model.put("basis", TEN_PCT);
    model.put(
        "goal",
        MonthlyDividendGoalCalculator.compute(
            TEN_PCT, new BigDecimal("2000000"), new BigDecimal("1000000")));
    model.put("goalMonthly", new BigDecimal("2000000"));
    model.put("goalContribution", new BigDecimal("1000000"));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendGoal.jte", model, output);
    String html = output.toString();
    assertThat(html)
        .contains("id=\"monthlyDividendGoal\"")
        .contains("hx-select=\"#monthlyDividendGoal\"")
        .contains("hx-replace-url=\"false\"")
        .contains("name=\"goalMonthly\" value=\"2000000\"")
        .contains("120,000,000")
        .contains("data-monthly-goal-months=");
    int shortfall = html.indexOf("data-monthly-goal-shortfall");
    int additional = html.indexOf("data-monthly-goal-additional");
    assertThat(additional).as("모자란 월배당 -> 더 살 금액 순").isGreaterThan(shortfall);

    String page =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte"),
            StandardCharsets.UTF_8);
    int goalAt = page.indexOf("@template.stock.fragments.monthlyDividendGoal(");
    int resultsAt = page.indexOf("id=\"monthlyDividendResults\"");
    assertThat(goalAt).as("결과 구역보다 앞(밖)에 둔다").isPositive().isLessThan(resultsAt);
  }
}
