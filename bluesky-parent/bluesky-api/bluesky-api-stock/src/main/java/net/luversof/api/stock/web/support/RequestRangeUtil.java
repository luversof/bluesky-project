package net.luversof.api.stock.web.support;

import java.time.Instant;

/**
 * 조회 구간의 앞뒤를 바로잡는다.
 *
 * <p>뒤집힌 구간({@code startDate > endDate})은 어느 저장소 질의에도 걸리지 않아 <b>빈 결과</b>가 된다. 그 빈 결과는 화면에서 "자료가
 * 없습니다" 와 구분되지 않는다 &mdash; 실측 2026-09-12(같은 사용자, 같은 두 날짜를 앞뒤만 바꿔서):
 *
 * <pre>
 *   /api/yearlyCost                       14 행 → 0 행
 *   /api/tradeProfit/calculateProfit      43 행 → 0 행
 *   /api/tradeProfit/timeSeriesWithSummary 10,864 B → 506 B
 *   /api/periodSummary                       275 B → 206 B
 * </pre>
 *
 * <p>게이트는 이미 들머리에서 바로잡아 보내므로(피커와 같은 규칙) 지금 화면에 보이는 증상은 없다. 그러나 이 계약이 "뒤집으면 빈 결과" 인 한, 다음 호출자는 같은
 * 함정을 다시 밟는다. 한쪽이라도 비어 있으면 판단할 것이 없으므로 그대로 둔다.
 */
public final class RequestRangeUtil {

  private RequestRangeUtil() {}

  /**
   * @return 길이 2 배열. 둘 다 있고 앞이 뒤보다 늦으면 서로 바꾼다. 그 밖에는 받은 값 그대로.
   */
  public static Instant[] ordered(Instant startDate, Instant endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      return new Instant[] {endDate, startDate};
    }
    return new Instant[] {startDate, endDate};
  }
}
