package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockRangePresetUtil;

/**
 * 화면에 넘기는 {@code rangeMode} 는 <b>실제로 적용한</b> 모드여야 한다.
 *
 * <p>{@link StockRangePresetUtil#resolve} 는 모르는 값을 올해(ytd)로 떨어뜨리고 그 사실을 {@code mode()} 로 알려 준다. 자산
 * 성장 화면만 그 값을 "비어 있을 때만" 받아, 모르는 값이 오면 그 값이 그대로 화면까지 갔다.
 *
 * <p>실측 2026-09-13 {@code rangeMode=oops}: 매매 · 배당 · 활동 · 종목 상세 · 계좌 상세는 모두 '올해' 버튼이 눌린 채
 * 2026-01-01~2026-09-13 을 보여 줬는데, <b>자산 성장 조각만</b> 눌린 프리셋이 없고 기간은 같은 올해였다. 데이터와 라벨이 어긋나면 사용자는 지금
 * 무엇을 보고 있는지 알 수 없다.
 */
class AssetGrowthEffectiveRangeModeTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java";

  private String read() throws IOException {
    return Files.readString(Path.of(CONTROLLER), StandardCharsets.UTF_8)
        .replaceAll("[ \t\r\n]+", " ");
  }

  /** 조건 없이 실효 모드를 쓴다. */
  @Test
  void 실효_모드를_조건_없이_받는다() throws IOException {
    assertThat(read()).contains("effectiveRangeMode = preset.mode();");
  }

  /** '비어 있을 때만' 채우는 옛 형태가 남아 있으면 안 된다. */
  @Test
  void 비어_있을_때만_채우던_가지가_없다() throws IOException {
    assertThat(read())
        .doesNotContain("if (effectiveRangeMode == null || effectiveRangeMode.isBlank())");
  }

  /** 화면에는 원래 파라미터가 아니라 실효 모드를 넘긴다. */
  @Test
  void 화면에_실효_모드를_넘긴다() throws IOException {
    assertThat(read()).contains("model.addAttribute(\"rangeMode\", effectiveRangeMode);");
  }

  /** 형제 컨트롤러도 같은 규칙을 쓴다 - 한 곳만 고치면 다시 갈린다. */
  @Test
  void 형제_컨트롤러도_같은_규칙이다() throws IOException {
    for (String sibling :
        new String[] {
          "src/main/java/net/luversof/web/gate/stock/controller/StockTradeHtmxController.java",
          "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java"
        }) {
      assertThat(Files.readString(Path.of(sibling), StandardCharsets.UTF_8))
          .as(sibling)
          .contains("rangeMode = preset.mode();");
    }
  }

  /** 규칙의 근거: 모르는 값은 ytd 로 떨어지고, resolve 가 그 사실을 알려 준다. */
  @Test
  void 모르는_값은_ytd_로_떨어진다() {
    ZoneId zone = ZoneId.of("Asia/Seoul");

    assertThat(StockRangePresetUtil.resolve("oops", zone).mode()).isEqualTo("ytd");
    assertThat(StockRangePresetUtil.resolve("9999", zone).mode()).isEqualTo("ytd");
    assertThat(StockRangePresetUtil.resolve("-3", zone).mode()).isEqualTo("ytd");
    assertThat(StockRangePresetUtil.resolve("", zone).mode()).isEqualTo("ytd");
  }

  /** 아는 값은 그대로 살아 있어야 한다 - 전부 ytd 로 만들면 버튼이 엉뚱한 곳에 눌린다. */
  @Test
  void 아는_값은_그대로_돌아온다() {
    ZoneId zone = ZoneId.of("Asia/Seoul");

    assertThat(StockRangePresetUtil.resolve("mtd", zone).mode()).isEqualTo("mtd");
    assertThat(StockRangePresetUtil.resolve("3", zone).mode()).isEqualTo("3");
    assertThat(StockRangePresetUtil.resolve("1200", zone).mode()).isEqualTo("1200");
  }
}
