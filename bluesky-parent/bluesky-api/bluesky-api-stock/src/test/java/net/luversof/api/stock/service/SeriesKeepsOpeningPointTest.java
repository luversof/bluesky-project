package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 다운샘플한 시계열도 <b>구간의 기초 지점</b>을 남긴다.
 *
 * <p>다운샘플은 버킷마다 마지막 날만 남긴다. 그래서 첫 버킷의 앞머리가 통째로 사라지는데, 요약의 {@code openingValue} 는 바로 그 사라진 첫 지점의 값이다
 * - 차트의 왼쪽 끝과 카드가 서로 다른 수를 말하게 된다.
 *
 * <p>실측 2026-09-13(AUTO).
 *
 * <ul>
 *   <li>'2025년': 차트 첫 점 2025-01-04 456,672,455 / 기초 2024-12-31 447,114,050
 *   <li>'최근 3년': 차트 첫 점 2023-09-29 493,978,320 / 기초 2023-09-13 524,988,225 - 3,100 만 원 차이에 오르내림의
 *       방향까지 달라 보였다
 * </ul>
 *
 * <p>끝 지점은 마지막 버킷의 마지막 날이라 이미 남는다(실측: 여섯 구간 모두 일치했다). 흐름값은 0 으로 둔다 - 그 날의 흐름은 이미 첫 버킷의 대표 지점에 합산돼
 * 있다.
 */
class SeriesKeepsOpeningPointTest {

  private static final String SERVICE =
      "src/main/java/net/luversof/api/stock/service/TradeProfitService.java";

  private String read() throws IOException {
    return Files.readString(Path.of(SERVICE), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 다운샘플 결과에 기초 지점 병합이 걸려 있다. */
  @Test
  void 다운샘플_뒤에_기초_지점을_병합한다() throws IOException {
    assertThat(read())
        .contains(
            "return mergeOpeningPoint(series, mergeHoldingsValueExtremes(series, downsampled));");
  }

  /** 기초는 일별 시리즈의 첫 지점이다. */
  @Test
  void 기초는_일별_시리즈의_첫_지점이다() throws IOException {
    assertThat(body()).contains("TradeProfitTimeSeriesPoint opening = daily.get(0);");
  }

  /** 이미 있으면 더하지 않는다 - 같은 시각의 점이 둘이면 차트에 겹친 점이 생긴다. */
  @Test
  void 이미_있으면_더하지_않는다() throws IOException {
    String body = body();

    assertThat(body)
        .contains("if (point != null && opening.timestamp().equals(point.timestamp()))");
    assertThat(body).contains("return downsampled;");
  }

  /** 흐름값은 0 - 그 날의 거래는 이미 첫 버킷 대표 지점에 합산돼 있다. */
  @Test
  void 흐름값은_0_으로_둔다() throws IOException {
    String body = body();
    int at = body.indexOf("result.add(");

    assertThat(at).isPositive();
    assertThat(body.substring(at, Math.min(body.length(), at + 420)))
        .contains("BigDecimal.ZERO, 0L, 0L, 0L,");
  }

  /** 시각 순으로 다시 정렬한다 - 뒤에 붙이기만 하면 차트가 선을 되돌아 긋는다. */
  @Test
  void 시각_순으로_다시_정렬한다() throws IOException {
    assertThat(body())
        .contains("result.sort(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp));");
  }

  /** 최고 · 최저 waypoint 규칙은 그대로 둔다 - 이 병합이 그것을 대체하는 것이 아니다. */
  @Test
  void 최고_최저_병합은_그대로다() throws IOException {
    assertThat(read())
        .contains("private List<TradeProfitTimeSeriesPoint> mergeHoldingsValueExtremes(");
  }

  /** 새 메서드 한 덩어리만 떼어 낸다 - 파일 전체로 보면 형제 병합 코드에 걸린다. */
  private String body() throws IOException {
    String src = read();
    int at = src.indexOf("private List<TradeProfitTimeSeriesPoint> mergeOpeningPoint(");
    if (at < 0) {
      return "";
    }
    return src.substring(at, Math.min(src.length(), at + 1200));
  }
}
