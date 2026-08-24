package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 배당 달력에 보이는 행을 더하면 소계와 같아야 한다.
 *
 * <p>예상 월배당은 주당 평균 x 보유수량이라 원 미만이 남는다. 예전에는 행이 각각 {@code longValue()} 로 버려지는데 소계만 BigDecimal 합계를 한
 * 번 버려서, <b>보이는 숫자를 더하면 소계와 달랐다</b> &mdash; 실측 2026-08-23: 월배당 8 종목의 행 합 2,778,302 vs 소계 2,778,304
 * 로 2 원 차이(지금은 행·소계 모두 반올림이라 2,778,305 로 맞는다). 버림이라 행마다 최대 1 원씩 모자라고 종목 수만큼 벌어진다.
 */
class DividendCalendarSubtotalTest {

  private static final String GROUP = "stock/fragments/dividendCalendarGroup.jte";

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

  /** 실측 8 종목의 주당 평균 배당과 보유 수량(2026-08-23 스냅샷). */
  private static final String[][] REAL_ROWS = {
    {"KODEX 200타겟위클리커버드콜", "240.0", "857"}, // 205,680
    {"RISE 200위클리커버드콜", "197.5833", "5710"}, // 1,128,200.643
    {"TIGER 코리아배당다우존스위클리커버드콜", "106.1", "22"}, // 2,334.2
    {"PLUS 고배당주위클리고정커버드콜", "150.9167", "117"}, // 17,657.2539
    {"TIGER 리츠부동산인프라", "33.0", "13748"}, // 453,684
    {"TIGER 배당커버드콜액티브", "350.1667", "1115"}, // 390,435.8705
    {"RISE 코리아밸류업위클리고정커버드콜", "308.0", "98"}, // 30,184
    {"KODEX 한국부동산리츠인프라", "30.5", "18037"}, // 550,128.5
  };

  private MonthlyDividendSnapshotResponse row(String name, String perShare, int quantity) {
    BigDecimal expected = new BigDecimal(perShare).multiply(BigDecimal.valueOf(quantity));
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "000000",
        name,
        LocalDate.parse("2026-08-04"),
        new BigDecimal(perShare),
        new BigDecimal(perShare),
        BigDecimal.ZERO,
        quantity,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        expected,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Instant.parse("2026-08-04T00:00:00Z"));
  }

  private List<MonthlyDividendSnapshotResponse> realRows() {
    List<MonthlyDividendSnapshotResponse> rows = new ArrayList<>();
    for (String[] r : REAL_ROWS) {
      rows.add(row(r[0], r[1], Integer.parseInt(r[2])));
    }
    return rows;
  }

  /**
   * 소계는 <b>컨트롤러의 실제 코드</b>로 만든다.
   *
   * <p>처음에는 테스트가 소계를 직접 다시 계산해 화면 행과 비교했다. 그러면 양쪽을 같은 식으로 만든 셈이라 무엇을 바꿔도 통과한다 &mdash; 실제로 화면 행을 옛
   * 버림 규칙으로 되돌리는 변이가 그대로 살아남았다.
   */
  private BigDecimal subtotalOf(List<MonthlyDividendSnapshotResponse> rows) {
    return StockViewController.sumExpectedMonthlyDividend(rows);
  }

  private String render(List<MonthlyDividendSnapshotResponse> rows, BigDecimal subtotal) {
    Map<String, Object> params = new HashMap<>();
    params.put("title", "월중");
    params.put("rows", rows);
    params.put("subtotal", subtotal);
    params.put("subtotalLatest", subtotal);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(GROUP, params, output);
    return output.toString();
  }

  /** 렌더된 화면에서 콤마 숫자를 전부 뽑는다. */
  private List<Long> numbers(String html) {
    List<Long> found = new ArrayList<>();
    Matcher matcher = Pattern.compile(">([0-9]{1,3}(?:,[0-9]{3})+)<").matcher(html);
    while (matcher.find()) {
      found.add(Long.parseLong(matcher.group(1).replace(",", "")));
    }
    return found;
  }

  @Test
  void 보이는_행을_더하면_소계와_같다() {
    List<MonthlyDividendSnapshotResponse> rows = realRows();
    BigDecimal subtotal = subtotalOf(rows);

    String html = render(rows, subtotal);
    List<Long> shown = numbers(html);

    // 화면에서 소계 줄을 빼고 남은 것이 행이다. 소계는 "평균"과 "최신" 두 줄로 같은 값이 두 번 나온다.
    long subtotalShown = subtotal.longValue();
    assertThat(shown).as("소계가 화면에 없다").contains(subtotalShown);

    List<Long> rowNumbers = new ArrayList<>(shown);
    rowNumbers.remove(Long.valueOf(subtotalShown)); // 평균 소계
    rowNumbers.remove(Long.valueOf(subtotalShown)); // 최신 소계

    long rowSum = 0;
    for (Long value : rowNumbers) {
      rowSum += value;
    }
    // 행마다 "평균"과 "최신" 두 값이 그려지는데 이 픽스처는 둘을 같게 두었으므로 합은 소계의 2 배다.
    assertThat(rowSum)
        .as("화면에 보이는 행을 더한 값이 소계와 다르다 - 사용자가 열을 더하면 맞지 않는다")
        .isEqualTo(subtotalShown * 2);
  }

  /**
   * 버림 규칙이면 실제로 어긋난다. 이 값이 어긋나지 않으면 위 검사는 아무것도 지키지 못한다.
   *
   * <p>실측 8 종목: 정확한 합 2,778,304.4674 / 버림 합 2,778,302 / 반올림 합 2,778,305.
   */
  @Test
  void 버림과_반올림은_실제로_다르다() {
    List<MonthlyDividendSnapshotResponse> rows = realRows();
    long truncated = 0;
    long rounded = 0;
    for (MonthlyDividendSnapshotResponse r : rows) {
      truncated += r.expectedMonthlyDividend().longValue();
      rounded += StockFormatUtil.displayWon(r.expectedMonthlyDividend());
    }
    assertThat(truncated).isEqualTo(2_778_302L);
    assertThat(rounded).isEqualTo(2_778_305L);
  }
}
