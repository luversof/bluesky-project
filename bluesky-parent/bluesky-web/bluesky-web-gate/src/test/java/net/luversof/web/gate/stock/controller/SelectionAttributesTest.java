package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

/**
 * 상세 조회 폼 · 필터 배지가 되살릴 선택 상태(StockBaseHtmxController.addSelectionAttributes).
 *
 * <p>2026-10-22 까지 배당 · 매매 · 활동 목록이 같은 다섯 줄을 한 벌씩 적고 있었다. 한 곳만 고치면 그 화면만 폼이 고른 계좌를 잃는다.
 */
class SelectionAttributesTest {

  private static final UUID A1 = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID A2 = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
  private static final UUID S1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
  private static final UUID S2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

  @Test
  void 고른_것과_첫_번째_값을_싣는다() {
    ExtendedModelMap model = new ExtendedModelMap();
    StockBaseHtmxController.addSelectionAttributes(
        model, List.of(A1, A2), List.of(S1, S2), List.of("배당주"));
    assertThat(model.get("selectedAccountIds")).isEqualTo(List.of(A1, A2));
    assertThat(model.get("selectedStockItemIds")).isEqualTo(List.of(S1, S2));
    assertThat(model.get("selectedStockTags")).isEqualTo(List.of("배당주"));
    assertThat(model.get("selectedAccountId")).isEqualTo(A1);
    assertThat(model.get("selectedStockItemId")).isEqualTo(S1);
  }

  @Test
  void 고르지_않았으면_빈_목록이고_첫_번째_값은_없다() {
    ExtendedModelMap model = new ExtendedModelMap();
    StockBaseHtmxController.addSelectionAttributes(model, null, List.of(), null);
    assertThat(model.get("selectedAccountIds")).isEqualTo(List.of());
    assertThat(model.get("selectedStockItemIds")).isEqualTo(List.of());
    // 키는 있고 값이 null 이어야 템플릿 인자가 "고르지 않음" 으로 읽힌다
    assertThat(model.containsAttribute("selectedAccountId")).isTrue();
    assertThat(model.get("selectedAccountId")).isNull();
    assertThat(model.get("selectedStockItemId")).isNull();
    assertThat(model.containsAttribute("selectedStockTags")).isTrue();
  }

  @Test
  void 세_목록_컨트롤러가_도우미를_쓴다() throws IOException {
    Path dir = Path.of("src/main/java/net/luversof/web/gate/stock/controller");
    String dividend =
        Files.readString(dir.resolve("StockDividendHtmxController.java"), StandardCharsets.UTF_8);
    String trade =
        Files.readString(dir.resolve("StockTradeHtmxController.java"), StandardCharsets.UTF_8);
    assertThat(dividend.split("addSelectionAttributes\\(", -1)).as("배당 목록").hasSize(2);
    assertThat(trade.split("addSelectionAttributes\\(", -1)).as("매매 · 활동 목록").hasSize(3);
    for (String source : List.of(dividend, trade)) {
      assertThat(source)
          .doesNotContain("\"selectedAccountId\"")
          .doesNotContain("\"selectedStockItemId\"");
    }
  }
}
