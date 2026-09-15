package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.stock.util.StockTabLinkUtil;

/**
 * 배당 캘린더의 달 이동 링크.
 *
 * <p>달 이동은 <b>달만</b> 바꿔야 한다. 2026-09-14 까지 이 세 링크는 {@code /stock/dividend?tab=calendar&month=...} 를
 * 손으로 붙여 기간·필터를 통째로 버렸다 &mdash; 실측: {@code
 * rangeMode=custom&startDate=2025-01-01&endDate=2025-12-31} 으로 들어가 다음 달을 한 번 눌렀더니 주소에 그 셋이 하나도 남지
 * 않았고, 그 화면의 실수령 배당 탭 링크도 기간을 잃어 <b>돌아가면 조용히 전체 기간</b>이 됐다.
 *
 * <p>같은 화면의 탭 링크는 이미 조건을 들고 간다({@link StockTabLinkUtil}). 한 화면 안에서 한쪽은 지키고 한쪽은 버리고 있었다.
 */
class DividendCalendarNavHrefTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  private static final String FRAGMENT_SOURCE =
      "src/main/jte/stock/fragments/dividendCalendarMonth.jte";

  private static final String CONTROLLER_SOURCE =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java";

  /** 달만 바뀌고 나머지는 조각째 남는다. */
  @Test
  void 달만_바꾸고_조건은_그대로_옮긴다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/dividend",
            "rangeMode=custom&startDate=2025-01-01&endDate=2025-12-31&tab=calendar&month=2026-09",
            "month",
            "2026-10");

    assertThat(href)
        .isEqualTo(
            "/stock/dividend?rangeMode=custom&startDate=2025-01-01&endDate=2025-12-31"
                + "&tab=calendar&month=2026-10");
  }

  /** 조건이 없던 주소는 그대로 달만 붙는다. */
  @Test
  void 조건이_없으면_달만_붙는다() {
    assertThat(StockTabLinkUtil.switchHref("/stock/dividend", "tab=calendar", "month", "2026-10"))
        .isEqualTo("/stock/dividend?tab=calendar&month=2026-10");
  }

  /** 조각은 받은 주소를 그대로 쓴다 &mdash; 여기서 다시 만들면 조건이 또 사라진다. */
  @Test
  void 조각은_받은_주소를_그대로_쓴다() {
    YearMonth month = YearMonth.of(2026, 9);
    Map<String, Object> model = new HashMap<>();
    model.put(
        "calendar",
        new DividendCalendarView(
            month,
            DividendCalendarGridUtil.build(month, LocalDate.parse("2026-09-14"), Map.of()),
            List.of()));
    model.put("prevHref", "/stock/dividend?keep=1&tab=calendar&month=2026-08");
    model.put("nextHref", "/stock/dividend?keep=1&tab=calendar&month=2026-10");
    model.put("thisHref", "/stock/dividend?keep=1&tab=calendar&month=2026-09");
    model.put("isThisMonth", true);
    model.put("avgLabel", "평균");
    model.put("latestLabel", "최근");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    String html = output.toString();

    // JTE 가 & 를 &amp; 로 이스케이프한다(HTML 로 맞다 - 브라우저가 & 로 되돌린다).
    assertThat(html).contains("href=\"/stock/dividend?keep=1&amp;tab=calendar&amp;month=2026-08\"");
    assertThat(html).contains("href=\"/stock/dividend?keep=1&amp;tab=calendar&amp;month=2026-10\"");
    assertThat(html).contains("href=\"/stock/dividend?keep=1&amp;tab=calendar&amp;month=2026-09\"");
  }

  /** 조각이 주소를 손으로 붙이지 않는다(붙이면 위 검사는 통과해도 실제 화면이 조건을 버린다). */
  @Test
  void 조각은_주소를_짓지_않는다() throws IOException {
    String template = Files.readString(Path.of(FRAGMENT_SOURCE), StandardCharsets.UTF_8);

    assertThat(template)
        .as("조각 안에서 경로를 만들면 컨트롤러가 들고 온 조건이 사라진다")
        .doesNotContain("/stock/dividend?");
  }

  /** 컨트롤러가 탭 링크와 <b>같은 도구</b>로 만든다 &mdash; 규칙이 두 벌이면 또 갈라진다. */
  @Test
  void 컨트롤러가_탭_링크와_같은_규칙을_쓴다() throws IOException {
    String source = Files.readString(Path.of(CONTROLLER_SOURCE), StandardCharsets.UTF_8);

    assertThat(source).contains("StockTabLinkUtil.switchHref(");
    assertThat(source).contains("monthHref(currentQuery, month.minusMonths(1))");
    assertThat(source).contains("monthHref(currentQuery, month.plusMonths(1))");
    assertThat(source).contains("monthHref(currentQuery, YearMonth.from(today))");
  }
}
