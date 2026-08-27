package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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
import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow;

/**
 * 계좌별 실현손익이 그 계좌의 매매와 맞지 않을 때 그 사실을 밝히는지 본다.
 *
 * <p>화면에 찍는 실현손익은 매도 거래에 <b>기록된</b> 값이다. 그런데 그 값은 계좌를 합친 원가를 따른다 &mdash; 실측 2026-08-23: 매도 54 건을
 * 원장에서 재계산해 보니 종목 단위(계좌 합산) 원가로는 50 건(92%)이 재현되는데 계좌x종목 단위로는 38 건(70%)뿐이다.
 *
 * <p>그래서 계좌별 배분이 실제 그 계좌의 성과와 크게 다를 수 있다 &mdash; 연금저축1 은 그 계좌 매매 기준이 화면값의 5 배이고, ISA 는 반대로 화면값이 그
 * 계좌 기준의 104 배다. 합계로는 0.11% 차이인데, 매매 화면 헤드라인과 계좌별 표가 어긋난다고 오래 적혀 있던 그 값이다.
 *
 * <p>아래 금액은 실제 값이 아니라 그 갈림 폭만 지킨 표본이다.
 *
 * <p>값 자체는 다른 화면과 맞추려고 기록값을 그대로 두고, <b>크게 갈릴 때만</b> 이 계좌 기준 값을 함께 적는다.
 */
class RealizedBasisGapTest {

  private static final String FRAGMENT = "stock/htmx/fragments/trade/tradeRealizedSections.jte";

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

  private String render(String name, String recorded, String ownBasis) {
    AccountRealizedRow row =
        new AccountRealizedRow(
            name,
            new BigDecimal("12000000"),
            new BigDecimal(recorded),
            new BigDecimal("9000000"),
            new BigDecimal(ownBasis),
            UUID.randomUUID());
    Map<String, Object> params = new HashMap<>();
    params.put("accountRealizedList", List.of(row));
    params.put("stockRealizedList", List.of());
    params.put("realizedWinCount", 1L);
    params.put("realizedStockCount", 1);
    params.put("decimalFormat", new DecimalFormat("#,##0"));
    params.put("accountLabel", "계좌");
    params.put("stockLabel", "종목");
    params.put("realizedProfitLabel", "실현손익");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(FRAGMENT, params, output);
    return output.toString();
  }

  /** 연금저축1 과 같은 모양: 계좌 기준이 기록값의 5 배. */
  @Test
  void 계좌_기준과_크게_갈리면_그_값을_함께_적는다() {
    String html = render("한국투자증권 연금저축1", "400000", "2000000");

    assertThat(html)
        .as("기록값만 보이면 그 계좌가 5배 못 번 것처럼 읽힌다")
        .contains("data-realized-basis-gap=\"1600000\"");
    assertThat(html).contains("+2,000,000");
    // 화면에 찍는 값 자체는 기록값 그대로여야 다른 화면과 맞는다.
    assertThat(html).contains("+400,000");
  }

  /** ISA 와 같은 모양: 기록값이 계좌 기준의 100 배(반대 방향). */
  @Test
  void 반대_방향으로_갈려도_적는다() {
    String html = render("한국투자증권 ISA", "1500000", "15000");

    assertThat(html).contains("data-realized-basis-gap=\"1485000\"");
    assertThat(html).contains("+15,000");
  }

  /** 위탁 계좌와 같은 모양: 차이가 절대액으로는 크지만 값의 0.01% 라 적지 않는다 &mdash; 모든 계좌에 붙으면 안내가 무뎌진다. */
  @Test
  void 값에_비해_작은_차이는_적지_않는다() {
    String html = render("한국투자증권 위탁", "200000000", "199980000");

    assertThat(html).doesNotContain("data-realized-basis-gap");
  }

  /** 동양증권과 같은 모양: 두 기준이 정확히 같다. */
  @Test
  void 두_기준이_같으면_적지_않는다() {
    String html = render("동양증권", "30000000", "30000000");

    assertThat(html).doesNotContain("data-realized-basis-gap");
  }

  /**
   * 임계 규칙 자체를 실측 6 계좌로 고정한다.
   *
   * <p>화면 두 곳(매매 화면 계좌별 표 / 계좌 상세)이 같은 규칙을 써야 한 곳에만 안내가 뜨는 일이 없다. 규칙이 한 곳에 있는지도 함께 지킨다.
   */
  @Test
  void 임계_규칙은_실측과_같은_모양의_계좌를_그대로_가른다() {
    // 밝혀야 하는 계좌
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("400000"), new BigDecimal("2000000")))
        .as("연금저축1 - 5 배 차이")
        .isTrue();
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("1500000"), new BigDecimal("15000")))
        .as("ISA - 100 배 차이")
        .isTrue();
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("480000"), new BigDecimal("150000")))
        .as("연금저축2")
        .isTrue();

    // 밝히지 않아야 하는 계좌
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("200000000"), new BigDecimal("199980000")))
        .as("위탁 - 절대액 차이는 크지만 값의 0.01%")
        .isFalse();
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("9000"), new BigDecimal("500")))
        .as("KB - 비율은 크지만 금액이 작다")
        .isFalse();
    assertThat(
            net.luversof.web.gate.stock.util.RealizedBasisGap.isNotable(
                new BigDecimal("30000000"), new BigDecimal("30000000")))
        .as("동양 - 두 기준이 같다")
        .isFalse();
  }

  /** 계좌 상세 화면도 같은 규칙을 쓴다. 한 화면에만 붙으면 다른 화면은 여전히 조용하다. */
  @Test
  void 계좌_상세도_같은_규칙을_쓴다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/accountDetail.jte"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(source)
        .as("계좌 상세 헤드라인도 계좌를 합친 원가를 따르므로 같은 안내가 필요하다")
        .contains("RealizedBasisGap.isNotable(realizedProfit, realizedProfitOwnBasis)")
        .contains("stock.trade.realized.basis.gap");
    String controller =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(controller).contains("realizedProfitOwnBasis");
  }

  /**
   * 종목 상세에는 붙이지 않는다.
   *
   * <p>기록값은 종목 단위 기준이라 종목 축에서는 어긋나지 않는다 &mdash; 실측 2026-08-23: 36 종목 전부 차이가 (삼성전자 기준) 값의 0.009%)이고
   * 임계를 넘는 종목은 0 개다. 그래도 붙이면 늘 꺼져 있는 코드가 남는다.
   */
  @Test
  void 종목_상세에는_붙이지_않는다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/stockItemDetail.jte"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(source).doesNotContain("RealizedBasisGap");
  }

  /** KB증권 위탁과 같은 모양: 비율은 크지만 금액이 임계값(10,000 원) 미만이라 적지 않는다. */
  @Test
  void 금액이_작으면_적지_않는다() {
    String html = render("KB증권 위탁", "9438", "570");

    assertThat(html).doesNotContain("data-realized-basis-gap");
  }
}
