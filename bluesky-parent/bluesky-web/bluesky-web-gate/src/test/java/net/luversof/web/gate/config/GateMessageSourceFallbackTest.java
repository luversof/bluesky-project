package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.github.luversof.boot.context.support.BlueskyReloadableResourceBundleMessageSource;

class GateMessageSourceFallbackTest {

  @Test
  void usesBaseEnglishBundleWhenSystemLocaleFallbackIsDisabled() {
    var previousDefaultLocale = Locale.getDefault();

    try {
      Locale.setDefault(Locale.KOREA);

      assertThat(resolveTradeTitle(true, Locale.US)).isEqualTo("매매 내역");
      assertThat(resolveTradeTitle(false, Locale.US)).isEqualTo("Trade History");
    } finally {
      Locale.setDefault(previousDefaultLocale);
    }
  }

  private String resolveTradeTitle(boolean fallbackToSystemLocale, Locale locale) {
    var messageSource = new BlueskyReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:gateMessage", "classpath:uiMessage");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(fallbackToSystemLocale);

    return messageSource.getMessage("stock.page.trade.title", null, locale);
  }
}
