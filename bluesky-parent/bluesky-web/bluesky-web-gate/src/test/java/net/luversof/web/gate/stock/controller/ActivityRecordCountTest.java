package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity;

/**
 * 활동 화면의 "건" 은 원장의 건수와 같아야 한다.
 *
 * <p>타임라인은 (날짜 · 유형 · 종목 · 매매구분) 이 같은 활동을 <b>계좌를 가로질러</b> 한 줄로 합친다. 합친 줄 수를 세면 다른 화면과 어긋난다 &mdash;
 * 실측 2026-09-10: 활동 화면이 매수 149 · 매도 53 · 배당 108 건이라 했는데 원장은 매수 203 · 매도 55 · 배당 202 건이었다. 같은 시점에 매매
 * 화면은 258 건, 배당 화면은 202 건을 보여준다. <b>금액은 정확했다</b>(매도 1,393,667,090 · 배당 65,652,134 모두 일치) — 틀린 것은
 * 건수뿐이라 눈에 띄지 않았다.
 */
class ActivityRecordCountTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private Activity trade(String instant, UUID stockItemId, String tradeType, String amount) {
    return new Activity(
        "TRADE",
        stockItemId,
        "종목",
        tradeType,
        1,
        null,
        new BigDecimal(amount),
        Instant.parse(instant),
        List.of(UUID.randomUUID()),
        null);
  }

  @Test
  void 낱개_활동의_원본_건수는_1이다() {
    assertThat(trade("2026-09-10T01:00:00Z", UUID.randomUUID(), "BUY", "100").recordCount())
        .isEqualTo(1);
  }

  @Test
  void 묶인_줄은_원본_건수를_합한다() {
    UUID stockItemId = UUID.randomUUID();
    List<Activity> raw =
        List.of(
            trade("2026-09-10T01:00:00Z", stockItemId, "BUY", "100"),
            trade("2026-09-10T02:00:00Z", stockItemId, "BUY", "200"),
            trade("2026-09-10T03:00:00Z", stockItemId, "BUY", "300"));

    List<Activity> grouped = StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL);

    assertThat(grouped).hasSize(1);
    assertThat(grouped.get(0).recordCount()).as("세 건이 한 줄로 묶였으면 원본은 3 건이다").isEqualTo(3);
    assertThat(grouped.get(0).amount()).isEqualByComparingTo("600");
  }

  @Test
  void 다른_종목은_따로_센다() {
    List<Activity> raw =
        List.of(
            trade("2026-09-10T01:00:00Z", UUID.randomUUID(), "BUY", "100"),
            trade("2026-09-10T02:00:00Z", UUID.randomUUID(), "BUY", "200"));

    List<Activity> grouped = StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL);

    assertThat(grouped).hasSize(2);
    assertThat(grouped.stream().mapToLong(Activity::recordCount).sum()).isEqualTo(2);
  }

  @Test
  void 묶기는_원본_건수를_잃지_않는다() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    List<Activity> raw =
        List.of(
            trade("2026-09-10T01:00:00Z", a, "BUY", "100"),
            trade("2026-09-10T02:00:00Z", a, "BUY", "200"),
            trade("2026-09-10T02:00:00Z", a, "SELL", "300"),
            trade("2026-09-11T02:00:00Z", b, "BUY", "400"));

    List<Activity> grouped = StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL);

    assertThat(grouped.stream().mapToLong(Activity::recordCount).sum())
        .as("묶고 나서 센 합이 원본 건수와 달라지면 화면 숫자가 원장과 어긋난다")
        .isEqualTo(raw.size());
  }

  @Test
  void 화면_건수는_묶인_줄이_아니라_원본을_센다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockTradeHtmxController.java"),
            StandardCharsets.UTF_8);

    int recordCountSums = 0;
    int from = 0;
    while (true) {
      int at = source.indexOf("mapToLong(Activity::recordCount)", from);
      if (at < 0) {
        break;
      }
      recordCountSums++;
      from = at + 1;
    }

    assertThat(recordCountSums).as("매수·매도·배당 건수를 두 화면(이번 달 요약 · 활동 목록)에서 세므로 여섯 군데다").isEqualTo(6);
  }

  @Test
  void 전체_활동_건수는_매매와_배당의_합이다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/activityList.jte"), StandardCharsets.UTF_8);

    assertThat(template)
        .as("묶인 줄 수(activities.size())를 쓰면 바로 아래 매매·배당 건수와도 어긋난다 (실측: 310 vs 258+202)")
        .contains("countMessage.apply(buyCount + sellCount + dividendCount)")
        .doesNotContain("countMessage.apply(activities != null ? activities.size() : 0)");
  }
}
