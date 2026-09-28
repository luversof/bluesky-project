package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;

/**
 * 월배당 프로필 표의 여섯 열 정렬.
 *
 * <p>기존 테스트는 {@code display-order asc} 한 경우만 봤다. 나머지 다섯 열과 내림차순, 그리고 빈 값이 어디로 가는지는 고정돼 있지 않았다 - 실측
 * 2026-09-12: 화면은 여섯 열 모두에 정렬 링크를 내보낸다.
 *
 * <p>빈 값은 <b>방향과 무관하게 늘 뒤로</b> 간다. 오름차순에서만 뒤로 보내면 내림차순에서 빈 줄이 맨 위에 몰려, 아직 확인하지 않은 프로필이 가장 최근에 확인한
 * 것처럼 보인다.
 */
class MonthlyDividendProfileSortTest {

  private final MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();

  private MonthlyDividendProfileResponse profile(
      String symbol,
      Integer displayOrder,
      String payoutWindow,
      String sourceUrl,
      String lastVerifiedDate,
      boolean active) {
    return new MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol + " 이름",
        sourceUrl,
        payoutWindow,
        displayOrder,
        active,
        "",
        lastVerifiedDate == null ? null : LocalDate.parse(lastVerifiedDate),
        Instant.parse("2026-09-01T00:00:00Z"),
        null,
        null);
  }

  private List<String> symbolsOf(List<MonthlyDividendProfileResponse> rows) {
    return rows.stream().map(MonthlyDividendProfileResponse::stockItemSymbol).toList();
  }

  private List<MonthlyDividendProfileResponse> sample() {
    return Arrays.asList(
        profile("CCC", 3, "MONTH_END", "https://c.example", "2026-08-01", false),
        profile("AAA", 1, "MID_MONTH", "https://a.example", "2026-09-01", true),
        profile("BBB", 2, null, null, null, true));
  }

  @Test
  void 종목코드는_양방향으로_정렬된다() {
    assertThat(symbolsOf(support.sortProfiles(sample(), "symbol", "asc")))
        .containsExactly("AAA", "BBB", "CCC");
    assertThat(symbolsOf(support.sortProfiles(sample(), "symbol", "desc")))
        .containsExactly("CCC", "BBB", "AAA");
  }

  @Test
  void 지급_시기는_양방향으로_정렬되고_빈_값은_뒤다() {
    assertThat(symbolsOf(support.sortProfiles(sample(), "payout-window", "asc")))
        .as("MID_MONTH < MONTH_END, 빈 값은 뒤")
        .containsExactly("AAA", "CCC", "BBB");
    assertThat(symbolsOf(support.sortProfiles(sample(), "payout-window", "desc")))
        .as("뒤집어도 빈 값은 여전히 뒤")
        .containsExactly("CCC", "AAA", "BBB");
  }

  @Test
  void 출처_주소도_같은_규칙이다() {
    assertThat(symbolsOf(support.sortProfiles(sample(), "source-url", "asc")))
        .containsExactly("AAA", "CCC", "BBB");
    assertThat(symbolsOf(support.sortProfiles(sample(), "source-url", "desc")))
        .containsExactly("CCC", "AAA", "BBB");
  }

  @Test
  void 최종_검증일은_없는_줄이_늘_뒤다() {
    assertThat(symbolsOf(support.sortProfiles(sample(), "last-verified-date", "asc")))
        .containsExactly("CCC", "AAA", "BBB");
    assertThat(symbolsOf(support.sortProfiles(sample(), "last-verified-date", "desc")))
        .as("확인 안 한 줄이 맨 위로 올라오면 안 된다")
        .containsExactly("AAA", "CCC", "BBB");
  }

  @Test
  void 활성_여부는_참거짓_순서다() {
    assertThat(symbolsOf(support.sortProfiles(sample(), "active", "asc")))
        .as("꺼진 것이 먼저, 같으면 종목코드")
        .containsExactly("CCC", "AAA", "BBB");
    assertThat(symbolsOf(support.sortProfiles(sample(), "active", "desc")))
        .containsExactly("AAA", "BBB", "CCC");
  }

  @Test
  void 표시_순서는_내림차순도_되고_빈_값은_뒤다() {
    List<MonthlyDividendProfileResponse> rows =
        Arrays.asList(
            profile("CCC", 3, "MONTH_END", "https://c.example", "2026-08-01", true),
            profile("AAA", 1, "MID_MONTH", "https://a.example", "2026-09-01", true),
            profile("BBB", null, "OTHER", "https://b.example", "2026-07-01", true));
    assertThat(symbolsOf(support.sortProfiles(rows, "display-order", "asc")))
        .containsExactly("AAA", "CCC", "BBB");
    assertThat(symbolsOf(support.sortProfiles(rows, "display-order", "desc")))
        .containsExactly("CCC", "AAA", "BBB");
  }

  /** 방향을 주지 않았을 때의 기본값 - 표를 처음 열었을 때 어느 쪽으로 서는지. */
  @Test
  void 방향_기본값은_열마다_다르다() {
    assertThat(support.resolveProfileDirection("symbol", null)).isEqualTo("asc");
    assertThat(support.resolveProfileDirection("payout-window", null)).isEqualTo("asc");
    assertThat(support.resolveProfileDirection("source-url", null)).isEqualTo("asc");
    assertThat(support.resolveProfileDirection("display-order", null)).isEqualTo("asc");
    assertThat(support.resolveProfileDirection("last-verified-date", null))
        .as("최근에 확인한 것부터 보는 편이 쓸모 있다")
        .isEqualTo("desc");
    assertThat(support.resolveProfileDirection("active", null)).as("켜진 것부터").isEqualTo("desc");
    assertThat(support.resolveProfileDirection("symbol", "DESC")).isEqualTo("desc");
  }
}
