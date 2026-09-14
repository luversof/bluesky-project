package net.luversof.web.gate.stock.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 화면의 기간 프리셋(이번달·1개월·…·올해)을 서버에서 그대로 계산한다.
 *
 * <p>{@code rangeMode} 는 어떤 프리셋 버튼이 눌렸는지 알리는 상태값이고 기간 자체는 {@code startDate}/{@code endDate} 로 온다.
 * 그래서 날짜 없이 이 값만 오면(공유 주소가 대표적이다) 기간이 정해지지 않는다. 화면 쪽 계산({@code date-range-picker.ts})은 선택 상태가 없을 때
 * 오늘을 기준으로 삼고, N 개월 프리셋은 "정확히 N 개월"이 되도록 시작일을 하루 밀어 준다. 여기서는 그 규칙을 그대로 옮긴다.
 *
 * <p>규칙은 이 클래스 한 곳에만 둔다. 두 벌이 되면 같은 화면의 컨트롤러끼리 다른 기간을 쓴다 &mdash; 이 세션에서 날짜 규칙이 복사되어 어긋난 결함이 반복해
 * 나왔다(전역 기간 hidden input, 사용자지정 이동).
 *
 * <p>{@code "all"} 은 호출부가 걸러 낸다(기간 없음이 곧 의도다). 알 수 없는 값은 예전 기본값인 올해(YTD)로 떨어뜨린다.
 */
public final class StockRangePresetUtil {

  private StockRangePresetUtil() {}

  /** 날짜 없이 프리셋 상태만 왔을 때 적용할 기간. {@code end} 는 '오늘까지'를 뜻하는 배타적 경계다. */
  public record PresetRange(Instant start, Instant end, String mode) {}

  /** 기간을 걸지 않겠다는 뜻의 프리셋인가. */
  public static boolean isAll(String rangeMode) {
    return "all".equalsIgnoreCase(rangeMode == null ? null : rangeMode.trim());
  }

  /**
   * 앞뒤가 뒤집힌 기간을 바로잡는다. 규칙은 여기 한 곳에만 둔다.
   *
   * <p>실측 2026-09-11: 목록 네 화면(매매·배당·활동·자산 성장)은 뒤집힌 주소를 받으면 앞뒤를 바꿔 2026-01-01 ~ 2026-09-11 로 보여 줬는데,
   * <b>종목 상세·계좌 상세만</b> 그대로 두어 배지가 "2026-09-12 ~ 2025-12-31" 로 뜨고 내역이 1~3 행으로 비었다.
   */
  public static Instant[] ordered(Instant start, Instant end) {
    if (start != null && end != null && start.isAfter(end)) {
      return new Instant[] {end, start};
    }
    return new Instant[] {start, end};
  }

  /** 프리셋 상태값이 실제로 들어왔는가(빈 문자열은 안 들어온 것으로 본다). */
  public static boolean hasMode(String rangeMode) {
    return rangeMode != null && !rangeMode.isBlank();
  }

  public static PresetRange resolve(String rangeMode, ZoneId zone) {
    LocalDate today = LocalDate.now(zone);
    String mode = rangeMode == null ? "" : rangeMode.trim();
    LocalDate from;
    String resolvedMode;
    if ("mtd".equalsIgnoreCase(mode)) {
      from = today.withDayOfMonth(1);
      resolvedMode = "mtd";
    } else if (mode.matches("[1-9][0-9]{0,3}") && Long.parseLong(mode) <= 1200L) {
      // 화면과 같은 규칙: minusMonths 는 양끝 포함이라 하루를 더해 정확히 N 개월로 만든다.
      from = today.minusMonths(Long.parseLong(mode)).plusDays(1);
      resolvedMode = mode;
    } else {
      from = LocalDate.of(today.getYear(), 1, 1);
      resolvedMode = "ytd";
    }
    return new PresetRange(
        from.atStartOfDay(zone).toInstant(),
        today.plusDays(1).atStartOfDay(zone).toInstant(),
        resolvedMode);
  }
}
