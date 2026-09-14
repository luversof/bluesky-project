package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;

/**
 * 최초 데이터 일자 조회의 종목·계좌 필터.
 *
 * <p>게이트 상세 화면의 '가장 이른 기간으로'(«) 가 이 값을 목표로 삼는다. 사용자 전체의 최초일을 쓰면 그 종목이 아직 없던 창으로 뛴다 - 실측
 * 2026-09-13(운영): 사용자 전체 2009-10-06 · 삼성전자 2020-03-04 · 연금저축1 계좌 2025-03-28.
 *
 * <p>널 파라미터는 <b>PostgreSQL 이 타입을 못 정한다</b>. {@code CAST(:x AS uuid) IS NULL} 로 써야 하고, {@code :x IS
 * NULL} 로 줄이면 컴파일은 되지만 <b>런타임에 터진다</b> - 단위 테스트로는 안 잡히므로 질의문 자체를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class DataFirstDateFilterTest {

  @Mock private TradeRepository tradeRepository;

  @Mock private DividendRepository dividendRepository;

  @InjectMocks private DataFirstDateController dataFirstDateController;

  private static final UUID USER = UUID.randomUUID();
  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID ACCOUNT = UUID.randomUUID();

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 받은_필터를_그대로_저장소에_넘긴다() {
    when(tradeRepository.findFirstTradeDate(USER, ITEM, ACCOUNT))
        .thenReturn(Instant.parse("2020-03-04T00:00:00Z"));
    when(dividendRepository.findFirstDividendDate(USER, ITEM, ACCOUNT))
        .thenReturn(Instant.parse("2020-05-19T00:00:00Z"));

    var response = dataFirstDateController.findDataFirstDate(USER, ITEM, ACCOUNT);

    assertThat(response.tradeFirstDate()).isEqualTo(Instant.parse("2020-03-04T00:00:00Z"));
    assertThat(response.dividendFirstDate()).isEqualTo(Instant.parse("2020-05-19T00:00:00Z"));
    verify(tradeRepository).findFirstTradeDate(USER, ITEM, ACCOUNT);
    verify(dividendRepository).findFirstDividendDate(USER, ITEM, ACCOUNT);
  }

  /** 필터 없이 부르면 예전과 같은 뜻이다(사용자 전체) - 널을 그대로 넘겨야 질의가 조건을 건너뛴다. */
  @Test
  void 필터가_없으면_널을_그대로_넘긴다() {
    when(tradeRepository.findFirstTradeDate(USER, null, null))
        .thenReturn(Instant.parse("2009-10-06T00:00:00Z"));

    var response = dataFirstDateController.findDataFirstDate(USER, null, null);

    assertThat(response.tradeFirstDate()).isEqualTo(Instant.parse("2009-10-06T00:00:00Z"));
    verify(tradeRepository).findFirstTradeDate(USER, null, null);
    verify(dividendRepository).findFirstDividendDate(USER, null, null);
  }

  /** 해당 조합에 데이터가 없으면 널이다 - 0 이나 오늘로 지어내지 않는다. */
  @Test
  void 없는_조합은_널이다() {
    when(tradeRepository.findFirstTradeDate(USER, ITEM, ACCOUNT)).thenReturn(null);
    when(dividendRepository.findFirstDividendDate(USER, ITEM, ACCOUNT)).thenReturn(null);

    var response = dataFirstDateController.findDataFirstDate(USER, ITEM, ACCOUNT);

    assertThat(response.tradeFirstDate()).isNull();
    assertThat(response.dividendFirstDate()).isNull();
  }

  /** {@code :x IS NULL} 로 줄이면 PostgreSQL 이 파라미터 타입을 못 정해 런타임에 터진다. */
  @Test
  void 널_비교는_캐스팅해서_쓴다() throws IOException {
    String trade = read("src/main/java/net/luversof/api/stock/repository/TradeRepository.java");
    String dividend =
        read("src/main/java/net/luversof/api/stock/repository/DividendRepository.java");

    assertThat(trade).contains("CAST(:stockItemId AS uuid) IS NULL");
    assertThat(trade).contains("CAST(:accountId AS uuid) IS NULL");
    assertThat(dividend).contains("CAST(:stockItemId AS uuid) IS NULL");
    assertThat(dividend).contains("CAST(:accountId AS uuid) IS NULL");
  }

  /** 필터는 <b>더하는</b> 조건이다 - 사용자 조건을 대체하면 남의 데이터가 섞인다. */
  @Test
  void 사용자_조건은_그대로_남는다() throws IOException {
    for (String path :
        new String[] {
          "src/main/java/net/luversof/api/stock/repository/TradeRepository.java",
          "src/main/java/net/luversof/api/stock/repository/DividendRepository.java"
        }) {
      String source = read(path);
      int at = source.indexOf("CAST(:stockItemId AS uuid) IS NULL");
      assertThat(at).as(path).isGreaterThanOrEqualTo(0);
      // 바로 앞 WHERE 만 본다 - 400 자 창으로 보면 같은 파일의 다른 질의가 섞인다.
      int where = source.lastIndexOf("WHERE", at);
      assertThat(where).as(path + " 의 WHERE").isGreaterThanOrEqualTo(0);
      String query = source.substring(where, at);
      assertThat(query).as(path + " 는 사용자로 먼저 좁힌다").contains("a.\"user_id\" = :userId");
    }
  }
}
