package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import net.luversof.web.gate.stock.dto.response.DataStatusResponse;

/**
 * 관리 화면의 시세 품질 줄이 <b>하루 만에 제한폭을 넘은 행</b>을 보여 주는지 렌더해서 본다.
 *
 * <p>기존 점검은 <b>거래량이 0 인 행</b>만 봤다. 그런데 액면분할은 보통 거래가 재개되며 거래량이 붙으므로 그 그물에 걸리지 않는다 &mdash; 지금까지 걸린
 * 유일한 행(쌍방울 2025-05-08, 정확히 1/5)은 거래량이 우연히 0 이었을 뿐이다. 소급 조정되지 않은 기업행위는 그 날 이전 구간의 평가액을 배율만큼 통째로
 * 어긋나게 한다.
 *
 * <p>한국 시장의 일일 제한폭은 &plusmn;30% 라, 그걸 넘는 하루 변동은 거래로 생길 수 없다. 다만 <b>정리매매와 상하한가</b>는 제한폭이 적용되지 않거나 그
 * 자체가 정상이므로 "오류"라고 단정하면 안 된다. 그래서 배율이 정수비에 붙을 때만 분할이라고 적고, 아니면 숫자만 보여 사람이 가리게 한다.
 *
 * <p>실측 2026-08-24(실데이터 5행): 쌍방울 2025-05-08 13,450 &rarr; 2,690 (-80.0%, 1:5) / 쌍방울 2025-11-19
 * 2,690 &rarr; 885 (-67.1%) / 2025-11-20 885 &rarr; 512 (-42.1%) / 2025-11-27 690 &rarr; 450
 * (-34.8%) / 한화오션 2015-07-15 55,526 &rarr; 38,868 (-30.0%). 분할로 설명되는 것은 첫 행뿐이다.
 */
class PriceLimitBreachRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/adminActions.jte";

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

  private static DataStatusResponse.PriceLimitBreachRow row(
      String name, String date, String previousDate, String previous, String close, double pct) {
    return new DataStatusResponse.PriceLimitBreachRow(
        name,
        LocalDate.parse(date),
        LocalDate.parse(previousDate),
        bd(previous),
        bd(close),
        pct,
        1);
  }

  private String render(long count, List<DataStatusResponse.PriceLimitBreachRow> rows) {
    DataStatusResponse dataStatus =
        new DataStatusResponse(
            null,
            0L,
            null,
            0L,
            LocalDate.parse("2026-08-24"),
            86L,
            LocalDate.parse("2026-08-21"),
            9L,
            9L,
            0L,
            9L,
            57477L,
            1352L,
            0L,
            List.of(),
            null,
            List.of(),
            count,
            rows);
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("dataStatus", dataStatus);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 분할로_설명되는_행은_그_비를_함께_적는다() {
    String html =
        render(1L, List.of(row("쌍방울", "2025-05-08", "2025-05-07", "13450", "2690", -80.0)));

    assertThat(html).as("행을 그리지 못했다 - 검사가 무력해진다").contains("쌍방울");
    assertThat(html).as("변동률을 보여 주지 않으면 얼마나 뛴 것인지 알 수 없다").contains("-80.0%");
    // data-* 속성만 보면 눈에 보이는 문구를 지워도 통과한다(실측: 그 뮤테이션이 살아남았다).
    // 사람이 읽는 문구 자체를 본다.
    assertThat(html)
        .as("정확히 1/5 인데 분할이라고 적지 않았다")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.admin.price.quality.split"), "1:5"));
  }

  @Test
  void 정수비에_붙지_않는_행은_분할이라고_적지_않는다() {
    // 정리매매·하한가는 제한폭이 적용되지 않거나 그 자체가 정상이다. 분할로 단정하면 사람이 잘못 판단한다.
    String html =
        render(
            2L,
            List.of(
                row("쌍방울", "2025-11-19", "2025-11-18", "2690", "885", -67.100372),
                row("한화오션", "2015-07-15", "2015-07-14", "55526", "38868", -30.000360)));

    assertThat(html).contains("-67.1%").contains("-30.0%");
    assertThat(html)
        .as("어떤 정수비에도 붙지 않는 배율인데 분할이라고 적었다")
        .doesNotContain("data-price-limit-split=\"1:3\"")
        .doesNotContain("data-price-limit-split=\"1:2\"");
    assertThat(html).contains("data-price-limit-split=\"\"");
  }

  @Test
  void 걸린_행이_없으면_줄_자체가_나오지_않는다() {
    String html = render(0L, List.of());

    assertThat(html).as("관리 화면을 그리지 못했다 - 검사가 무력해진다").contains("57,477");
    // 패턴 문자열 그대로 찾으면 {0} 이 치환돼 절대 걸리지 않는다(실측: 그래서 뮤테이션이 살아남았다).
    // 0 을 넣어 실제로 그려질 문구를 만들어 본다.
    assertThat(html)
        .as("걸린 행이 없는데 제한폭 줄이 나온다")
        .doesNotContain(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.admin.price.quality.limit"), "0"));
  }
}
