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
   * '가장 최근 종가'를 고르는 LATERAL 조회는 거래가 있던 행을 먼저 본다.
   *
   * <p>단순히 걸러내면 모든 행이 거래량 0 인 종목의 값이 통째로 사라진다. 정렬 우선순위로 두면 그런 종목도 기존과 같은 값을 유지한다.
   */
  @Test
  void 최근_종가_조회는_거래가_있던_행을_우선한다() throws IOException {
    for (Path path : List.of(REPOSITORY, QUERY)) {
      assertThat(count(read(path), "ORDER BY (h.\"volume\" > 0) DESC"))
          .as(path + " 의 최근 종가 조회가 거래량을 보지 않는다")
          .isGreaterThan(0);
    }
  }

  /** 구간 조회 결과는 '그 날의 종가'로 쓰이므로 거래가 없던 행을 아예 빼야 한다. */
  @Test
  void 구간_조회는_거래량_0_행을_제외한다() throws IOException {
    assertThat(count(read(QUERY), "AND h.\"volume\" > 0"))
        .as("구간 조회 두 개(일반/ordinality)가 모두 걸러야 한다")
        .isEqualTo(2);
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
