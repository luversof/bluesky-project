package net.luversof.web.gate.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * 영어 화면 달력의 공휴일 이름(2026-10-03). 표의 한국어 이름이 그대로 나가 영어 화면에 "개천절 · 한글날" 이 남았다(stock-en-hangul-scan).
 */
class KoreanHolidaysEnglishTest {

  @Test
  void 표의_이름은_모두_영어로_옮겨진다() {
    for (String name : KoreanHolidays.allNames()) {
      assertThat(KoreanHolidays.englishName(name))
          .as("영어 이름이 없다: " + name)
          .doesNotContainPattern("[가-힣]");
    }
  }

  @Test
  void 로케일에_따라_한국어_또는_영어() {
    LocalDate day = LocalDate.of(2026, 10, 5);
    assertThat(KoreanHolidays.holidayName(day, Locale.KOREAN)).isEqualTo("대체공휴일(개천절)");
    assertThat(KoreanHolidays.holidayName(day, Locale.ENGLISH))
        .isEqualTo("Substitute holiday (National Foundation Day)");
    // 겹친 날(2009-10-03 추석 + 개천절)
    assertThat(KoreanHolidays.holidayName(LocalDate.of(2009, 10, 3), Locale.ENGLISH))
        .isEqualTo("Chuseok · National Foundation Day");
    assertThat(KoreanHolidays.holidayName(LocalDate.of(2026, 10, 6), Locale.ENGLISH)).isNull();
  }

  @Test
  void 요청_밖에서는_JVM_기본_로케일이_아니라_표의_한국어() {
    // 2026-10-03 검토: 인자 하나짜리가 LocaleContextHolder.getLocale() 을 그대로 쓰면 요청 밖(시험 · 뒤 작업)에서 JVM 기본
    // 로케일로 떨어져
    // 기본이 en 인 환경에서 "어린이날" 단언이 깨졌다.
    Locale saved = Locale.getDefault();
    try {
      Locale.setDefault(Locale.ENGLISH);
      org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
      assertThat(KoreanHolidays.holidayName(LocalDate.of(2026, 10, 5))).isEqualTo("대체공휴일(개천절)");
    } finally {
      Locale.setDefault(saved);
    }
  }
}
