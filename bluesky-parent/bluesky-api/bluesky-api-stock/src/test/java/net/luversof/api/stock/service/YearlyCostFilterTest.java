package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;

/**
 * 연도별 세금·비용을 계좌·종목으로 좁히는 규칙.
 *
 * <p>2026-09-12 까지 이 집계에는 좁히는 수단이 아예 없었다. 자산 성장 화면에는 계좌·종목 필터가 있고 같은 화면의 다른 구역(종목별 기여 · 연도별 성과 · 매매
 * 내역)은 전부 그 필터를 따르는데, 연도별 세금·비용 표만 늘 전 계좌 합계를 그렸다 &mdash; 실측: 한 계좌로 좁힌 화면과 안 좁힌 화면의 표가 14 행 · 합계
 * +225,630,135 원까지 <b>바이트 단위로 같았다</b>. 한 화면에서 한 표만 다른 범위를 말하면 어느 쪽이 맞는지 화면으로는 알 수 없다.
 */
class YearlyCostFilterTest {

  private TradeRepository tradeRepository;
  private DividendRepository dividendRepository;
  private YearlyCostService service;

  @BeforeEach
  void setUp() {
    tradeRepository = mock(TradeRepository.class);
    dividendRepository = mock(DividendRepository.class);
    when(tradeRepository.findYearlyCost(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    when(dividendRepository.findYearlyIncome(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    service = new YearlyCostService();
    service.setTradeRepository(tradeRepository);
    service.setDividendRepository(dividendRepository);
  }

  private String[] capture() {
    ArgumentCaptor<String> accounts = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> items = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(tradeRepository)
        .findYearlyCost(any(), any(), any(), any(), accounts.capture(), items.capture());
    return new String[] {accounts.getValue(), items.getValue()};
  }

  @Test
  void 좁히지_않으면_두_목록_모두_널이다() {
    service.findYearlyCost(UUID.randomUUID(), null, null, ZoneId.of("Asia/Seoul"));
    assertThat(capture()).containsExactly(null, null);
  }

  /** 화면은 "선택 없음" 을 빈 목록으로 보낸다 - 빈 목록으로 좁히면 결과가 통째로 사라진다. */
  @Test
  void 빈_목록은_좁히지_않는_것과_같다() {
    service.findYearlyCost(
        UUID.randomUUID(), null, null, ZoneId.of("Asia/Seoul"), List.of(), List.of());
    assertThat(capture()).containsExactly(null, null);
  }

  @Test
  void 목록은_쉼표로_이어_넘긴다() {
    UUID a1 = UUID.fromString("00000000-0000-4000-8000-000000000001");
    UUID a2 = UUID.fromString("00000000-0000-4000-8000-000000000002");
    UUID s1 = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    service.findYearlyCost(
        UUID.randomUUID(), null, null, ZoneId.of("Asia/Seoul"), List.of(a1, a2), List.of(s1));
    assertThat(capture()).containsExactly(a1 + "," + a2, s1.toString());
  }

  /** 배당 쪽 집계도 같은 값으로 좁혀야 한다 - 한쪽만 좁히면 표 안에서 매매와 배당의 범위가 갈린다. */
  @Test
  void 배당_집계도_같은_값으로_좁힌다() {
    UUID a1 = UUID.fromString("00000000-0000-4000-8000-000000000001");
    service.findYearlyCost(
        UUID.randomUUID(), null, null, ZoneId.of("Asia/Seoul"), List.of(a1), null);
    org.mockito.Mockito.verify(dividendRepository)
        .findYearlyIncome(any(), any(), any(), any(), eq(a1.toString()), eq(null));
  }

  /**
   * 질의가 실제로 그 값을 쓰는지.
   *
   * <p>서비스가 값을 넘겨도 SQL 이 안 읽으면 아무것도 달라지지 않는다. {@code IN (:list)} 는 빈 목록에서 {@code IN ()} 이 되어 문법 오류가
   * 나므로 이 저장소가 이미 쓰는 {@code string_to_array(...)::uuid[]} 방식이어야 한다.
   */
  @Test
  void 두_질의_모두_넘긴_값으로_좁힌다() throws IOException {
    String trade =
        flatten(
            Files.readString(
                Path.of("src/main/java/net/luversof/api/stock/repository/TradeRepository.java"),
                StandardCharsets.UTF_8));
    String dividend =
        flatten(
            Files.readString(
                Path.of("src/main/java/net/luversof/api/stock/repository/DividendRepository.java"),
                StandardCharsets.UTF_8));
    assertThat(trade).contains(flatten(clause("t", "account_id", ":accountIds")));
    assertThat(trade).contains(flatten(clause("t", "stockItem_id", ":stockItemIds")));
    assertThat(dividend).contains(flatten(clause("d", "account_id", ":accountIds")));
    assertThat(dividend).contains(flatten(clause("d", "stockItem_id", ":stockItemIds")));
  }

  private static String clause(String alias, String column, String param) {
    char quote = (char) 34;
    String name = quote + column + quote;
    return "AND (CAST("
        + param
        + " AS text) IS NULL OR "
        + alias
        + "."
        + name
        + " = ANY(string_to_array("
        + param
        + ", ',')::uuid[]))";
  }

  /** spotless 가 줄바꿈·들여쓰기를 바꾸므로 공백을 눌러 비교한다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
