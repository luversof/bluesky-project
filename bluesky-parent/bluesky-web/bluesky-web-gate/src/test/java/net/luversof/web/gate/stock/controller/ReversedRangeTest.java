package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 역순 기간(시작 &gt; 종료)은 서버가 앞뒤를 바로잡는다.
 *
 * <p>실측 2026-09-09: 역순으로 부른 자산성장·배당·매매·활동 조각 4개가 전부 오류도 안내도 없이 "2026-07-01 ~ 2026-02-28" 과 빈 결과를
 * 그렸다. 화면의 피커는 이미 역순을 바로잡지만(date-range-picker.ts), 손으로 고친 주소나 어긋난 공유 링크는 서버에 그대로 온다.
 */
class ReversedRangeTest {

  private static final Instant EARLY = Instant.parse("2026-02-28T15:00:00Z");
  private static final Instant LATE = Instant.parse("2026-06-30T15:00:00Z");

  @Test
  void 역순이면_맞바꾼다() {
    Instant[] ordered = StockBaseHtmxController.orderedRange(LATE, EARLY);
    assertThat(ordered).containsExactly(EARLY, LATE);
  }

  @Test
  void 정순이거나_같으면_그대로다() {
    assertThat(StockBaseHtmxController.orderedRange(EARLY, LATE)).containsExactly(EARLY, LATE);
    assertThat(StockBaseHtmxController.orderedRange(EARLY, EARLY)).containsExactly(EARLY, EARLY);
  }

  /** 한쪽만 있으면 기본 기간 판정이 뒤에서 하므로 손대지 않는다. */
  @Test
  void 한쪽이_없으면_그대로다() {
    assertThat(StockBaseHtmxController.orderedRange(null, LATE)).containsExactly(null, LATE);
    assertThat(StockBaseHtmxController.orderedRange(EARLY, null)).containsExactly(EARLY, null);
    assertThat(StockBaseHtmxController.orderedRange(null, null)).containsExactly(null, null);
  }

  private static final Path CONTROLLERS =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static String handlerBody(String source, String path) {
    // 줄바꿈은 \R - 다른 세션의 포매터가 CRLF 로 저장한다.
    Matcher m =
        Pattern.compile(
                "@GetMapping\\(\""
                    + Pattern.quote(path)
                    + "\"\\)\\R  public String \\w+\\((.*?)\\R  \\}\\R",
                Pattern.DOTALL)
            .matcher(source);
    assertThat(m.find()).as("핸들러를 찾지 못했다: " + path).isTrue();
    return m.group(1);
  }

  /** 기간을 받는 목록 조각 4개가 모두 같은 규칙을 거친다. 하나만 빠져도 그 화면만 예전처럼 빈 결과를 그린다. */
  @Test
  void 기간을_받는_목록_조각_4개가_모두_바로잡는다() throws IOException {
    String asset =
        Files.readString(
            CONTROLLERS.resolve("StockAssetGrowthHtmxController.java"), StandardCharsets.UTF_8);
    String dividend =
        Files.readString(
            CONTROLLERS.resolve("StockDividendHtmxController.java"), StandardCharsets.UTF_8);
    String trade =
        Files.readString(
            CONTROLLERS.resolve("StockTradeHtmxController.java"), StandardCharsets.UTF_8);

    assertThat(handlerBody(asset, "/asset-growth/view")).contains("effectiveRange(rangeMode,");
    assertThat(handlerBody(dividend, "/dividend/list")).contains("effectiveRange(rangeMode,");
    assertThat(handlerBody(trade, "/trade/list")).contains("effectiveRange(rangeMode,");
    assertThat(handlerBody(trade, "/activity-list")).contains("effectiveRange(rangeMode,");
  }

  /**
   * '전체' 는 모드가 이긴다 - 함께 실린 날짜는 버린다(실측 2026-09-09: 공유 주소 ?rangeMode=all 이 저장된 올해 날짜와 함께 와 내용이 올해였다).
   */
  @Test
  void 전체_모드는_함께_온_날짜를_버린다() {
    assertThat(StockBaseHtmxController.effectiveRange("all", EARLY, LATE))
        .containsExactly(null, null);
    assertThat(StockBaseHtmxController.effectiveRange("ALL", LATE, EARLY))
        .containsExactly(null, null);
  }

  @Test
  void 다른_모드는_날짜를_바로잡아_쓴다() {
    assertThat(StockBaseHtmxController.effectiveRange("ytd", LATE, EARLY))
        .containsExactly(EARLY, LATE);
    assertThat(StockBaseHtmxController.effectiveRange(null, EARLY, LATE))
        .containsExactly(EARLY, LATE);
    assertThat(StockBaseHtmxController.effectiveRange("", null, LATE)).containsExactly(null, LATE);
  }
}
