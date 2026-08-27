package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

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
import net.luversof.web.gate.stock.controller.StockSummaryHtmxController.AllocationBarRow;

/**
 * 비중 막대의 표시 규칙을 고정한다.
 *
 * <p>실측 2026-08-23: 보유 9 종목의 원시 비중 합은 정확히 100.000000% 이고, 상위 5 종목 + 기타의 금액 합도 총 평가액과 <b>차이 0</b>
 * 이다. 다만 화면은 소수 1 자리로 찍으므로 눈으로 더하면 100.1% 가 된다(83.6 + 5.1 + 4.6 + 3.8 + 1.5 + 1.5).
 *
 * <p>이 화면에는 <b>합계 행이 없다</b>. 그래서 반올림 잔차가 화면에서 모순으로 드러나지 않는다. 합계를 붙이려면 그 잔차를 어디에 태울지 먼저 정해야 한다
 * &mdash; 기타 행에 태우면 열 합은 맞지만 그 행의 값이 틀려진다(1.46% 를 1.4 로 적게 된다). 지금은 각 행이 제 값을 지키는 쪽을 택했다.
 */
class AllocationBarsRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/allocationBars.jte";

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

  /**
   * 실측 2026-08-23 의 상위 5 종목 <b>구성</b>(한 종목이 8 할 넘게 쏠린 모양). 금액은 표본값이다.
   *
   * <p>이 검사가 재는 것은 비중이 극단적으로 갈릴 때 막대와 라벨이 어떻게 그려지는지라, 실제 평가액이어야 할 이유가 없다.
   */
  private static final List<AllocationBarRow> TOP_ROWS =
      List.of(
          new AllocationBarRow(UUID.randomUUID(), "삼성전자", 83.58267, 1_000_000_000L),
          new AllocationBarRow(UUID.randomUUID(), "KODEX 한국부동산리츠인프라", 5.08514, 60_000_000L),
          new AllocationBarRow(UUID.randomUUID(), "RISE 200위클리커버드콜", 4.63238, 55_000_000L),
          new AllocationBarRow(UUID.randomUUID(), "TIGER 리츠부동산인프라", 3.76829, 45_000_000L),
          new AllocationBarRow(UUID.randomUUID(), "TIGER 배당커버드콜액티브", 1.46639, 17_000_000L));

  private static final AllocationBarRow OTHERS_ROW =
      new AllocationBarRow(null, "기타 4종목", 1.46335, 17_000_000L);

  private String render(List<AllocationBarRow> rows, AllocationBarRow others) {
    Map<String, Object> params = new HashMap<>();
    params.put("allocationRows", rows);
    params.put("othersRow", others);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  @Test
  void 비중은_소수_한_자리로_찍는다() {
    String html = render(TOP_ROWS, OTHERS_ROW);

    // 라벨만 본다. "83.6%" 만 찾으면 막대 너비(style="width: 83.6%")에도 걸려, 라벨 서식이 바뀌어도
    // 통과한다(실측: 라벨을 %.0f 로 바꾼 변이가 검출되지 않았다).
    assertThat(html).contains(">83.6%<").contains(">5.1%<").contains(">4.6%<").contains(">3.8%<");
    // 상위 5위와 기타가 반올림하면 둘 다 1.5% 다. 서로 다른 값(1.46639 / 1.46335)이라도 표기는 같다.
    assertThat(html).contains(">1.5%<");
  }

  @Test
  void 막대_너비와_표기가_같은_값을_쓴다() {
    String html = render(TOP_ROWS, OTHERS_ROW);

    assertThat(html).as("막대 길이와 옆의 숫자가 다르면 어느 쪽이 맞는지 알 수 없다").contains("width: 83.6%");
  }

  @Test
  void 기타가_없으면_그_행을_그리지_않는다() {
    String html = render(TOP_ROWS, null);

    assertThat(html).doesNotContain("기타");
    assertThat(html).contains("삼성전자");
  }

  /** 보유가 없으면 빈 상태를 그린다. 카드 자체는 항상 나와야 한다(outerHTML 스왑 대상). */
  @Test
  void 보유가_없어도_카드는_나온다() {
    String html = render(List.of(), null);

    assertThat(html).doesNotContain("width:");
    assertThat(html).as("스왑 대상이 사라지면 이후 갱신이 붙을 곳이 없다").contains("card");
  }

  @Test
  void 종목_id_가_없으면_링크를_걸지_않는다() {
    String html = render(TOP_ROWS, OTHERS_ROW);

    // 기타 행은 id 가 없다 - 링크가 아니라 그냥 글자여야 한다.
    int othersAt = html.indexOf("기타 4종목");
    assertThat(othersAt).isGreaterThan(-1);
    assertThat(html.substring(Math.max(0, othersAt - 200), othersAt))
        .as("id 가 없는 행에 링크를 걸면 /stock/item?stockItemId= 로 빈 링크가 나간다")
        .doesNotContain("/stock/item?stockItemId=");
  }
}
