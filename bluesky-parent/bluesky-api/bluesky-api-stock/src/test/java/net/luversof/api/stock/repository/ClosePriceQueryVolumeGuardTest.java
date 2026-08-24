package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 종가를 고르는 SQL 은 모두 거래량 0 인 행을 피하는지 본다.
 *
 * <p>거래량 0 인 날은 그 날 거래가 없었다는 뜻인데 종가 자리에는 직전 종가가 그대로 들어 있다(실측: 거래량 0 행 1,352 개 중 종가가 직전과 다른 것은 1
 * 개뿐). 그 행을 그 날의 종가로 쓰면 값은 같아도 <b>'마지막 시세일'이 실제보다 앞당겨져</b> 화면이 "오늘 종가" 라고 말하게 된다.
 *
 * <p>그래서 종가 선택 SQL 은 {@code AND "volume" > 0} 으로 걸러 내거나, 마지막 한 건을 고르는 경우 {@code ORDER BY ("volume"
 * > 0) DESC} 로 거래가 있던 행을 먼저 집는다(전부 거래량 0 이면 그래도 값을 준다).
 *
 * <p>이 검사를 두는 이유: 조건이 빠진 조회가 하나만 있어도 그 경로를 타는 화면만 조용히 다른 날짜를 말한다. 실제로 거래량 필터가 없는 조회가 하나 남아 있었는데, 마침
 * <b>아무도 부르지 않는 죽은 메서드</b>였다 &mdash; 누군가 그걸 재사용했으면 그대로 되살아났을 함정이다(2026-08-24 제거).
 */
class ClosePriceQueryVolumeGuardTest {

  /**
   * 종가 SQL 이 있을 수 있는 곳 전체.
   *
   * <p>예전에는 파일 두 개를 손으로 적어 두었다. 오늘은 그 둘이 전부지만(실측 2026-08-23: closePrice 를 고르는 텍스트블록 SQL 은
   * StockPriceHistoryRepository 4 개 · StockDailyClosePriceQuery 3 개) 세 번째 파일이 생기면 검사에서 조용히 빠진다.
   *
   * <p>같은 약점으로 실제 결함을 놓친 적이 있다 &mdash; 프론트엔드 로케일 검사가 파일 셋을 손으로 적어 두는 바람에, 규칙을 정의한 파일 자신의 {@code
   * toLocaleString("ko-KR")} 을 놓쳤다. 그래서 목록 대신 훑는다.
   */
  private static final Path SOURCE_ROOT = Path.of("src/main/java/net/luversof/api/stock");

  private List<Path> sources() throws IOException {
    try (java.util.stream.Stream<Path> files = Files.walk(SOURCE_ROOT)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  /** 텍스트 블록 안의 SELECT 문 하나. */
  private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);

  private List<String> closePriceQueries() throws IOException {
    List<String> queries = new ArrayList<>();
    for (Path source : sources()) {
      Matcher matcher = TEXT_BLOCK.matcher(Files.readString(source, StandardCharsets.UTF_8));
      while (matcher.find()) {
        String sql = matcher.group(1);
        // '종목 목록을 받아 종가를 돌려주는' 조회만 본다. 거래량 0 행을 세거나 비교하는 진단용 SQL 은
        // 오히려 그 행을 봐야 하므로 대상이 아니다(전자는 :ids 를 받고 후자는 받지 않는다 - 실측으로
        // 이 두 성질이 4 개의 가격 조회와 3 개의 진단 SQL 을 정확히 가른다).
        String selectList = sql.contains("FROM") ? sql.substring(0, sql.indexOf("FROM")) : sql;
        if (selectList.contains("closePrice") && sql.contains(":ids")) {
          queries.add(sql);
        }
      }
    }
    return queries;
  }

  @Test
  void 종가를_고르는_SQL_은_거래량_0_행을_피한다() throws IOException {
    List<String> queries = closePriceQueries();
    // 파서가 조용히 0 건을 반환하면 이 검사는 아무것도 보지 않는다(현재 4 개).
    //
    // 예전에는 5 개였다. StockPriceHistoryRepository 에 (종목, 기준일) 쌍 조회가 한 벌 더 있었는데
    // StockDailyClosePriceQuery 의 같은 이름 메서드와 SQL 까지 같은 <b>중복</b>이었고 부르는 곳이 없었다
    // (실사용은 StockPriceService:146 -> StockDailyClosePriceQuery 쪽). 같은 규칙의 사본이 둘이면 한쪽만
    // 조용히 갈리므로 지웠다(2026-08-23).
    assertThat(queries).as("종가 SQL 을 하나도 찾지 못했다 - 검사가 무력하다").hasSizeGreaterThanOrEqualTo(4);

    List<String> unguarded =
        queries.stream()
            .filter(sql -> !sql.contains("\"volume\" > 0"))
            .map(sql -> sql.strip().lines().findFirst().orElse("") + " ...")
            .toList();

    assertThat(unguarded).as("거래량 조건이 없는 종가 조회다. 거래량 0 행을 그 날의 종가로 쓰면 마지막 시세일이 앞당겨진다").isEmpty();
  }

  /** 거래량 필터가 없던 죽은 조회들이 실제로 사라졌는지. */
  @Test
  void 거래량_필터가_없던_죽은_조회는_남아있지_않다() throws IOException {
    String repository =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/api/stock/repository/StockPriceHistoryRepository.java"),
            StandardCharsets.UTF_8);
    assertThat(repository)
        .as("거래량 조건 없이 기간 내 전 행을 돌려주던 조회다. 부르는 곳이 없어 지웠다")
        .doesNotContain("findByStockItemIdInAndTradeDateBetween");
  }
}
