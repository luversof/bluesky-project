package net.luversof.web.gate.stock.util;

import java.time.ZoneId;

/**
 * 타임존 문자열을 ZoneId 로 바꾼다. 값이 없으면 서버 존, 값을 줬는데 모르는 값이면 이름을 들고 끊는다.
 *
 * <p>ZoneId.of 를 그대로 부르면 알 수 없는 값에서 ZoneRulesException 이 난다. 템플릿 렌더 중에 나면 응답이 500 이 되고, 컨트롤러에서 나면
 * 공통 예외 처리기가 본문 없는 200 으로 바꿔 htmx 가 빈 내용을 갈아끼운다(화면이 조용히 빈다). 그래서 여기서 한 번에 거른다.
 *
 * <p>2026-09-11 까지는 모르는 값을 <b>서버 존으로 조용히 바꿔</b> 썼다. 실측({@code ?timeZone=Not/AZone} 으로 다섯 화면): 자산성장만
 * 일반 문구로 실패하고 나머지 넷은 아무 말 없이 그려졌다 &mdash; 그 넷은 주소에 적은 존이 아니라 서버 존으로 계산한 값이다. 존은 일자 경계를 옮기므로(같은 요청의
 * {@code UTC} 응답은 {@code Asia/Seoul} 과 다르다) 조용한 폴백은 틀린 값을 그럴듯하게 만든다.
 */
public final class StockZoneUtil {

  private StockZoneUtil() {}

  /** 파라미터 이름이 따로 없는 자리(내부 호출)는 timeZone 으로 적는다. */
  public static ZoneId resolve(String timeZone) {
    return resolve("timeZone", timeZone);
  }

  public static ZoneId resolve(String name, String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return ZoneId.systemDefault();
    }
    try {
      return ZoneId.of(timeZone.trim());
    } catch (Exception ex) {
      throw new net.luversof.web.gate.stock.support.StockZoneParamException(name, timeZone, ex);
    }
  }
}
