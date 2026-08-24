package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 지급 지연 판정의 기준 간격은 <b>최근</b> 지급만 본다.
 *
 * <p>기준을 상수로 박지 않고 종목 자신의 과거 간격에서 뽑는 것은 맞지만, 전체 이력을 쓰면 주기가 바뀐 종목에서 옛 간격이 기준을 무력화한다.
 *
 * <p>실측 2026-08-23: TIGER 리츠부동산인프라는 2020-02 ~ 2022-11 에 <b>분기</b> 배당이었다(간격 88~94 일). 2022-11 부터
 * 월배당으로 바뀌어 최근 12 간격은 27~34 일인데, 전체 최대 94 일을 기준으로 쓰면 3 개월이 비어도 지연으로 잡히지 않았다.
 *
 * <p>나머지 7 종목은 최근 12 간격의 최대가 전체 최대와 같거나 1 일 차이라 판정이 달라지지 않는다.
 */
class RecentPayoutGapWindowTest {

  /** 시작일부터 간격 목록대로 이어지는 지급일. */
  private List<LocalDate> days(String start, int... gaps) {
    List<LocalDate> days = new ArrayList<>();
    LocalDate current = LocalDate.parse(start);
    days.add(current);
    for (int gap : gaps) {
      current = current.plusDays(gap);
      days.add(current);
    }
    return days;
  }

  @Test
  void 주기가_바뀌면_옛_간격은_기준이_되지_않는다() {
    // 분기(91일) 3회 뒤 월배당(30일 안팎) 12회
    List<LocalDate> days =
        days("2020-02-04", 91, 91, 91, 29, 30, 33, 28, 34, 29, 29, 29, 34, 27, 30, 33);

    assertThat(DataStatusController.widestRecentGapDays(days))
        .as("전체를 보면 91일이라 3개월이 비어도 지연으로 잡히지 않는다")
        .isEqualTo(34);
  }

  @Test
  void 주기가_그대로면_전체를_봐도_같다() {
    List<LocalDate> days = days("2025-01-17", 31, 30, 33, 29, 31, 30, 33, 31, 30, 31, 33);

    assertThat(DataStatusController.widestRecentGapDays(days)).isEqualTo(33);
  }

  /** 이력이 창보다 짧으면 있는 만큼만 본다. */
  @Test
  void 이력이_짧으면_있는_만큼만_본다() {
    assertThat(DataStatusController.widestRecentGapDays(days("2026-01-17", 31, 45))).isEqualTo(45);
  }

  @Test
  void 지급이_한_건뿐이면_간격이_없다() {
    assertThat(DataStatusController.widestRecentGapDays(List.of(LocalDate.parse("2026-01-17"))))
        .isEqualTo(0);
  }
}
