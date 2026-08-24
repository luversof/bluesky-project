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
 * 활동 목록의 묶기 규칙을 고정한다.
 *
 * <p>이 화면은 매매와 배당을 한 줄기로 합쳐 (날짜 · 유형 · 종목 · 매매구분) 으로 묶는다. 예전에는 같은 코드가 컨트롤러 안에 세 벌 복사돼 있었고(그중 한 벌은
 * 아무도 부르지 않는 오버로드 안이었다) 세 벌 모두 아래 세 가지를 틀리게 하고 있었다.
 */
class ActivityGroupingTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

  private Activity trade(String instant, UUID stockItemId, String name, int qty, String amount) {
    return new Activity(
        "TRADE",
        stockItemId,
        name,
        "BUY",
        qty,
        null,
        new BigDecimal(amount),
        Instant.parse(instant),
        List.of(UUID.randomUUID()));
  }

  /**
   * 날짜 칸은 요청 타임존으로 나눈다.
   *
   * <p>저장된 매매/배당 시각은 전부 UTC 자정이다(실측 443건). 서버 존으로 나누면 서버보다 서쪽 브라우저에서 달력(요청 존)과 표(서버 존)가 하루씩 어긋난다.
   *
   * <p>테스트 JVM 의 기본 존이 Asia/Seoul 이므로 서울 기준 단언만으로는 넘겨받은 존을 쓰는지 알 수 없다. 그래서 <b>뉴욕에서는 같은 날이지만 서울에서는
   * 다른 날</b>인 두 시각을 쓴다 &mdash; 넘겨받은 존을 무시하면 뉴욕으로 물어도 두 행으로 갈린다.
   */
  @Test
  void 날짜_묶음은_넘겨받은_타임존을_따른다() {
    UUID id = UUID.randomUUID();
    List<Activity> raw =
        List.of(
            // 뉴욕 8/19 01:00 · 서울 8/19 14:00
            trade("2026-08-19T05:00:00Z", id, "삼성전자", 1, "1000"),
            // 뉴욕 8/19 12:00 · 서울 8/20 01:00
            trade("2026-08-19T16:00:00Z", id, "삼성전자", 2, "2000"));

    assertThat(StockTradeHtmxController.groupActivitiesByDay(raw, NEW_YORK)).hasSize(1);
    assertThat(StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL)).hasSize(2);
  }

  /** 존이 다르면 '같은 날' 판정도 달라져 묶이는 결과 자체가 달라진다. */
  @Test
  void 존이_다르면_같은_날_판정이_달라진다() {
    UUID id = UUID.randomUUID();
    // 서울에서는 둘 다 8/19, 뉴욕에서는 8/18 과 8/19 로 갈린다.
    List<Activity> raw =
        List.of(
            trade("2026-08-19T00:00:00Z", id, "삼성전자", 1, "1000"),
            trade("2026-08-19T14:00:00Z", id, "삼성전자", 2, "2000"));

    assertThat(StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL)).hasSize(1);
    assertThat(StockTradeHtmxController.groupActivitiesByDay(raw, NEW_YORK)).hasSize(2);
  }

  /**
   * 종목은 이름이 아니라 id 로 묶는다.
   *
   * <p>{@link Activity} 가 id 를 들고 있는 이유와 같은 사정이다 &mdash; 이름으로 되찾으면 동명 종목에서 엉뚱하게 이어진다. 현재 데이터에 동명
   * 종목은 없지만(86종 전부 고유), 하나 생기는 순간 두 종목의 수량과 금액이 한 행으로 합산돼 버린다.
   */
  @Test
  void 이름이_같아도_다른_종목이면_묶이지_않는다() {
    List<Activity> raw =
        List.of(
            trade("2026-08-19T00:00:00Z", UUID.randomUUID(), "같은이름", 1, "1000"),
            trade("2026-08-19T00:00:00Z", UUID.randomUUID(), "같은이름", 2, "2000"));

    List<Activity> grouped = StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL);

    assertThat(grouped).hasSize(2);
    assertThat(grouped).extracting(Activity::quantity).containsExactlyInAnyOrder(1, 2);
  }

  /** id 가 같으면(같은 종목·같은 날·같은 구분) 수량·금액이 합쳐지고 계좌가 모인다. */
  @Test
  void 같은_종목_같은_날은_수량과_금액이_합쳐진다() {
    UUID id = UUID.randomUUID();
    List<Activity> grouped =
        StockTradeHtmxController.groupActivitiesByDay(
            List.of(
                trade("2026-08-19T00:00:00Z", id, "삼성전자", 1, "1000"),
                trade("2026-08-19T00:00:00Z", id, "삼성전자", 2, "2000")),
            SEOUL);

    assertThat(grouped).hasSize(1);
    assertThat(grouped.get(0).quantity()).isEqualTo(3);
    assertThat(grouped.get(0).amount()).isEqualByComparingTo("3000");
    assertThat(grouped.get(0).accountIds()).hasSize(2);
  }

  /**
   * 같은 날 안의 행 순서가 정해져 있다.
   *
   * <p>예전에는 날짜 하나로만 정렬해 같은 날의 순서가 {@code HashMap} 순회 순서였다(실측: 299행 중 171행이 같은 날에 다른 행과 함께 있다).
   *
   * <p>행이 서너 개뿐이면 해시 순서가 우연히 정렬 순서와 같을 수 있어(처음 쓴 이 테스트가 그래서 옛 코드를 통과했다) 같은 날에 8행을 둔다.
   */
  @Test
  void 같은_날_안에서도_순서가_정해져_있다() {
    List<String> names = List.of("아", "사", "바", "마", "라", "다", "나", "가");
    List<Activity> raw = new java.util.ArrayList<>();
    for (String name : names) {
      raw.add(trade("2026-08-19T00:00:00Z", UUID.randomUUID(), name, 1, "1"));
    }
    raw.add(trade("2026-08-20T00:00:00Z", UUID.randomUUID(), "하", 1, "1"));

    assertThat(StockTradeHtmxController.groupActivitiesByDay(raw, SEOUL))
        .extracting(Activity::stockItemName)
        .containsExactly("하", "가", "나", "다", "라", "마", "바", "사", "아");
  }

  /**
   * 묶기 구현이 한 벌만 남아 있는지 본다.
   *
   * <p>세 벌이 복사돼 있던 시절 셋은 우연히 같았지만, 이 저장소에서 같은 공식이 두 곳에 있으면 한쪽만 고쳐져 갈라진 사례가 반복됐다(매도원가 2곳, 보유원가
   * 대체경로).
   */
  @Test
  void 묶기_구현은_한_벌만_있다() throws IOException {
    Path controllerDir = Path.of("src/main/java/net/luversof/web/gate/stock/controller");
    int copies = 0;
    for (Path file :
        Files.list(controllerDir).filter(p -> p.toString().endsWith(".java")).toList()) {
      String source = Files.readString(file, StandardCharsets.UTF_8);
      copies += source.split("Map<String, Activity> groupedMap", -1).length - 1;
    }
    assertThat(copies).as("활동 묶기 구현은 groupActivitiesByDay 한 곳에만 있어야 한다").isEqualTo(1);
  }
}
