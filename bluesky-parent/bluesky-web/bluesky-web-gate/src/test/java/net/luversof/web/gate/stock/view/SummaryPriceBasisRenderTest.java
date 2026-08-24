package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
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
 * 요약(대시보드) 화면을 실제로 렌더해 "평가 기준 종가일"이 나오는지 본다.
 *
 * <p>소스에 문구가 있는지 훑는 것만으로는 조건이 잘못 걸려 화면에 안 나오는 경우를 못 잡는다. 실제로 {@code holdings-snapshot} 은 안내를 갖고
 * 있으면서도 사용자가 날짜를 직접 고른 경우에만 그리도록 조건이 걸려 있어, 기본 화면에서는 나오지 않는다.
 *
 * <p>이 조각은 필수 파라미터가 있어 빈 상태 렌더 검사({@link StockFragmentRenderTest})의 대상이 아니므로 여기서 값을 채워 그린다.
 */
class SummaryPriceBasisRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/summary.jte";

  /**
   * 실제 메시지 번들을 물린다.
   *
   * <p>{@code MessageUtil} 은 정적 접근자가 비면 빈 문자열을 돌려준다. 그 상태로 렌더하면 안내 문구가 통째로 빈 칸이 되어, 날짜가 나오는지 확인할 수
   * 없다(처음에 이 상태로 만들어 헛되이 실패했다). 번들을 물려 배포되는 문구 그대로 검증한다.
   */
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

  private Map<String, Object> params(LocalDate priceBasisDate) {
    Map<String, Object> params = new HashMap<>();
    params.put("totalAsset", new BigDecimal("1493281835"));
    params.put("totalUnrealizedProfit", new BigDecimal("861058009"));
    params.put("totalRealizedProfit", new BigDecimal("225584549"));
    params.put("totalDividend", new BigDecimal("61646257"));
    params.put("winRate", 0.62);
    params.put("displayPrincipal", new BigDecimal("621595902"));
    params.put("displayCurrentEvaluationProfit", new BigDecimal("871685932"));
    params.put("combinedAdjustmentAmount", BigDecimal.ZERO);
    params.put("holdingFeeAdjustment", BigDecimal.ZERO);
    params.put("manualPrincipalAdjustment", BigDecimal.ZERO);
    params.put("priceBasisDate", priceBasisDate);
    return params;
  }

  private String render(LocalDate priceBasisDate) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render(TEMPLATE, params(priceBasisDate), output);
    return output.toString();
  }

  @Test
  void 기준일이_있으면_총자산_옆에_그_날짜를_적는다() {
    String html = render(LocalDate.parse("2026-08-20"));

    assertThat(html).as("총자산이 어느 날 종가로 계산됐는지 화면에 없다. 사용자가 실시간 시세로 오해한다").contains("2026-08-20");
    assertThat(html)
        .as("문구 자체가 배포 번들에서 오지 않았다(치환만 되고 안내가 비었다)")
        .containsPattern("(Valued at 2026-08-20 close|평가 기준 2026-08-20 종가)");
  }

  /** 근거가 없는데 날짜를 지어내면 오히려 더 나쁘다. */
  @Test
  void 기준일이_없으면_아무_날짜도_적지_않는다() {
    String html = render(null);

    assertThat(html).doesNotContain("2026-08-20");
    assertThat(html)
        .as("기준일이 없을 때 안내 문구가 남으면 안 된다")
        .doesNotContain("Valued at")
        .doesNotContain("평가 기준");
  }

  /** 안내가 없어도 화면 자체는 정상이어야 한다(총자산 숫자는 그대로 나온다). */
  @Test
  void 기준일_유무와_무관하게_총자산은_그려진다() {
    for (LocalDate basis : new LocalDate[] {LocalDate.parse("2026-08-20"), null}) {
      String html = render(basis);
      assertThat(html).as("기준일=" + basis).doesNotContain("NaN").doesNotContain("Infinity");
      assertThat(html).as("기준일=" + basis).isNotEmpty();
    }
  }
}
