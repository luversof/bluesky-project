package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 배당 달력에서 <b>보이는 숫자를 더하면 그 칸의 합계와 같아야 한다.</b>
 *
 * <p>예상 월배당은 주당 평균 x 보유수량이라 원 미만이 남는다. 행은 각각 원 단위로 반올림해 찍히는데 합계만 원값을 더한 뒤 한 번 반올림하면 <b>보이는 숫자를 더한
 * 값과 달라진다</b> &mdash; 실측 2026-08-23 월배당 8 종목에서 행 합과 소계가 2 원 어긋났다.
 *
 * <p>2026-09-14 에 이 화면이 지급 시기 묶음(월중/월말)에서 <b>달력</b>으로 바뀌면서 같은 불변식이 '달력 한 칸'으로 옮겨왔다.
 */
class DividendCalendarSubtotalTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

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

  /** 원 미만이 남는 금액들 - 각각 반올림하면 합계가 원값 합계의 반올림과 달라진다. */
  private static DividendCalendarView.Entry entry(String name, String amount) {
    BigDecimal value = new BigDecimal(amount);
    return new DividendCalendarView.Entry(
        name, name, value, value, BigDecimal.ZERO, null, 17, 17, 17, 12, false);
  }

  private String render(List<DividendCalendarView.Entry> entries) {
    YearMonth month = YearMonth.of(2026, 9);
    DividendCalendarView calendar =
        new DividendCalendarView(
            month,
            DividendCalendarGridUtil.build(
                month,
                LocalDate.parse("2026-09-14"),
                DividendCalendarGridUtil.groupByDay(entries, month)),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=2026-08");
    model.put("nextHref", "/stock/dividend?tab=calendar&month=2026-10");
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", true);
    model.put("avgLabel", "평균");
    model.put("latestLabel", "최근");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  private static final String QUOTE = String.valueOf((char) 34);

  /** 화면에 찍힌 금액 하나. 압축 표기(221만)는 단위가 달라 걸리지 않는다. */
  private static final String AMOUNT_PATTERN =
      "class="
          + QUOTE
          + "amount-value"
          + QUOTE
          + ">([0-9,]+)"
          + String.valueOf((char) 0xC6D0)
          + "</span>";

  /** 17 일 칸만 잘라 본다 - 다른 칸의 숫자까지 세면 검사가 헛돈다. */
  private String dayCell(String html, String date) {
    int at = html.indexOf("data-calendar-day=\"" + date + "\"");
    assertThat(at).as(date + " 칸을 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    int end = html.indexOf("</td>", at);
    return html.substring(at, end);
  }

  @Test
  void 보이는_값을_더하면_칸_합계와_같다() {
    List<DividendCalendarView.Entry> entries =
        List.of(
            entry("가", "581692.6"),
            entry("나", "257894.5"),
            entry("다", "82848.7"),
            entry("라", "29633.6"));

    String html = render(entries);

    // 칸에는 합계만 남았다 - 종목별 금액은 달력 아래 목록이 제 크기로 말한다(사용자 지적 2026-09-15:
    // 42 칸 중 평균 2 칸만 차는 격자 때문에 금액이 11px 로 눌려 있었다). 검산은 그대로 지킨다.
    List<Long> cellAmounts = amounts(dayCell(html, "2026-09-17"));
    assertThat(cellAmounts).as("칸에서 합계를 읽지 못했다").hasSize(1);
    long dayTotal = cellAmounts.get(0);

    long rowSum = 0L;
    Matcher row =
        Pattern.compile("data-calendar-entry=.*?</li>", Pattern.DOTALL)
            .matcher(html.substring(html.indexOf("data-calendar-daylist")));
    int rows = 0;
    while (row.find()) {
      List<Long> shown = amounts(row.group());
      assertThat(shown).as("종목 줄에서 금액을 읽지 못했다").isNotEmpty();
      rowSum += shown.get(0); // 첫 값이 세전 - 칸 합계와 같은 축이다
      rows++;
    }
    assertThat(rows).as("목록에서 종목 줄을 읽지 못했다 - 검사가 무력해진다").isEqualTo(entries.size());

    assertThat(dayTotal).as("보이는 행을 더한 값과 칸 합계가 다르면 사용자가 검산할 수 없다").isEqualTo(rowSum);
  }

  /** 화면에 찍힌 금액만 순서대로. 압축 표기(221만)는 단위가 달라 걸리지 않는다. */
  private List<Long> amounts(String fragment) {
    List<Long> found = new ArrayList<>();
    Matcher matcher = Pattern.compile(AMOUNT_PATTERN).matcher(fragment);
    while (matcher.find()) {
      found.add(Long.parseLong(matcher.group(1).replace(",", "")));
    }
    return found;
  }

  /** 버림과 반올림은 실제로 다르다 - 규칙을 바꾸면 화면 숫자가 움직인다. */
  @Test
  void 버림과_반올림은_실제로_다르다() {
    List<BigDecimal> amounts =
        List.of(
            new BigDecimal("581692.6"),
            new BigDecimal("257894.5"),
            new BigDecimal("82848.7"),
            new BigDecimal("29633.6"));

    long truncated = amounts.stream().mapToLong(BigDecimal::longValue).sum();
    long rounded = amounts.stream().mapToLong(StockFormatUtil::displayWon).sum();

    assertThat(truncated).isEqualTo(952_067L);
    assertThat(rounded).isEqualTo(952_071L);
  }
}
