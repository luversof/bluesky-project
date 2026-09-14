package net.luversof.web.gate.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 공휴일 정적 테이블. 활동 캘린더가 빨간 날과 이름을 여기서 얻는다.
 *
 * <p>테스트가 없었다(실측 2026-09-13: 게이트에서 테스트가 한 번도 부르지 않는 public 메서드 23 개 중 하나). 음력·대체공휴일은 규칙 계산이 복잡해
 * <b>사람이 한 해씩 손으로 넣는</b> 표라, 새 해를 넣을 때 고정 공휴일 한둘을 빠뜨리기 쉽다. 빠뜨려도 화면은 그 날을 평일로 그릴 뿐 아무 말도 하지 않는다.
 *
 * <p>실측 2026-09-13: 수록 범위는 2009~2026(309 항목)이고 그 안에서 누락은 0 이었다. 이 테스트는 <b>수록된 해를 스스로 찾아</b> 검사하므로
 * 2027 을 넣으면 그 해도 자동으로 대상이 된다.
 */
class KoreanHolidaysTest {

  /** 날짜가 법으로 고정된 공휴일. 음력(설·추석·석가탄신일)과 대체공휴일은 해마다 달라 여기서 다루지 않는다. */
  private static final MonthDay[] FIXED = {
    MonthDay.of(1, 1), // 신정
    MonthDay.of(3, 1), // 삼일절
    MonthDay.of(5, 5), // 어린이날
    MonthDay.of(6, 6), // 현충일
    MonthDay.of(8, 15), // 광복절
    MonthDay.of(10, 3), // 개천절
    MonthDay.of(12, 25), // 성탄절
  };

  /** 그 해에 항목이 하나라도 있으면 "수록된 해"로 본다. */
  private boolean covered(int year) {
    LocalDate day = LocalDate.of(year, 1, 1);
    LocalDate end = LocalDate.of(year, 12, 31);
    while (!day.isAfter(end)) {
      if (KoreanHolidays.holidayName(day) != null) {
        return true;
      }
      day = day.plusDays(1);
    }
    return false;
  }

  private List<Integer> coveredYears() {
    List<Integer> years = new ArrayList<>();
    for (int year = 2000; year <= 2040; year++) {
      if (covered(year)) {
        years.add(year);
      }
    }
    return years;
  }

  @Test
  void 수록된_해가_끊기지_않는다() {
    List<Integer> years = coveredYears();

    assertThat(years).as("표가 비어 있으면 캘린더가 통째로 평일이 된다").isNotEmpty();
    for (int i = 1; i < years.size(); i++) {
      assertThat(years.get(i) - years.get(i - 1))
          .as("수록 연도 사이에 빈 해가 있으면 그 해만 조용히 평일이 된다: " + years)
          .isEqualTo(1);
    }
  }

  /** 한 해를 새로 넣을 때 고정 공휴일을 빠뜨리기 쉽다 - 빠져도 화면은 아무 말 없이 평일로 그린다. */
  @Test
  void 수록된_해에는_고정_공휴일이_모두_있다() {
    List<String> missing = new ArrayList<>();
    for (int year : coveredYears()) {
      for (MonthDay monthDay : FIXED) {
        LocalDate date = monthDay.atYear(year);
        if (KoreanHolidays.holidayName(date) == null) {
          missing.add(date.toString());
        }
      }
    }

    assertThat(missing).as("빠진 고정 공휴일").isEmpty();
  }

  /** 한글날은 2013 년부터 공휴일이다(2009~2012 는 비공휴일) - 파일 주석이 밝힌 제도 변화. */
  @Test
  void 한글날은_2013년부터다() {
    for (int year : coveredYears()) {
      LocalDate hangul = LocalDate.of(year, 10, 9);
      if (year >= 2013) {
        assertThat(KoreanHolidays.holidayName(hangul)).as(year + " 한글날").isNotNull();
      } else {
        assertThat(KoreanHolidays.holidayName(hangul)).as(year + " 은 한글날이 공휴일이 아니었다").isNull();
      }
    }
  }

  /** 같은 날에 둘이 겹치면 이름을 모두 보여 준다 - 하나만 남기면 다른 하나가 사라진다. */
  @Test
  void 겹친_공휴일은_모두_읊는다() {
    // 실측: 2025-05-05 는 어린이날과 석가탄신일이 겹친다.
    String name = KoreanHolidays.holidayName(LocalDate.of(2025, 5, 5));

    assertThat(name).contains("어린이날");
    assertThat(name).contains("석가탄신일");
    assertThat(name).contains("·");
  }

  @Test
  void 공휴일이_아니면_널이고_널_입력도_널이다() {
    assertThat(KoreanHolidays.holidayName(LocalDate.of(2026, 9, 10))).isNull();
    assertThat(KoreanHolidays.holidayName(null)).isNull();
  }

  /** 수록 범위 밖은 null 이다 - 캘린더는 그 달을 주말 색만 칠한다(문서화된 한계). */
  @Test
  void 수록_범위_밖은_널이다() {
    List<Integer> years = coveredYears();
    int after = years.get(years.size() - 1) + 1;
    int before = years.get(0) - 1;

    assertThat(KoreanHolidays.holidayName(LocalDate.of(after, 1, 1)))
        .as(after + " 신정 - 표에 없으면 null")
        .isNull();
    assertThat(KoreanHolidays.holidayName(LocalDate.of(before, 1, 1))).isNull();
  }
}
