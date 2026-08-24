package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * 예상 월배당을 보여주는 화면이 "어느 시점 기준 데이터인지"를 밝히는지 본다.
 *
 * <p>예상 배당은 저장된 기준 데이터(월배당 스냅샷)로 계산된다. 스냅샷은 자동으로 갱신되지 않고 사람이 넣어야 하며 종목마다 시점이 다르다.
 *
 * <p>실측 2026-08-22: 8 종목의 기준일이 2026-07-20 ~ 2026-08-04 로 <b>최대 33 일</b> 뒤처져 있었고, 그만큼 보유 수량이 옛 값이라
 * 예상 월배당이 현재 수량 기준보다 1.66% 낮게 잡혔다(2,778,304 vs 2,824,428). 월배당 시뮬레이터에는 이미 기준일 표기가 있는데 <b>대시보드 카드와
 * 배당 달력에는 없어서</b>, 가장 눈에 띄는 두 화면이 그 숫자를 현재값처럼 보여주고 있었다.
 */
class DividendAsOfDisclosureTest {

  private static final String CARD = "stock/htmx/fragments/upcomingDividends.jte";

  private static final String KEY = "stock.simulator.monthly.summary.as.of.date";

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

  private String renderCard(LocalDate oldest, LocalDate newest) {
    return renderCard(oldest, newest, 0, java.math.BigDecimal.ZERO);
  }

