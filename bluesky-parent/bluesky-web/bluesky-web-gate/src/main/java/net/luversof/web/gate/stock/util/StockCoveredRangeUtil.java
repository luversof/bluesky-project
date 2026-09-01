package net.luversof.web.gate.stock.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

/**
 * 화면이 실제로 <b>덮은 구간</b>(가장 이른 자료 ~ 가장 늦은 자료).
 *
 * <p>기간 막대의 "전체" 는 날짜를 아예 보내지 않는다 &mdash; 거는 게 아니라 거는 것을 그만두는 것이라 시작·종료가 {@code null} 이다. 그래서 오른쪽
 * 배지에 찍을 날짜가 없어 "전체" 만 구간 표기가 사라졌다. 그때는 <b>조회된 자료의 구간</b>을 대신 적는다.
 *
 * <p>구간은 화면마다 다르다 &mdash; 종목 상세는 그 종목의 첫 거래일부터고, 매매 목록은 필터에 걸린 거래의 범위다. 그러니 화면이 <b>자기가 그린 자료</b>에서
 * 뽑아야 한다. 사용자 전체의 최초 일자({@code dataFirstDate})를 쓰면 종목 상세에서 그 종목과 무관한 날짜가 찍힌다.
 */
public final class StockCoveredRangeUtil {

  private StockCoveredRangeUtil() {}

  /** 시작·끝 각각. 자료가 없으면 두 값 모두 {@code null}. */
  public record Covered(LocalDate startDate, LocalDate endDate) {

    public boolean isEmpty() {
      return startDate == null && endDate == null;
    }
  }

  public static final Covered EMPTY = new Covered(null, null);

  /**
   * @param rows 화면이 그린 자료
   * @param when 각 행의 시각을 꺼내는 함수. {@code null} 을 돌려주는 행은 센다고 볼 수 없어 건너뛴다
   * @param zoneId 날짜로 접을 존. 화면의 다른 날짜 칸과 같은 존이어야 하루가 어긋나지 않는다
   */
  public static <T> Covered covered(List<T> rows, Function<T, Instant> when, ZoneId zoneId) {
    if (rows == null || rows.isEmpty() || when == null || zoneId == null) {
      return EMPTY;
    }
    LocalDate first = null;
    LocalDate last = null;
    for (T row : rows) {
      if (row == null) {
        continue;
      }
      Instant at = when.apply(row);
      if (at == null) {
        continue;
      }
      LocalDate date = at.atZone(zoneId).toLocalDate();
      if (first == null || date.isBefore(first)) {
        first = date;
      }
      if (last == null || date.isAfter(last)) {
        last = date;
      }
    }
    return new Covered(first, last);
  }
}
