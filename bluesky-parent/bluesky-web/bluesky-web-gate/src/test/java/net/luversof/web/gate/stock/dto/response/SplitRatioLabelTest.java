package net.luversof.web.gate.stock.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * 하루 배율이 액면분할 · 병합으로 설명되는지(1/2 ~ 1/20, 2 ~ 20 배, 차이 0.0005 까지). 렌더
 * 시험(view/ZeroVolumeSplitRatioTest)만 있어 경계를 바로 보는 시험을 2026-10-22 에 더했다.
 */
class SplitRatioLabelTest {

  private static String label(String ratio) {
    return DataStatusResponse.splitRatioLabelOf(ratio == null ? null : new BigDecimal(ratio));
  }

  @Test
  void 분할과_병합의_비를_적는다() {
    assertThat(label("0.2")).isEqualTo("1:5");
    assertThat(label("0.2004")).as("0.0005 안").isEqualTo("1:5");
    assertThat(label("0.2005")).as("차이가 딱 0.0005 - 포함").isEqualTo("1:5");
    assertThat(label("0.3333")).as("1/3 반올림").isEqualTo("1:3");
    assertThat(label("5")).isEqualTo("5:1");
    assertThat(label("20")).isEqualTo("20:1");
    assertThat(label("0.05")).isEqualTo("1:20");
  }

  @Test
  void 설명되지_않으면_null() {
    assertThat(label("0.21")).isNull();
    assertThat(label("21")).as("20 배 넘음").isNull();
    assertThat(label("1")).as("변동 없음").isNull();
    assertThat(label("0.2006")).as("0.0005 밖").isNull();
    assertThat(label(null)).isNull();
  }
}
