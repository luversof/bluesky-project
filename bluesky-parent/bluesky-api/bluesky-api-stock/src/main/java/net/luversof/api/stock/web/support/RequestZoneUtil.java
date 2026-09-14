package net.luversof.api.stock.web.support;

import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 요청의 타임존 문자열을 해석한다.
 *
 * <p>규칙은 {@code granularity} / {@code breakdown} 과 같다 &mdash; 값을 아예 주지 않으면 종전 기본값, 값을 줬는데 모르는 값이면
 * 400 이다.
 *
 * <p>실측 2026-09-11: {@code granularity=BOGUS} 는 400 인데 {@code timeZone=Not/AZone} 은 <b>200</b> 이었고,
 * 응답은 타임존을 주지 않은 것과 바이트 단위로 같았다(전체 기간 일별 시계열 1,652,366 자, md5 동일). 타임존이 결과를 바꾸지 않아서가 아니라 조용히 버려져서다
 * &mdash; 같은 요청의 {@code timeZone=UTC} 는 1,652,594 자로 다르고, 첫 점의 경계가 {@code 2008-12-31T15:00Z} 에서
 * {@code 2008-12-31T00:00Z} 로 옮겨간다.
 *
 * <p>이 서비스는 UTC 로 뜬 컨테이너에서 돈다. 거기서 오타 난 타임존은 서버 기본값(UTC)으로 떨어져 KST 오전 거래가 전날로 집계된다 &mdash; 호출자는 한국
 * 기준을 적었다고 믿는데 하루씩 밀린 차트를 받고, 오타는 끝까지 드러나지 않는다.
 */
public final class RequestZoneUtil {

  private RequestZoneUtil() {}

  /**
   * 값이 없으면 {@code fallback}, 알 수 없는 값이면 400.
   *
   * @param timeZone 요청이 준 타임존 ID(널 허용)
   * @param fallback 값이 없을 때 쓸 존(널 허용 &mdash; 널을 그대로 돌려주는 호출자가 있다)
   */
  public static ZoneId parse(String timeZone, ZoneId fallback) {
    if (timeZone == null || timeZone.isBlank()) {
      return fallback;
    }
    try {
      return ZoneId.of(timeZone.trim());
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported timeZone: " + timeZone);
    }
  }
}
