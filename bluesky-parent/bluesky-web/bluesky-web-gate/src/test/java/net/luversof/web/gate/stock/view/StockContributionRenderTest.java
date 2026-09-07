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
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.util.StockContributionUtil;
import net.luversof.web.gate.stock.util.StockContributionUtil.Contribution;

/**
 * 자산 성장의 <b>종목별 기여</b> 표를 렌더해서 본다.
 *
 * <p>이 화면은 기간 손익을 시간으로만 쪼갤 수 있었다. 종목으로 쪼갤 곳이 없어 "이 기간에 누가 벌어줬나" 는 답이 없었고, 그 기간에 다 판 종목은 자산 현황(보유 중인
 * 것만)에도 나오지 않아 종목 단위로 통째로 사라졌다.
 */
class StockContributionRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/stockContributionTable.jte";
  private static final UUID HELD = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID SOLD = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

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

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static HoldingsSnapshotItem snap(UUID id, String name, String qty, String unrealized) {
    return new HoldingsSnapshotItem(
        id,
        name,
        "000000",
        bd(qty),
        bd("1000"),
        bd("1100"),
        LocalDate.parse("2026-09-07"),
        bd("11000"),
        bd(unrealized));
  }

  private static TradeProfit realized(UUID id, String name, String amount) {
    return TradeProfit.ofStockStatus(
        id,
        name,
        bd("1000"),
        0,
        bd("1100"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(amount),
        BigDecimal.ZERO);
  }

  /** 보유 중인 종목 하나(기여 700) + 그 기간에 다 판 종목 하나(기여 300). 합계 1,000. */
  private static List<Contribution> rows() {
    return StockContributionUtil.of(
        List.of(snap(HELD, "보유종목", "10", "0"), snap(SOLD, "판종목", "10", "200")),
        List.of(snap(HELD, "보유종목", "10", "700")),
        List.of(realized(SOLD, "판종목", "400")),
        Map.of(SOLD, bd("100")));
  }

  private String render(List<Contribution> contributions) {
    Map<String, Object> model = new HashMap<>();
    model.put("stockContributions", contributions);
    model.put("stockContributionTotal", StockContributionUtil.total(contributions));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 종목마다_기여와_쪼갠_값을_적는다() {
    String html = render(rows());

    assertThat(html)
        .as("기간 손익을 종목으로 쪼갤 곳이 없었다")
        .contains("data-stock-contribution")
        .contains(MessageUtil.getMessage("stock.asset.growth.contribution.title"))
        .contains("보유종목")
        .contains("판종목");

    assertThat(html)
        .as("보유종목 0->700 = +700 / 판종목 -200 + 400 + 100 = +300")
        .contains("+&#8361;700")
        .contains("+&#8361;300");
  }

  /**
   * 그 기간에 <b>다 판</b> 종목을 밝힌다.
   *
   * <p>밝히지 않으면 왜 자산 현황에는 없는 종목이 여기 있는지 알 수 없다.
   */
  @Test
  void 기간에_다_판_종목을_밝힌다() {
    String html = render(rows());

    assertThat(html).contains(MessageUtil.getMessage("stock.asset.growth.contribution.sold.out"));
    int sold = html.indexOf("판종목");
    int held = html.indexOf("보유종목");
    assertThat(html.substring(Math.min(sold, held)))
        .as("보유 중인 종목에까지 '매도 완료' 가 붙으면 안 된다")
        .isNotBlank();
  }

  /** 합계는 위 카드의 기간 손익과 같아야 한다. 실측 2026-09-07: 세 기간 모두 차이 0. */
  @Test
  void 합계는_기여의_합이다() {
    String html = render(rows());

    assertThat(html).contains("data-stock-contribution-total");
    String foot = html.substring(html.indexOf("data-stock-contribution-total"));
    assertThat(foot).as("700 + 300 = 1,000").contains("+&#8361;1,000").contains("100.0%");
  }

  /** 비중은 기간 손익 대비다. 손익이 0 이하면 음의 기여가 양의 비중으로 뒤집히므로 적지 않는다. */
  @Test
  void 기간_손익이_양수가_아니면_비중을_적지_않는다() {
    List<Contribution> losing =
        StockContributionUtil.of(
            List.of(snap(HELD, "손실종목", "10", "0"), snap(SOLD, "다른종목", "10", "0")),
            List.of(snap(HELD, "손실종목", "10", "-900"), snap(SOLD, "다른종목", "10", "100")),
            List.of(),
            Map.of());

    String html = render(losing);
    assertThat(html).as("합계가 음수인데 비중을 적으면 부호가 뒤집혀 읽힌다").doesNotContain("100.0%");
  }

  /** 종목이 하나뿐이면 위의 기간 손익 카드를 그대로 되풀이할 뿐이라 그리지 않는다. */
  @Test
  void 종목이_하나뿐이면_그리지_않는다() {
    List<Contribution> single =
        StockContributionUtil.of(null, List.of(snap(HELD, "하나", "10", "700")), List.of(), Map.of());

    assertThat(render(single).trim()).isEmpty();
  }

  @Test
  void 자료가_없으면_아무것도_그리지_않는다() {
    assertThat(render(List.of()).trim()).isEmpty();
  }

  /** 조각만 만들고 화면에 붙이지 않으면 없는 것과 같다. */
  @Test
  void 자산_성장_화면이_표를_부른다() throws IOException {
    String page =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/asset-growth.jte"), StandardCharsets.UTF_8);

    assertThat(page)
        .as("조각만 만들고 화면에 붙이지 않으면 없는 것과 같다")
        .contains("stockContributionTable")
        .contains("stockContributions = stockContributions")
        .contains("stockContributionTotal = stockContributionTotal");
  }

  /**
   * 계좌·종목 필터를 스냅샷에도 넘기는지.
   *
   * <p>스냅샷 엔드포인트는 원래 계좌 하나만 받았다. 그대로 뒀으면 필터를 건 화면에서 <b>평가 변동만 전 계좌를 보게</b> 되어 합계가 기간 손익과 어긋났다.
   * api-stock 이 다중 선택을 받도록 고치고 게이트가 넘긴다(실측 2026-09-07: 계좌 하나로 좁혀도 차이 0).
   */
  @Test
  void 계좌와_종목_필터를_스냅샷에도_넘긴다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller)
        .as("필터를 안 넘기면 평가 변동만 전 계좌를 보게 되어 합계가 기간 손익과 어긋난다")
        .contains("snapshotParams.add(\"accountIdList\"")
        .contains("snapshotParams.add(\"stockItemIdList\"");
  }

  /** 접힌 나머지는 '기타 N종목' 한 줄로 낸다. 접어도 합계는 그대로여야 한다. */
  @Test
  void 접힌_나머지를_기타_한_줄로_낸다() {
    List<Contribution> all = rows();
    var folded = StockContributionUtil.fold(all, 1);

    Map<String, Object> model = new HashMap<>();
    model.put("stockContributions", folded.rows());
    model.put("stockContributionTotal", StockContributionUtil.total(all));
    model.put("stockContributionOthers", folded.others());
    model.put("stockContributionOthersCount", folded.othersCount());
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    String html = output.toString();

    assertThat(html).contains("data-stock-contribution-others");
    assertThat(html)
        .as("접힌 줄 수를 밝히지 않으면 표가 자료를 빠뜨린 것처럼 보인다")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.growth.contribution.others"), "1"));

    String foot = html.substring(html.indexOf("data-stock-contribution-total"));
    assertThat(foot).as("접어도 합계는 접기 전 전체다").contains("+&#8361;1,000");
  }

  /** 접을 것이 없으면 '기타' 줄을 내지 않는다. */
  @Test
  void 접을_것이_없으면_기타_줄이_없다() {
    assertThat(render(rows())).doesNotContain("data-stock-contribution-others");
  }

  /**
   * 화면이 실제로 <b>접어서</b> 내보내는지.
   *
   * <p>접지 않으면 전 기간이 44 줄이 된다(실측 2026-09-07: 그중 25 줄이 100 만원 미만). 유틸에 접기를 만들어 두고 컨트롤러가 안 쓰면 아무 일도
   * 일어나지 않는데, 렌더 검사는 모델을 직접 채우므로 그것을 알아채지 못한다.
   */
  @Test
  void 컨트롤러가_접어서_내보낸다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller)
        .as("접기를 만들어 두고 안 쓰면 표가 44 줄로 나간다")
        .contains("StockContributionUtil.fold(")
        .contains("StockContributionUtil.DEFAULT_VISIBLE")
        .as("합계는 접기 전 전체를 더한 값이어야 위 카드와 맞는다")
        .contains("StockContributionUtil.total(contributions)");

    assertThat(controller).as("이름을 채우지 않으면 이미 다 판 종목이 전부 '-' 로 나간다").contains("contributionNames");
  }
}
