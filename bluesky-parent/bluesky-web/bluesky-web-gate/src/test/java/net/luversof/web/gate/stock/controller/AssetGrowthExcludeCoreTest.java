package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 자산 성장의 '핵심 보유 제외' 토글.
 *
 * <p>실측 2026-09-08: 한 종목이 평가액의 83.9% 라 이 화면의 차트·기간 손익이 사실상 그 종목 주가였다. 빼고 보면 배당 ETF 들의 성적이 드러난다. 무엇이
 * 핵심인지는 종목의 '핵심' 태그가 정한다.
 */
class AssetGrowthExcludeCoreTest {

  /**
   * 폼의 hidden(현재 제외 중)과 누른 버튼의 값이 함께 온다.
   *
   * <p>해제 버튼은 hidden 이 살아 있는 채로 눌리므로 {@code ["true","false"]} 가 온다 &mdash; 거짓이 하나라도 있으면 해제다.
   */
  @Test
  void 거짓_값이_하나라도_있으면_해제다() {
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of("true", "false")))
        .as("제외 중(hidden) + 전체 보기 버튼 = 해제")
        .isFalse();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(Arrays.asList("false", "true")))
        .isFalse();
  }

  @Test
  void 모두_참이면_제외한다() {
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of("true"))).isTrue();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of("true", "true")))
        .as("hidden 과 버튼이 둘 다 제외면 제외")
        .isTrue();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of("TRUE"))).isTrue();
  }

  @Test
  void 없거나_알_수_없는_값이면_제외하지_않는다() {
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(null)).isFalse();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of())).isFalse();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of(""))).isFalse();
    assertThat(StockAssetGrowthHtmxController.resolveExcludeCore(List.of("yes"))).isFalse();
  }

  /** 템플릿: 태그를 단 종목이 있을 때만 토글을 내고, 제외 중이면 hidden 으로 상태를 잇는다. */
  @Test
  void 템플릿은_상태에_맞는_버튼과_hidden_을_낸다() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/asset-growth.jte"), StandardCharsets.UTF_8);

    assertThat(source)
        .as("태그를 단 종목이 없으면 토글 자체가 없다")
        .contains("@if(coreHoldingTagged)")
        .contains("data-exclude-core=\"active\"")
        .contains("data-exclude-core-clear")
        .contains("data-exclude-core=\"available\"")
        .contains("data-exclude-core-apply")
        .as("기간을 바꿔도 제외 상태가 남아야 한다")
        .contains("<input type=\"hidden\" name=\"excludeCore\" value=\"true\">");
    assertThat(source)
        .as("제외 버튼은 true 를, 해제 버튼은 false 를 hx-vals 로 낸다(hx-include 로 폼의 기간 값과 함께)")
        .contains("hx-vals='{\"excludeCore\": \"true\"}'")
        .contains("hx-vals='{\"excludeCore\": \"false\"}'");
    // 폼 밖 form= 제출 버튼은 htmx 가 submitter 값을 싣지 않는다(실측 2026-09-08: 요청에 파라미터가 없었다).
    assertThat(source).doesNotContain("form=\"assetGrowthSearchForm\" name=\"excludeCore\"");
    assertThat(source).as("종목 id 로 지목하던 옛 방식은 남아 있지 않다").doesNotContain("excludeStockItemId");
  }
}
