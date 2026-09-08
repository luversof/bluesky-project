package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 정적 문맥의 메시지 헬퍼.
 *
 * <p>월배당 기준 관리의 검증·실패 문구 99 줄이 한글 리터럴이었다(실측 2026-09-08) &mdash; 로케일 전환이 있는데도 그 문구만 영어 화면에 한글로 나갔다.
 * 키로 옮기려면 파서·서포트 같은 정적 문맥에서도 인자를 끼워 문구를 만들 수 있어야 한다.
 */
class StockViewSupportMessageTest {

  private static final String KEY = "stock.monthly.reference.error.bulk.pay.date.before.record";

  @BeforeEach
  void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterEach
  void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  /** 한국어 문구는 옮기기 전과 글자 하나 다르지 않다. 기존 파서 검사가 이 문장을 그대로 단정한다. */
  @Test
  void 한국어_문구에_인자를_끼운다() {
    LocaleContextHolder.setLocale(Locale.KOREAN);

    assertThat(
            StockViewSupport.msg(
                KEY, 2, LocalDate.parse("2025-05-19"), LocalDate.parse("2026-05-15")))
        .isEqualTo("2번째 줄의 실지급일(2025-05-19)은 지급기준일(2026-05-15)보다 빠를 수 없습니다.");
  }

  /** 영어 로케일에서는 한글이 한 글자도 나가지 않는다 - 이것이 옮긴 이유다. */
  @Test
  void 영어_로케일에서는_한글이_나가지_않는다() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String text =
        StockViewSupport.msg(KEY, 2, LocalDate.parse("2025-05-19"), LocalDate.parse("2026-05-15"));

    assertThat(text)
        .contains("Line 2")
        .contains("2025-05-19")
        .doesNotMatch(".*[\\uac00-\\ud7a3].*");
  }

  /** 없는 키는 키를 그대로 돌려 준다 - 오류를 알리는 자리에서 또 오류가 나면 안 된다. */
  @Test
  void 없는_키는_키를_그대로_돌려_준다() {
    assertThat(StockViewSupport.msg("stock.no.such.key")).isEqualTo("stock.no.such.key");
  }

  /** 메시지 소스가 없어도(단위 테스트 · 초기화 전) 죽지 않는다. */
  @Test
  void 메시지_소스가_없어도_죽지_않는다() {
    MessageUtil.setMessageSourceAccessor(null);

    assertThat(StockViewSupport.msg(KEY, 1, "a", "b")).isEqualTo(KEY);
  }
}
