package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DataStatusResponse.ZeroVolumeChangedCloseRow;

/**
 * 거래량 0 인데 종가가 바뀐 행이 액면분할·병합인지 알아볼 수 있게 하는지.
 *
 * <p>관리 화면은 그동안 두 숫자만 적었다 &mdash; "쌍방울 2025-05-08 13450 → 2690". 그것만 보면 시세 수집이 깨진 것처럼 읽힌다. 실제로는 배율이
 * 정확히 1/5 인 5:1 액면분할이다(실측 2026-08-23: 이 행은 전체 시세 57,459 행 중 하나뿐이다).
 *
 * <p>분할로 설명되지 않는 배율까지 "분할" 이라고 적으면 오히려 진짜 이상을 덮는다. 그래서 1/2~1/20, 2~20 배의 깔끔한 정수비일 때만 적는다.
 */
class ZeroVolumeSplitRatioTest {

  private ZeroVolumeChangedCloseRow row(String previous, String close) {
    return new ZeroVolumeChangedCloseRow(
        "쌍방울", LocalDate.parse("2025-05-08"), new BigDecimal(previous), new BigDecimal(close));
  }

  /** 실측 행: 13,450 → 2,690 = 정확히 1/5. */
  @Test
  void 실측_행은_5대1_분할로_읽힌다() {
    assertThat(row("13450", "2690").splitRatioLabel()).isEqualTo("1:5");
    assertThat(row("13450", "2690").closeRatio()).isEqualByComparingTo("0.2000");
  }

  /** 반대 방향(병합)도 읽는다. */
  @Test
  void 병합도_읽는다() {
    assertThat(row("2690", "13450").splitRatioLabel()).isEqualTo("5:1");
  }

  /** 분할로 설명되지 않는 배율은 단정하지 않는다 - 진짜 이상일 수 있다. */
  @Test
  void 어중간한_배율은_분할이라고_적지_않는다() {
    assertThat(row("10000", "7300").splitRatioLabel()).isNull();
    assertThat(row("10000", "3300").splitRatioLabel()).isNull();
    // 1/20 은 보고 1/21 은 보지 않는다(범위 밖).
    assertThat(row("21000", "1050").splitRatioLabel()).isEqualTo("1:20");
    assertThat(row("21000", "1000").splitRatioLabel()).isNull();
  }

  /** 값이 없거나 0 이면 배율을 내지 않는다(0 으로 나누면 터진다). */
  @Test
  void 값이_없으면_배율을_내지_않는다() {
    assertThat(row("0", "2690").splitRatioLabel()).isNull();
    assertThat(
            new ZeroVolumeChangedCloseRow("쌍방울", LocalDate.parse("2025-05-08"), null, null)
                .closeRatio())
        .isNull();
  }
}
