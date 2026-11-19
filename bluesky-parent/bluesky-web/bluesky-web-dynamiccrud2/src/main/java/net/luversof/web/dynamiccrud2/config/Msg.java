package net.luversof.web.dynamiccrud2.config;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 화면 · 오류 문구(다국어). {@code messages.properties}(한국어, 기본) · {@code messages_en.properties}. 템플릿은
 * {@code @import static net.luversof.web.dynamiccrud2.config.Msg.t} 후 {@code ${t("key")}}.
 *
 * <p>요청의 언어는 bluesky-boot 로케일 처리(쿠키 → Accept-Language)가 정하고 DispatcherServlet 이 LocaleContextHolder
 * 에 둔다. 요청 밖(단위 테스트 · 배치)은 한국어. Spring 빈이 아니라 자체 MessageSource 를 들어 컨텍스트 없이도 쓴다.
 *
 * <p>인자가 있는 문구는 MessageFormat 이라 작은따옴표를 두 번('') 써야 한다 - 영어 문구는 축약형(can't)을 쓰지 않는다.
 */
public final class Msg {

  private static final ResourceBundleMessageSource SOURCE = new ResourceBundleMessageSource();

  static {
    SOURCE.setBasename("messages");
    SOURCE.setDefaultEncoding("UTF-8");
    // 서버 기본 로케일(en 일 수 있다)로 새지 않고 messages.properties(한국어)로
    SOURCE.setFallbackToSystemLocale(false);
  }

  private Msg() {}

  /** 지금 요청 언어의 문구. 없는 key 면 NoSuchMessageException(문구 빠짐을 테스트 · 화면에서 바로 알게) */
  public static String t(String code, Object... args) {
    return SOURCE.getMessage(code, args, locale());
  }

  /** 지금 요청 언어(요청 밖이면 한국어) */
  public static Locale locale() {
    return LocaleContextHolder.getLocaleContext() == null
        ? Locale.KOREAN
        : LocaleContextHolder.getLocale();
  }
}
