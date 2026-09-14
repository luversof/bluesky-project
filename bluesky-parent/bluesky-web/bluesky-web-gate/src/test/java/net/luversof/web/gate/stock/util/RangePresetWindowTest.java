package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 기간 프리셋이 라벨대로의 구간을 만든다.
 *
 * <p>실측 2026-09-12(매매 화면에서 버튼을 눌러 얻은 날짜 칸, 오늘 = 2026-09-12):
 *
 * <pre>
 *   이번달 2026-09-01 ~ 09-12   1개월 2026-08-13 ~ 09-12   3개월 2026-06-13 ~ 09-12
 *   6개월 2026-03-13 ~ 09-12    올해  2026-01-01 ~ 09-12   1년   2025-09-13 ~ 2026-09-12
 *   3년   2023-09-13 ~ 2026-09-12                          전체  (날짜 없음)
 * </pre>
 *
 * <p>N 개월은 {@code minusMonths(N)} 이 양끝을 포함하므로 하루를 밀어 "정확히 N 개월"이 된다. 이 규칙에는 가드가 없었다 &mdash; {@code
 * plusDays(1)} 하나가 빠지면 모든 기간이 하루씩 길어지고, 합계·수익률이 조용히 달라진다.
 */
class RangePresetWindowTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private LocalDate startOf(String mode) {
    return LocalDate.ofInstant(StockRangePresetUtil.resolve(mode, KST).start(), KST);
  }

  private LocalDate endOf(String mode) {
    return LocalDate.ofInstant(StockRangePresetUtil.resolve(mode, KST).end(), KST);
  }

  @Test
  void 이번달은_그_달_1일부터다() {
    LocalDate today = LocalDate.now(KST);

    assertThat(startOf("mtd")).isEqualTo(today.withDayOfMonth(1));
    assertThat(StockRangePresetUtil.resolve("mtd", KST).mode()).isEqualTo("mtd");
  }

  @Test
  void N개월은_정확히_N개월이다() {
    LocalDate today = LocalDate.now(KST);

    for (String mode : new String[] {"1", "3", "6", "12", "36"}) {
      long months = Long.parseLong(mode);
      assertThat(startOf(mode))
          .as(mode + "개월은 minusMonths 뒤 하루를 밀어야 양끝 포함으로 정확히 N 개월이 된다")
          .isEqualTo(today.minusMonths(months).plusDays(1));
      assertThat(StockRangePresetUtil.resolve(mode, KST).mode()).isEqualTo(mode);
    }
  }

  @Test
  void 끝은_오늘_다음날_0시_배타적이다() {
    LocalDate today = LocalDate.now(KST);

    for (String mode : new String[] {"mtd", "1", "ytd"}) {
      assertThat(endOf(mode)).as(mode + " 의 끝").isEqualTo(today.plusDays(1));
    }
  }

  /** 문서화된 폴백 - 알 수 없는 값은 올해다(2026-09-12 실측: BOGUS · 0 · 9999 · -1 모두 올해). */
  @Test
  void 모르는_값은_올해로_떨어진다() {
    LocalDate today = LocalDate.now(KST);

    for (String mode : new String[] {"BOGUS", "0", "9999", "-1", "", null}) {
      var resolved = StockRangePresetUtil.resolve(mode, KST);
      assertThat(LocalDate.ofInstant(resolved.start(), KST))
          .as(String.valueOf(mode) + " 는 올해 1월 1일부터")
          .isEqualTo(LocalDate.of(today.getYear(), 1, 1));
      assertThat(resolved.mode()).isEqualTo("ytd");
    }
  }

  @Test
  void 전체는_기간을_걸지_않는다() {
    assertThat(StockRangePresetUtil.isAll("all")).isTrue();
    assertThat(StockRangePresetUtil.isAll(" ALL ")).isTrue();
    assertThat(StockRangePresetUtil.isAll("ytd")).isFalse();
    assertThat(StockRangePresetUtil.isAll(null)).isFalse();
  }
}
