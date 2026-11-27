package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;

/** 시황 분석의 회차 시점 · 방향은 말로 적고, 모르는 코드는 빈 칸 대신 코드 그대로 보여 준다. */
class StockAnalysisLabelUtilTest {

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREAN);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void 아는_코드를_말로_바꾼다() {
    assertThat(StockAnalysisLabelUtil.slot("NIGHT")).isEqualTo("06:00 야간선물 뒤");
    assertThat(StockAnalysisLabelUtil.slot("all")).isEqualTo("모든 시점 (운영 회차 우선)");
    assertThat(StockAnalysisLabelUtil.direction("UP")).isEqualTo("상승");
    assertThat(StockAnalysisLabelUtil.direction("FLAT")).isEqualTo("보합");
  }

  @Test
  void 영어_로케일에서도_말로_바꾼다() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    try {
      assertThat(StockAnalysisLabelUtil.slot("CLOSE")).isEqualTo("16:00 after the close");
      assertThat(StockAnalysisLabelUtil.direction("DOWN")).isEqualTo("Down");
    } finally {
      LocaleContextHolder.setLocale(Locale.KOREAN);
    }
  }

  @Test
  void 모르는_코드는_그대로_없으면_대시() {
    assertThat(StockAnalysisLabelUtil.slot("LUNCH")).isEqualTo("LUNCH");
    assertThat(StockAnalysisLabelUtil.direction("SIDEWAYS")).isEqualTo("SIDEWAYS");
    assertThat(StockAnalysisLabelUtil.slot(null)).isEqualTo("-");
    assertThat(StockAnalysisLabelUtil.direction(null)).isEqualTo("-");
  }
}
