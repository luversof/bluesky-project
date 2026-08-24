package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 손으로 쓴 SQL 의 컬럼 순서와 RowMapper 의 위치 인덱스가 맞는지 본다.
 *
 * <p>{@code TradeQuery} 는 파생 쿼리 대신 직접 쓴 SQL 을 {@code rs.getXxx(1..11)} 로 읽는다(엔티티 머티리얼라이즈 비용 때문이다).
 * 그래서 SELECT 목록을 한 칸만 옮겨도 <b>컴파일도 되고 예외도 나지 않은 채</b> 값이 뒤바뀐다.
 *
 * <p>실측 2026-08-23: {@code fee} 와 {@code tax} 를 서로 바꿔 실제로 배포해 봤더니 거래 250 건 중 201 건의 두 값이 뒤바뀌었다. 검사
 * 스크립트가 48 개 중 3 개(보유 원가 WMA 합 / 종목별 시계열 합 / 원장 재계산)로 잡아 내긴 했다.
 *
 * <p>다만 그건 <b>거래를 읽는 경로가 둘이라</b> 서로 어긋난 덕분이다 &mdash; {@code TradeService.findByUserId} 는 이 클래스를,
 * {@code findByAccountId} 는 Spring Data 파생 쿼리를 쓴다. 나중에 나머지 경로까지 이 클래스로 통일하면 그 교차 검증이 사라지고 같은 실수가
 * 조용히 지나간다. 그래서 배포 후 검사에 기대지 않고 여기서 잡는다.
 */
class TradeQueryColumnOrderTest {

  private static final Path SOURCE =
      Path.of("src/main/java/net/luversof/api/stock/repository/TradeQuery.java");

  /** SELECT 목록 순서. 이 순서가 곧 {@code rs.getXxx(n)} 의 n 이다. */
  private static final List<String> EXPECTED_COLUMNS =
      List.of(
          "id",
          "account_id",
          "stockItem_id",
          "type",
          "quantity",
          "price",
          "fee",
          "tax",
          "tradeDate",
          "realizedProfit",
          "exchangeRate");

  /** 각 위치에서 채워야 하는 필드. */
  private static final List<String> EXPECTED_SETTERS =
      List.of(
          "setId",
          "setAccountId",
          "setStockItemId",
          "setType",
          "setQuantity",
          "setPrice",
          "setFee",
          "setTax",
          "setTradeDate",
          "setRealizedProfit",
          "setExchangeRate");

  /**
   * {@code open} 다음부터 {@code close} 전까지를 차례로 뽑는다.
   *
   * <p>정규식을 쓰지 않는 이유: 이 검사가 읽는 대상이 따옴표와 역슬래시투성이라, 정규식으로 쓰면 이스케이프를 틀리기 쉽다(실제로 이 파일을 만들며 두 번 틀렸다).
   */
  private List<String> tokensBetween(String text, String open, String close) {
    List<String> found = new ArrayList<>();
    int at = 0;
    while (true) {
      int start = text.indexOf(open, at);
      if (start < 0) {
        return found;
      }
      start += open.length();
      int end = text.indexOf(close, start);
      if (end < 0) {
        return found;
      }
      found.add(text.substring(start, end));
      at = end;
    }
  }

  private String source() throws IOException {
    assertThat(SOURCE).exists();
    return Files.readString(SOURCE, StandardCharsets.UTF_8);
  }

  @Test
  void SELECT_컬럼_순서가_그대로다() throws IOException {
    String source = source();
    int from = source.indexOf("SELECT t.");
    int to = source.indexOf("FROM \"Trade\"", from);
    assertThat(from).as("SELECT 블록을 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    assertThat(to).isGreaterThan(from);

    List<String> columns = tokensBetween(source.substring(from, to), "t.\"", "\"");

    assertThat(columns)
        .as("SELECT 목록의 순서가 바뀌었다. rs.getXxx(n) 은 위치로 읽으므로 값이 조용히 뒤바뀐다")
        .containsExactlyElementsOf(EXPECTED_COLUMNS);
  }

  @Test
  void 매퍼가_1부터_차례로_읽고_같은_순서로_채운다() throws IOException {
    String source = source();
    int from = source.indexOf("private static Trade mapRow");
    assertThat(from).as("mapRow 를 찾지 못했다").isGreaterThan(0);
    String body = source.substring(from, source.indexOf("return trade;", from));

    List<Integer> indexes = new ArrayList<>();
    for (String raw : tokensBetween(body, "rs.get", ")")) {
      int open = raw.indexOf('(');
      assertThat(open).as("rs.get 뒤에 여는 괄호가 없다: %s", raw).isGreaterThanOrEqualTo(0);
      String first = raw.substring(open + 1).split(",")[0].trim();
      indexes.add(Integer.parseInt(first));
    }

    List<Integer> expectedIndexes = new ArrayList<>();
    for (int i = 1; i <= EXPECTED_COLUMNS.size(); i++) {
      expectedIndexes.add(i);
    }
    assertThat(indexes)
        .as("ResultSet 을 1..%d 순서대로 읽지 않는다", EXPECTED_COLUMNS.size())
        .containsExactlyElementsOf(expectedIndexes);

    List<String> setters = new ArrayList<>();
    for (String raw : tokensBetween(body, "trade.", "(")) {
      if (raw.startsWith("set")) {
        setters.add(raw);
      }
    }
    assertThat(setters)
        .as("읽는 순서와 채우는 순서가 달라졌다 - 컬럼과 필드가 어긋난다")
        .containsExactlyElementsOf(EXPECTED_SETTERS);
  }
}
