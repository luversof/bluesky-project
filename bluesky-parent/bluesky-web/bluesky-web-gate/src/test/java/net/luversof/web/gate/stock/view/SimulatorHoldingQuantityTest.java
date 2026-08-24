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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/**
 * 월배당 시뮬레이터 표의 '보유수량' 칸이 옛 값을 사실처럼 찍지 않는지 본다.
 *
 * <p>이 표의 보유수량은 월배당 스냅샷에 저장된 값이다. 스냅샷은 자동 갱신이 아니라 사람이 넣는다. 예상 배당은 "추정"으로 읽히지만 보유수량은 사용자가 <b>사실</b>로
 * 읽는 값이라, 옛 값이 그대로 찍히면 자기 보유량을 잘못 안다.
 *
 * <p>실측 2026-08-23: 스냅샷 8 종목 중 <b>7 종목</b>의 수량이 원장의 현재 수량과 달랐다(전부 현재가 더 많았다. 예: 857 주로 찍혔지만 실제 879
 * 주). 예상 월배당은 정확히 {@code averageMonthlyDividendPerShare1y x heldQuantity} 라, 같은 이유로 합계도 1.66% 낮게 잡혀
 * 있었다.
 *
 * <p>스냅샷 값을 몰래 바꿔 쓰지는 않는다 &mdash; 갱신은 사용자의 몫이고, 행 안의 다른 숫자(예상 배당)는 스냅샷 수량으로 계산된 값이라 수량만 바꾸면 행이 서로 안
 * 맞는다. 대신 현재 수량을 함께 적어 갱신할지 판단하게 한다.
 */
class SimulatorHoldingQuantityTest {

  private static final String TEMPLATE = "stock/fragments/monthlyDividendSimulator.jte";

  private static final UUID STOCK_ITEM_ID = UUID.randomUUID();

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

  private MonthlyDividendSnapshotResponse snapshotRow(int heldQuantity) {
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        STOCK_ITEM_ID,
        "0000A0",
        "KODEX 200타겟위클리커버드콜",
        LocalDate.parse("2026-07-20"),
        new BigDecimal("323"),
        new BigDecimal("240"),
        new BigDecimal("0.5"),
        heldQuantity,
        new BigDecimal("10000"),
        new BigDecimal("11000"),
        new BigDecimal("9427000"),
        new BigDecimal("240").multiply(BigDecimal.valueOf(heldQuantity)),
        new BigDecimal("2.1"),
        new BigDecimal("25.2"),
        new BigDecimal("2.4"),
        new BigDecimal("28.8"),
        new BigDecimal("100000"),
        new BigDecimal("30"),
        new BigDecimal("55"),
        null);
  }

  private String render(Map<UUID, Integer> currentQuantities) {
    return render(currentQuantities, Map.of());
  }

  private String render(
      Map<UUID, Integer> currentQuantities, Map<UUID, BigDecimal> currentAverageBuyPrices) {
    Map<String, Object> params = new HashMap<>();
    params.put("monthlyDividendRows", List.of(snapshotRow(857)));
    params.put("monthlyDividendCurrentQuantities", currentQuantities);
    params.put("monthlyDividendCurrentAverageBuyPrices", currentAverageBuyPrices);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  @Test
  void 스냅샷_수량이_현재와_다르면_현재_수량도_적는다() {
    String html = render(Map.of(STOCK_ITEM_ID, 879));

    assertThat(html).as("스냅샷 수량은 그대로 보여야 한다(행의 다른 숫자가 이 수량 기준이다)").contains("857");
    assertThat(html).as("원장의 현재 수량을 적지 않는다").contains("879");
    assertThat(html).containsPattern("(now 879|현재 879)");
  }

  /** 같으면 아무 말도 하지 않는다. 늘 붙는 표시는 곧 무시된다. */
  @Test
  void 수량이_같으면_덧붙이지_않는다() {
    String html = render(Map.of(STOCK_ITEM_ID, 857));

    assertThat(html).contains("857");
    assertThat(html).doesNotContainPattern("(now 857|현재 857)");
  }

  /** 원장 조회가 비어 있으면(실패 포함) 예전 화면 그대로다. 없는 수량을 지어내지 않는다. */
  @Test
  void 현재_수량을_모르면_예전과_같다() {
    String html = render(Map.of());

    assertThat(html).contains("857");
    assertThat(html).doesNotContainPattern("(now [0-9]|현재 [0-9])");
  }

  /**
   * 상위 템플릿이 실제로 값을 넘겨주는지.
   *
   * <p>조각에 기본값({@code Map.of()})이 있어서, 넘기는 줄이 빠져도 렌더는 성공하고 표시만 조용히 사라진다.
   */
  @Test
  void 상위_템플릿과_컨트롤러가_현재_수량을_넘긴다() throws IOException {
    String parent =
        Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8);
    assertThat(parent)
        .as("simulator.jte 가 조각에 현재 수량을 넘기지 않는다")
        .contains("monthlyDividendCurrentQuantities = monthlyDividendCurrentQuantities");

    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(controller)
        .as("컨트롤러가 모델에 현재 수량을 싣지 않는다")
        .contains("model.addAttribute(\"monthlyDividendCurrentQuantities\"");
    assertThat(controller)
        .as("현재 수량은 종목 단위로 묶어 읽어야 한다")
        .contains("params.add(\"groupBy\", \"STOCKITEM\")");
  }

  /**
   * 평균단가도 스냅샷 저장값이라 원장과 어긋난다.
   *
   * <p>수량보다 파급이 크다 &mdash; 이 값이 원가기준 수익률의 분모라서, 표의 <b>정렬 기준</b>인 종합수익률이 통째로 흔들린다.
   *
   * <p>실측 2026-08-23: 저장 평단이 최대 <b>-5.47%</b> 어긋나(RISE 코리아밸류업위클리 21,047 저장 / 19,896 실제) 원가기준 수익률이
   * -19.18% 로 찍혔지만 실제 평단으로는 -14.51% 였다(<b>4.68%p</b> 차이). 그 결과 종합수익률 내림차순에서 이 종목이 7 위로 밀려 있었지만 실제로는
   * 5 위였다.
   *
   * <p>스냅샷 값을 서버가 덮어쓰지는 않는다 &mdash; 이 표는 "다른 수량/단가로 시뮬레이션" 하는 용도로도 쓰이는 사용자 입력값이다.
   */
  @Test
  void 평단이_현재와_다르면_현재_평단도_적는다() {
    String html = render(Map.of(), Map.of(STOCK_ITEM_ID, new BigDecimal("19896.03")));

    assertThat(html).as("스냅샷 평단은 그대로 보여야 한다").contains("10,000");
    assertThat(html).containsPattern("(now 19,896|현재 19,896)");
  }

  /** 원 단위로 같으면 덧붙이지 않는다(소수점 아래 차이로 늘 경고가 뜨면 무시된다). */
  @Test
  void 평단이_원_단위로_같으면_덧붙이지_않는다() {
    String html = render(Map.of(), Map.of(STOCK_ITEM_ID, new BigDecimal("10000.4")));

    assertThat(html).contains("10,000");
    assertThat(html).doesNotContainPattern("(now 10,000|현재 10,000)");
  }
}
