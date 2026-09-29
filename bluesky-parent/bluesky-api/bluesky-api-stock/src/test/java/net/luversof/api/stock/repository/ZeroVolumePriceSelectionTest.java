package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 종가를 고르는 모든 조회가 '거래가 있던 날'을 쓰는지 본다.
 *
 * <p>거래량 0 행은 그 날 거래가 없었다는 뜻이고, 그때 KIS 는 종가 자리에 직전 종가를 넣는다. 그 행을 그 날의 종가로 쓰면 화면의 평가 기준 일자가 실제보다
 * 앞당겨진다(실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일).
 *
 * <p>값은 달라지지 않는다 - 거래량 0 행 1,352 개 중 종가가 직전과 다른 것은 1 개뿐이었고, 실제로 바꾼 뒤 평가액/시계열/월배당 응답이 바이트 단위로 같았다
 * (달라진 것은 currentPriceDate 21 행과 보유 스냅샷 priceDate 9 건뿐).
 *
 * <p>한 곳만 고치면 화면마다 다른 날짜가 나온다. 실제로 처음에는 손익 조회만 08-19 로 바뀌고 보유 스냅샷은 08-20 그대로였다 - 스냅샷은 다른 클래스의 조회를
 * 쓰고 있었기 때문이다. 그래서 두 파일을 함께 검사한다.
 */
class ZeroVolumePriceSelectionTest {

  private static final Path REPOSITORY =
      Path.of("src/main/java/net/luversof/api/stock/repository/StockPriceHistoryRepository.java");

  private static final Path QUERY =
      Path.of("src/main/java/net/luversof/api/stock/repository/StockDailyClosePriceQuery.java");

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/StockPriceService.java");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private int count(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }

  /**
   * '가장 최근 종가'를 고르는 LATERAL 조회는 거래가 있던 행을 먼저 보고, 없으면 그냥 최근 행으로 물러선다.
   *
   * <p>단순히 걸러내면 모든 행이 거래량 0 인 종목의 값이 통째로 사라진다. 그래서 두 갈래(거래량 &gt; 0 인 최근 행 / 그냥 최근 행)를 두고 앞 갈래를 먼저
   * 쓴다.
   *
   * <p>예전에는 같은 뜻을 '거래량이 있는가' 식을 첫 정렬 키로 둔 한 줄 정렬로 썼다. 식이 앞에 있으면 (종목, 일자) 인덱스 순서를 못 타 쌍마다 그 종목 이력
   * 전체를 정렬한다 &mdash; holdingsSnapshotBatch 요청 표본의 51% 였다(2026-09-23). 그래서 그 모양이 돌아오지 않는지도 본다.
   */
  @Test
  void 최근_종가_조회는_거래가_있던_행을_우선한다() throws IOException {
    String preferTraded =
        squash(
            """
            (SELECT h."tradeDate", h."closePrice", 0 AS pick
             FROM "StockPriceHistory" h
             WHERE h."stockItem_id" =""");
    String fallback =
        squash(
            """
            UNION ALL
            (SELECT h."tradeDate", h."closePrice", 1 AS pick
             FROM "StockPriceHistory" h
             WHERE h."stockItem_id" =""");
    String pickFirst = squash(") AS y ORDER BY y.pick LIMIT 1");
    for (Path path : List.of(REPOSITORY, QUERY)) {
      String source = squash(read(path));
      assertThat(count(source, preferTraded)).as(path + " : 거래가 있던 행 갈래").isEqualTo(1);
      assertThat(count(source, fallback)).as(path + " : 폴백 갈래").isEqualTo(1);
      assertThat(count(source, pickFirst)).as(path + " : 앞 갈래를 먼저 고르기").isEqualTo(1);

      // 앞 갈래는 거래량으로 거르고 뒤 갈래는 거르지 않는다 - 둘이 바뀌면 폴백이 사라지거나 거래 없는 날을 고른다.
      int start = source.indexOf(preferTraded);
      int union = source.indexOf(fallback, start);
      int end = source.indexOf(pickFirst, union);
      String first = source.substring(start, union);
      String second = source.substring(union, end);
      assertThat(first).as(path + " : 앞 갈래는 거래량 > 0 만").contains("ANDh.\"volume\">0");
      assertThat(first)
          .as(path + " : 앞 갈래는 최근 순 한 건")
          .endsWith("ORDERBYh.\"tradeDate\"DESCLIMIT1)");
      assertThat(second).as(path + " : 폴백 갈래는 거래량을 거르지 않는다").doesNotContain("volume");
      assertThat(second)
          .as(path + " : 폴백 갈래도 최근 순 한 건")
          .endsWith("ORDERBYh.\"tradeDate\"DESCLIMIT1)");

      if (path.equals(QUERY)) {
        // (종목, 기준일) 쌍 조회는 두 갈래 모두 기준일 이하만 본다 - 하나라도 빠지면 그 날엔 없던 미래 종가를 고른다.
        String onOrBeforeDay = "ANDh.\"tradeDate\"<=p.day";
        assertThat(count(first, onOrBeforeDay)).as("앞 갈래가 기준일을 본다").isEqualTo(1);
        assertThat(count(second, onOrBeforeDay)).as("폴백 갈래가 기준일을 본다").isEqualTo(1);
      }

      assertThat(source)
          .as(path + " : 인덱스를 못 타는 식 정렬이 돌아왔다")
          .doesNotContain("ORDERBY(h.\"volume\">0)");
    }
  }

