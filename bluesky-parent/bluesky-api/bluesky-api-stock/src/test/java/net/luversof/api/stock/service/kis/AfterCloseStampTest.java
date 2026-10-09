package net.luversof.api.stock.service.kis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 오늘 행을 장중에 저장한 뒤 마감 뒤 갱신에서 값이 같으면 한 번 저장해 갱신 시각을 마감 뒤로(2026-10-03 검토). 안 그러면 그 뒤 체결이 없던 종목 하나로 화면
 * 전체가 종일 "10:16 장중 시세" 라고 불렀다.
 */
class AfterCloseStampTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final LocalDate DAY = LocalDate.of(2026, 10, 2);

  private static Instant kst(String time) {
    return java.time.LocalDateTime.parse("2026-10-02T" + time).atZone(KST).toInstant();
  }

  @Test
  void 장중에_저장한_오늘_행은_마감_뒤에_한_번_저장한다() {
    assertTrue(
        KisStockPriceUpdateService.needsAfterCloseStamp(kst("10:16"), kst("18:19"), DAY, DAY, KST));
    assertTrue(
        KisStockPriceUpdateService.needsAfterCloseStamp(
            kst("15:29:59"), kst("15:30"), DAY, DAY, KST));
  }

  @Test
  void 마감_전_갱신이거나_이미_마감_뒤에_저장했으면_아니다() {
    assertFalse(
        KisStockPriceUpdateService.needsAfterCloseStamp(
            kst("10:16"), kst("15:29:59"), DAY, DAY, KST));
    assertFalse(
        KisStockPriceUpdateService.needsAfterCloseStamp(kst("15:30"), kst("18:19"), DAY, DAY, KST));
    // 오늘 행이 아니면(과거 행은 needsFinalityConfirmation 이 맡는다)
    assertFalse(
        KisStockPriceUpdateService.needsAfterCloseStamp(
            kst("10:16"), kst("18:19"), DAY, DAY.plusDays(1), KST));
    assertFalse(KisStockPriceUpdateService.needsAfterCloseStamp(null, kst("18:19"), DAY, DAY, KST));
  }
}