  private String renderCard(
      LocalDate oldest, LocalDate newest, long staleQuantityCount, java.math.BigDecimal total) {
    Map<String, Object> params = new HashMap<>();
    params.put("hasSnapshots", false);
    params.put("dividendAsOfOldest", oldest);
    params.put("dividendAsOfNewest", newest);
    params.put("staleQuantityCount", staleQuantityCount);
    params.put("currentQuantityTotal", total);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CARD, params, output);
    return output.toString();
  }

  @Test
  void 대시보드_카드가_기준일_구간을_적는다() {
    String html = renderCard(LocalDate.parse("2026-07-20"), LocalDate.parse("2026-08-04"));

    assertThat(html)
        .as("예상 배당이 어느 시점 데이터로 계산됐는지 화면에 없다")
        .contains("2026-07-20")
        .contains("2026-08-04");
    assertThat(html).containsPattern("(Reference as of|기준 데이터 기준일)");
  }

  /** 종목별 시점이 모두 같으면 한 날짜만 적는다(같은 날짜를 두 번 적으면 지저분하다). */
  @Test
  void 시점이_하나면_한_날짜만_적는다() {
    String html = renderCard(LocalDate.parse("2026-08-04"), LocalDate.parse("2026-08-04"));

    assertThat(html).contains("2026-08-04");
    assertThat(html).doesNotContain("2026-08-04 ~ 2026-08-04");
  }

  /** 근거가 없으면 아무 날짜도 적지 않는다. 없는 기준일을 지어내면 더 나쁘다. */
  @Test
  void 기준일이_없으면_적지_않는다() {
    String html = renderCard(null, null);

    assertThat(html).doesNotContain("Reference as of");
    assertThat(html).doesNotContain("2026-");
  }

  /**
   * 스냅샷 수량이 원장과 어긋났다는 안내는 <b>같은 숫자를 보여 주는 화면 전부</b>에 있어야 한다.
   *
   * <p>실측 2026-08-23: 8 종목 중 7 종목의 수량이 어긋나 예상 월배당이 46,124 원 / 1.66% 낮았다(2,778,304 vs 2,824,428).
   * 그런데 안내는 요약 카드와 월배당 시뮬레이터에만 있었고 <b>배당 달력에는 없었다</b> &mdash; 같은 숫자가 화면에 따라 밝혀지기도 하고 아무 말 없이 나가기도
   * 했다.
   */
  @Test
  void 스냅샷_수량_안내가_있어야_할_화면에_다_있다() throws IOException {
    for (String template :
        List.of(
            "src/main/jte/stock/htmx/fragments/upcomingDividends.jte",
            "src/main/jte/stock/fragments/monthlyDividendSimulator.jte",
            "src/main/jte/stock/dividend.jte")) {
      String source = Files.readString(Path.of(template), StandardCharsets.UTF_8);
      assertThat(source)
          .as("%s 에 스냅샷 수량 안내가 없다 - 같은 숫자인데 이 화면만 조용하다", template)
          .contains("stock.summary.upcoming.dividend.stale.quantity");
    }
  }

  /** 어긋난 종목이 없으면 안내를 내지 않는다. 항상 켜져 있으면 경고가 무뎌진다. */
  @Test
  void 달력은_어긋난_수량이_없으면_안내를_내지_않는다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/jte/stock/dividend.jte"), StandardCharsets.UTF_8);
    assertThat(source)
        .as("조건 없이 항상 그리면 어긋나지 않을 때도 경고가 뜬다")
        .contains("@if(calendarStaleQuantityCount > 0)");
  }

  /** 달력 화면에 넘길 값을 컨트롤러가 실제로 만드는지. 템플릿만 고치면 값이 늘 0 이라 안내가 영영 안 뜬다. */
  @Test
  void 컨트롤러가_달력용_수량_기준을_만든다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(source).contains("calendarStaleQuantityCount");
    assertThat(source).contains("calendarCurrentQuantityTotal");
    // 요약 카드와 같은 계산을 써야 두 화면의 숫자가 갈리지 않는다.
    int at = source.indexOf("calendarStaleQuantityCount");
    assertThat(source.substring(Math.max(0, at - 600), at)).contains("currentQuantitySummary(");
  }

  @Test
  void 컨트롤러가_전체_행에서_구간을_구한다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java"),
            StandardCharsets.UTF_8);
    // 화면에 넘기는 목록은 상위 5 개(limit(5))뿐이라 거기서 구하면 구간이 실제보다 좁아진다.
    // 합계를 만든 전체 행(windowRows)에서 구해야 한다.
    int oldestAt = source.indexOf("dividendAsOfOldest");
    int newestAt = source.indexOf("dividendAsOfNewest");
    assertThat(oldestAt).as("구간 시작을 넘기지 않는다").isGreaterThan(0);
    assertThat(newestAt).as("구간 끝을 넘기지 않는다").isGreaterThan(0);
    for (int at : new int[] {oldestAt, newestAt}) {
      String block = source.substring(at, Math.min(source.length(), at + 300));
      assertThat(block).as("합계를 만든 전체 행에서 구해야 한다").contains("windowRows.stream()");
      // windowRows.stream().limit(5) 도 위 검사를 통과한다(실제로 그렇게 통과해 주입을 놓쳤다).
      assertThat(block).as("상위 몇 개만 보면 구간이 실제보다 좁아진다").doesNotContain("limit(");
    }
  }

  /** 달력도 같은 스냅샷을 쓰므로 같은 표기가 있어야 한다. */
  @Test
  void 배당_달력도_기준일을_적는다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/jte/stock/dividend.jte"), StandardCharsets.UTF_8);
    assertThat(source).contains(KEY).contains("calendarAsOfOldest");
    assertThat(source).as("기준일이 없을 때도 그리면 빈 라벨만 남는다").contains("@if(calendarAsOfOldest != null)");
  }

  @Test
  void 안내_문구_키가_두_번들에_있다() throws IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      assertThat(Files.readString(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8))
          .as(bundle + " 에 " + KEY + " 이 없다")
          .contains(KEY);
    }
  }

  /**
   * 기준일만으로는 무엇이 낡았는지 알 수 없다.
   *
   * <p>스냅샷이 낡으면 <b>보유 수량</b>이 옛 값이라 예상 배당이 실제보다 낮게(또는 높게) 잡힌다. 원장의 현재 수량은 항상 정확하므로, 어긋난 종목 수와 현재
   * 수량으로 다시 낸 합계를 같이 적어 사용자가 갱신할지 판단할 수 있게 한다.
   *
   * <p>실측 2026-08-23: 스냅샷 8 종목 중 <b>7 종목</b>의 수량이 현재와 달랐고(전부 현재가 더 많았다), 그 7 종목이 표시 합계의 80.2% 를
   * 차지했다. 합계는 2,778,304 vs 현재 수량 기준 2,824,428 로 1.66% 낮게 잡혀 있었다.
   */
  @Test
  void 수량이_어긋나면_현재_수량_기준_합계를_같이_적는다() {
    String html =
        renderCard(
            LocalDate.parse("2026-07-20"),
            LocalDate.parse("2026-08-04"),
            7,
            new java.math.BigDecimal("2824428"));

    // "7" 만 찾으면 항상 통과한다(실측: 4로 바꿔도 통과했다).
    assertThat(html).as("어긋난 종목 수를 적지 않는다").contains("data-stale-quantity=\"7\"");
    assertThat(html).as("현재 수량으로 다시 낸 합계를 적지 않는다").contains("2,824,428");
  }

  /** 어긋난 종목이 없으면 아무 말도 하지 않는다. 늘 뜨는 경고는 곧 무시된다. */
  @Test
  void 수량이_모두_맞으면_경고하지_않는다() {
    String html =
        renderCard(
            LocalDate.parse("2026-08-04"),
            LocalDate.parse("2026-08-04"),
            0,
            new java.math.BigDecimal("2824428"));

    assertThat(html).doesNotContain("2,824,428");
  }

  /**
   * 현재 수량 기준 합계가 스냅샷 수량이 아니라 <b>원장 수량</b>으로 계산되는지.
   *
   * <p>스냅샷의 {@code expectedMonthlyDividend} 는 정확히 {@code averageMonthlyDividendPerShare1y x
   * heldQuantity} 다(실측 8 건 전부 일치). 그래서 1 주당 값은 그대로 두고 수량만 원장 값으로 바꿔 다시 곱해야 한다.
   *
   * <p>계산은 {@code MonthlyDividendCalculator.currentQuantitySummary} 한 곳에 있다 &mdash; 예전에는 이 컨트롤러
   * 본문에만 있었고, 같은 스냅샷 수량으로 계산되는 월배당 시뮬레이터의 합계 카드는 안내 없이 옛 수량 기준 값을 내보냈다. 값 자체는 {@code
   * MonthlyDividendCurrentQuantityTest} 가 고정하고, 여기서는 컨트롤러가 그 규칙을 직접 다시 구현하지 않는지 본다.
   */
  @Test
  void 현재_수량_합계는_공용_규칙으로_계산한다() throws IOException {
    String calculator =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/service/MonthlyDividendCalculator.java"),
            StandardCharsets.UTF_8);
    assertThat(calculator)
        .as("1 주당 값(평균 1년)에 원장 수량을 곱해야 한다")
        .contains("perShare.multiply(BigDecimal.valueOf(currentQuantity))");

    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java"),
            StandardCharsets.UTF_8);
    assertThat(source)
        .as("컨트롤러는 공용 규칙을 불러야 한다")
        .contains("MonthlyDividendCalculator.currentQuantitySummary(");
    assertThat(source)
        .as("규칙을 컨트롤러에 다시 적으면 두 화면이 갈린다")
        .doesNotContain("perShare.multiply(BigDecimal.valueOf(currentQuantity))");
    assertThat(source)
        .as("원장 수량은 종목 단위로 묶어 읽어야 한다")
        .contains("TradeProfitRequestGroup.STOCKITEM.name()");
  }

  /** 시뮬레이터 합계 카드도 같은 규칙을 쓰고 안내 문구를 낸다. */
  @Test
  void 시뮬레이터_합계도_같은_규칙과_문구를_쓴다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(controller)
        .as("시뮬레이터가 현재 수량 기준 합계를 계산하지 않는다")
        .contains("MonthlyDividendCalculator.currentQuantitySummary(");

    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte"),
            StandardCharsets.UTF_8);
    assertThat(template)
        .as("합계 카드에 수량 어긋남 안내가 없다 - 행에만 경고가 뜨고 헤드라인은 조용해진다")
        .contains("stock.summary.upcoming.dividend.stale.quantity");
  }
}
