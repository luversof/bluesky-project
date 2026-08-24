package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 자산현황 표를 <b>실제로 그려서</b> 합계행이 본문 행의 합과 맞는지 본다.
 *
 * <p>이 표는 지금까지 <b>소스를 읽어서만</b> 확인했다(계좌 행은 {@code (평가액 − 기준원금) / 기준원금}, 합계행은 {@code (Σ평가액 − Σ기준원금) /
 * Σ기준원금} 으로 같은 정의다). 그런데 같은 종류의 결함이 배당 수익률 표에서 실제로 있었다 &mdash; 행과 합계행이 다른 분자를 쓰고 있었고, 소스를 읽는 검사는
 * <b>식을 다르게 고쳐 적으면</b> 빠져나간다(2026-08-24 확인).
 *
 * <p>그래서 여기서는 조각을 렌더해 나온 숫자를 파싱해 맞춘다.
 *
 * <p><b>기준원금은 매수금액이 아니다.</b> 계좌 설정에 수동 입력값이 있으면 그 값이고, 없으면 보유분 원가로 떨어진다. 이 사용자의 6 계좌는 지금 수동 원금이 모두
 * 비어 있어(실측) 둘이 우연히 가까울 수 있으므로, 이 검사는 <b>일부러 다른 값</b>을 넣어 두 분모가 갈라지게 만든다.
 */
class AssetStatusFooterRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetStatus.jte";

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

  /** 자산현황이 읽는 값만 채운 계좌 행. 나머지는 0/null 로 둔다. */
  private static TradeProfit account(
      UUID accountId, String name, String buyCost, String evaluation, String evaluationProfit) {
    return new TradeProfit(
        null,
        null,
        accountId,
        name,
        bd(buyCost),
        BigDecimal.ZERO,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        BigDecimal.ZERO,
        bd(evaluation),
        bd(evaluationProfit),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(buyCost),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        LocalDate.parse("2026-08-19"));
  }

  /** 종목 표가 읽는 값만 채운 행. */
  private static TradeProfit stock(
      String name, String buyCost, String evaluation, String evaluationProfit, int quantity) {
    return new TradeProfit(
        UUID.randomUUID(),
        name,
        null,
        null,
        bd(buyCost),
        BigDecimal.ONE,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        quantity,
        BigDecimal.ONE,
        bd(evaluation),
        bd(evaluationProfit),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(buyCost),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        LocalDate.parse("2026-08-19"));
  }

  private String render(
      Map<UUID, TradeProfit> accounts, Map<UUID, BigDecimal> principals, String totalEvaluation) {
    return render(accounts, principals, totalEvaluation, List.of(), "0");
  }

  private String render(
      Map<UUID, TradeProfit> accounts,
      Map<UUID, BigDecimal> principals,
      String totalEvaluation,
      List<TradeProfit> stocks,
      String totalEvaluationProfit) {
    Map<String, Object> model = new HashMap<>();
    model.put("accountTotalMap", accounts);
    model.put("accountProfitBasisMap", principals);
    model.put("manualPrincipalAccountIds", principals.keySet());
    Map<UUID, List<Object>> holdings = new LinkedHashMap<>();
    accounts.keySet().forEach(id -> holdings.put(id, List.of()));
    model.put("accountHoldingMap", holdings);
    model.put("stockItemList", List.of());
    model.put("stockAggregated", stocks);
    model.put("totalEvaluationAmount", bd(totalEvaluation));
    model.put("totalEvaluationProfit", bd(totalEvaluationProfit));
    model.put("priceBasisDate", LocalDate.parse("2026-08-19"));

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  private static final Pattern CELL = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);

  private static String text(String cell) {
    return cell.replaceAll("<[^>]*>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
  }

  /** {@code <tfoot>} 안의 셀 글자들. */
  private List<String> footerCells(String html) {
    List<String> cells = new ArrayList<>();
    int at = 0;
    while (true) {
      int start = html.indexOf("<tfoot", at);
      if (start < 0) {
        return cells;
      }
      int end = html.indexOf("</tfoot>", start);
      if (end < 0) {
        return cells;
      }
      Matcher matcher = CELL.matcher(html.substring(start, end));
      while (matcher.find()) {
        cells.add(text(matcher.group(1)));
      }
      at = end;
    }
  }

  @Test
  void 계좌_합계행이_본문_금액의_합과_같다() {
    UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Map<UUID, TradeProfit> accounts = new LinkedHashMap<>();
    accounts.put(first, account(first, "가계좌", "1000000", "1200000", "200000"));
    accounts.put(second, account(second, "나계좌", "3000000", "2700000", "-300000"));
    Map<UUID, BigDecimal> principals = new LinkedHashMap<>();
    // 기준원금을 매수금액과 다르게 둔다 - 두 분모가 갈라져야 이 검사가 의미가 있다.
    principals.put(first, bd("1000000"));
    principals.put(second, bd("4000000"));

    List<String> cells = footerCells(render(accounts, principals, "3900000"));

    assertThat(cells).as("합계행 셀을 찾지 못했다 - 검사가 무력해진다").isNotEmpty();
    assertThat(cells).contains("4,000,000"); // 매수금액 1,000,000 + 3,000,000
    assertThat(cells).contains("3,900,000"); // 평가액 1,200,000 + 2,700,000
    assertThat(cells).contains("5,000,000"); // 기준원금 1,000,000 + 4,000,000
  }

  @Test
  void 계좌_합계행의_수익률이_합의_비율이다() {
    UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Map<UUID, TradeProfit> accounts = new LinkedHashMap<>();
    accounts.put(first, account(first, "가계좌", "1000000", "1200000", "200000"));
    accounts.put(second, account(second, "나계좌", "3000000", "2700000", "-300000"));
    Map<UUID, BigDecimal> principals = new LinkedHashMap<>();
    principals.put(first, bd("1000000"));
    principals.put(second, bd("4000000"));

    String html = render(accounts, principals, "3900000");
    List<String> cells = footerCells(html);

    // 평가손익률 = (200,000 − 300,000) / (1,000,000 + 3,000,000) = −2.5%
    assertThat(String.join(" | ", cells)).as("합계행의 평가손익률이 합의 비율이 아니다").contains("-2.5%");
    // 원금 대비 = (3,900,000 − 5,000,000) / 5,000,000 = −22.0%
    // 매수금액(4,000,000)으로 나누면 −27.5% 가 된다.
    assertThat(String.join(" | ", cells))
        .as("원금 대비 수익률이 기준원금이 아닌 값으로 나뉘었다")
        .contains("-22.0%")
        .doesNotContain("-27.5%");
  }

  /**
   * 종목 표의 합계행은 <b>템플릿이 직접</b> {@code stockAggregated} 를 더해 만든다(계좌 표와 달리 매수금액 합계가 파라미터로 오지 않는다). 그래서
   * 렌더해서 맞춰 볼 값이 실제로 있다.
   */
  @Test
  void 종목_합계행이_본문_금액의_합과_같다() {
    List<TradeProfit> stocks =
        List.of(
            stock("가종목", "1000000", "1500000", "500000", 10),
            stock("나종목", "2000000", "1700000", "-300000", 20));

    List<String> cells =
        footerCells(
            render(new LinkedHashMap<>(), new LinkedHashMap<>(), "3200000", stocks, "200000"));

    assertThat(cells).as("합계행 셀을 찾지 못했다 - 검사가 무력해진다").isNotEmpty();
    // 매수금액 1,000,000 + 2,000,000 (템플릿이 직접 더한다)
    assertThat(cells).contains("3,000,000");
    // 평가액·평가손익은 파라미터로 온다 - 같이 그려지는지만 본다.
    assertThat(cells).contains("3,200,000");
    assertThat(String.join(" | ", cells)).contains("+200,000");
  }
}
