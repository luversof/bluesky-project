package net.luversof.web.gate.stock.util;

import java.time.ZoneId;

/**
 * 타임존 문자열을 안전하게 ZoneId 로 바꾼다.
 *
 * <p>ZoneId.of 를 그대로 부르면 알 수 없는 값에서 ZoneRulesException 이 난다. 템플릿 렌더 중에 나면 응답이 500 이 되고, 컨트롤러에서 나면
 * 공통 예외 처리기가 본문 없는 200 으로 바꿔 htmx 가 빈 내용을 갈아끼운다(화면이 조용히 빈다).
 */
public final class StockZoneUtil {

  private StockZoneUtil() {}

  public static ZoneId resolve(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return ZoneId.systemDefault();
    }
    try {
      return ZoneId.of(timeZone);
    } catch (Exception ex) {
      return ZoneId.systemDefault();
    }
  }
}
