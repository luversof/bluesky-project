package net.luversof.web.gate.stock.util;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 월배당 프로필의 <b>지급 시기</b> 코드를 사람이 읽는 말로 바꾼다.
 *
 * <p>같은 화면(관리 &rarr; 월배당 기준 데이터)의 편집 폼은 예전부터 "월중 / 월말 / 기타 / 미확인" 으로 보여 주는데, 표는 코드를 그대로 찍고 있었다
 * &mdash; 실측 2026-09-12: 프로필 8 행의 "지급 시기" 칸이 {@code MID_MONTH} · {@code MONTH_END} 였다. 한 화면이 같은 값을
 * 두 말로 적은 셈이다.
 *
 * <p>문구는 폼이 쓰는 키를 그대로 쓴다. 모르는 코드는 감추지 않고 코드 그대로 보여 준다 &mdash; 관리 화면에서는 "무엇이 들어 있는지" 가 "예쁘게 보이는 것"
 * 보다 중요하다.
 */
public final class StockPayoutWindowLabelUtil {

  private static final String PREFIX =
      "stock.page.dividend.monthly.reference.profile.payout.window.";

  private StockPayoutWindowLabelUtil() {}

  /** 코드에 대응하는 문구. 값이 없거나 모르는 코드면 받은 값을 그대로 돌려준다. */
  public static String label(String payoutWindow) {
    if (payoutWindow == null || payoutWindow.isBlank()) {
      return "";
    }
    String key =
        switch (payoutWindow) {
          case "MID_MONTH" -> PREFIX + "mid.month";
          case "MONTH_END" -> PREFIX + "month.end";
          case "OTHER" -> PREFIX + "other";
          case "UNKNOWN" -> PREFIX + "unknown";
          default -> null;
        };
    return key == null ? payoutWindow : MessageUtil.getMessage(key);
  }
}