  /** 공백을 전부 지운다 - SQL 줄바꿈 · 들여쓰기 · 서식기와 무관하게 비교한다. */
  private static String squash(String text) {
    return text.replaceAll("\\s+", "");
  }

  /** 구간 조회 결과는 '그 날의 종가'로 쓰이므로 거래가 없던 행을 아예 빼야 한다. */
  @Test
  void 구간_조회는_거래량_0_행을_제외한다() throws IOException {
    assertThat(count(read(QUERY), "AND h.\"volume\" > 0"))
        // 구간 조회 두 개(일반/ordinality) + 최근 종가 쌍 조회의 '거래가 있던 행' 갈래 하나
        // + 카탈로그 종목별 일별 종가(저장소에서 옮겨 옴) 하나(2026-09-23).
        // 정확한 개수로 본다 - 구간 조회 하나가 조건을 잃으면 2 가 되어 걸린다.
        .as("구간 조회 두 개(일반/ordinality) · 최근 종가 쌍 조회의 앞 갈래 · 카탈로그 종목별 조회가 모두 걸러야 한다")
        .isEqualTo(4);
    // 레포지토리에 있던 구간 조회는 부르는 곳이 없어 2026-08-24 에 지웠다. 남은 구간 조회는 위 두 개뿐이다.
    // "종가를 돌려주는 모든 SQL 이 거래량을 본다" 는 더 넓은 검사는 ClosePriceQueryVolumeGuardTest 가 한다.
    assertThat(read(REPOSITORY))
        .as("레포지토리에 거르지 않는 구간 조회가 되살아났다")
        .doesNotContain("findByStockItemIdInAndTradeDateBetween");
  }

  /** 종목의 모든 행이 거래량 0 이어도 값이 사라지면 안 된다. */
  @Test
  void 단건_조회는_폴백을_남긴다() throws IOException {
    String service = read(SERVICE);
    // 파일 어딘가에 .or( 가 있는지만 보면 다른 메서드의 폴백에 속는다(실제로 그렇게 속아 주입을 놓쳤다).
    // 거래량으로 거른 호출 '바로 뒤'에 폴백이 붙어 있는지를 위치로 확인한다.
    for (String filtered :
        List.of(
            "findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc",
            "findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc")) {
      int at = service.indexOf(filtered);
      assertThat(at).as(filtered + " 호출이 없다").isGreaterThan(0);
      String following = service.substring(at, Math.min(service.length(), at + 400));
      assertThat(following).as(filtered + " 뒤에 폴백이 없다. 모든 행이 거래량 0 인 종목의 값이 사라진다").contains(".or(");
    }
  }
}
