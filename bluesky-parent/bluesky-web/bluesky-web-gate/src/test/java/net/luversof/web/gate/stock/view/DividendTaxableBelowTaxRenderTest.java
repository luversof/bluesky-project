package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 표에서 <b>세금이 과세표준보다 큰 행</b>을 표시하는지 렌더해서 본다.
 *
 * <p>표는 세금과 과세표준을 나란히 보여 준다. 그런데 세금이 과세표준보다 큰 행이 실제로 있어서, 그대로 읽으면 산술이 성립하지 않는다. 왜 그런지는 관리 화면의 원장
 * 점검({@code DIVIDEND_TAX_EXCEEDS_TAXABLE})에만 있었고 배당 화면은 아무 말 없이 그 숫자를 내보냈다.
 *
 * <p>근거는 그 행 안에 있다. api-stock 이 {@code taxPerShare} 라는 이름으로 내려주는 값은 <b>주당 세금이 아니라 주당 과세표준</b>이다
 * &mdash; 실측 2026-08-24: 값이 있는 배당 177 건 중 {@code 값 x 수량 = 세금} 인 것은 <b>0 건</b>이고, 과세표준·세금이 모두 있는 80
 * 건 중 {@code 값 x 수량 = 과세표준} 인 것이 <b>72 건</b>이다. 남은 8 건이 바로 이 행들이다.
 *
 * <pre>
 *   KODEX 한국부동산리츠인프라  세금 29,210 · 기록 과세표준 2,233 (= 29 x 77 주)
 *                              주당 과세표준 29 x 기록 수량 10,256 = 297,424
 * </pre>
 *
 * <p>숫자는 바꾸지 않는다 &mdash; 어느 쪽이 옳은지는 원장을 고쳐야 정해진다. 대신 그 자리에서 계산할 수 있는 값을 함께 보여 준다.
 */
class DividendTaxableBelowTaxRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendTable.jte";

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

  private static DividendView dividend(String tax, String taxable, String perShare, int quantity) {
    return new DividendView(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
        "한국투자증권 위탁",
        UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
        "KODEX 한국부동산리츠인프라",
        quantity,
        bd("29"),
        bd("297424"),
        bd(tax),
        bd(taxable),
        bd(perShare),
        bd("268214"),
        Instant.parse("2026-08-19T00:00:00Z"),
        Instant.parse("2026-08-19T00:00:00Z"),
        bd("4000"),
        bd("4200"),
        bd("43075200"),
        bd("41024000"),
        bd("0.62"),
        bd("0.65"));
  }

  private String render(List<DividendView> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("dividendList", rows);
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put("zone", ZoneId.of("Asia/Seoul"));
    model.put("dateFormatter", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    model.put(
        "percentFormat",
        (Function<BigDecimal, String>)
            value -> value == null ? "-" : String.format("%.2f%%", value.doubleValue()));
    model.put("payDateLabel", "지급일");
    model.put("stockLabel", "종목");
    model.put("accountLabel", "계좌");
    model.put("grossAmountLabel", "세전");
    model.put("netAmountLabel", "세후");
    model.put("quantityLabel", "수량");
    model.put("referencePriceLabel", "기준가");
    model.put("basisCostLabel", "기준일 원금");
    model.put("yieldOnBasisCostLabel", "수익률");
    model.put("taxLabel", "세금");
    model.put("taxableAmountLabel", "과세표준");
    model.put("sortField", "payDate");
    model.put("sortDir", "desc");
    model.put("emptyLabel", "없음");

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 세금이_과세표준보다_큰_행을_표시하고_근거를_적는다() {
    String html = render(List.of(dividend("29210", "2233", "29", 10256)));

    assertThat(html).as("배당 표를 그리지 못했다 - 검사가 무력해진다").contains("KODEX 한국부동산리츠인프라");
    assertThat(html).as("세금이 과세표준보다 큰 행을 표시하지 않는다").contains("data-taxable-below-tax");
    assertThat(html)
        .as("무엇이 이상한지·근거가 얼마인지 적지 않는다")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.dividend.taxable.below.tax"),
                "2,233",
                "29,210",
                "297,424"));
  }

  @Test
  void 정상_행에는_표시하지_않는다() {
    // 주당 과세표준 29 x 10,256 = 297,424 가 그대로 기록된 정상 행. 늘 뜨는 경고는 곧 무시된다.
    String html = render(List.of(dividend("29210", "297424", "29", 10256)));

    assertThat(html).as("배당 표를 그리지 못했다").contains("297,424");
    assertThat(html).as("정상 행에 경고가 붙었다").doesNotContain("data-taxable-below-tax");
  }

  /**
   * 과세표준이 0 인 행은 대상이 아니다.
   *
   * <p>비과세 계좌나 과세표준을 적지 않은 행은 과세표준이 0 이다 &mdash; 실측 2026-08-24: 193 건 중 <b>113 건</b>이 그렇다. 0 을
   * "세금보다 작다"로 잡으면 세금이 하나라도 적힌 순간 그 113 건이 통째로 경고가 되고, 경고는 곧 무시된다.
   *
   * <p>지금 원장에는 "과세표준 0 인데 세금 > 0" 인 행이 없다(0 건). 그래서 이 경우는 실데이터로는 재현되지 않고, 여기서 만들어 넣어 못박는다.
   */
  @Test
  void 과세표준이_0_이면_세금이_있어도_대상이_아니다() {
    String html = render(List.of(dividend("29210", "0", "29", 10256)));

    assertThat(html).as("배당 표를 그리지 못했다 - 검사가 무력해진다").contains("29,210");
    assertThat(html).as("과세표준 0 인 행에 경고가 붙었다").doesNotContain("data-taxable-below-tax");
  }
}
