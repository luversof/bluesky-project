package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;

/**
 * 비동기로 던진 작업도 요청의 로케일로 메시지를 풀어야 한다.
 *
 * <p>{@code LocaleContextHolder} 는 ThreadLocal 이라 다른 스레드에서는 비어 있다. 실측 2026-09-11: 영어 화면의 자산성장 매매 이력
 * 제목이 "Trade History <b>전체</b>" 로 나왔다. 같은 값을 요청 스레드에서 만드는 {@code /stock/htmx/trade-history} 는
 * "Trade History All" 이었다. 기간을 지정하면 메시지를 안 타서 정상이라, 기간이 '전체' 일 때만 드러났다.
 */
class LocaleContextExecutorServiceTest {

  private final ExecutorService raw = Executors.newVirtualThreadPerTaskExecutor();

  private final ExecutorService wrapped = new LocaleContextExecutorService(raw);

  /** submit 은 Callable/Runnable 로 모호해진다 - 타입을 못박아 둔다. */
  private static final Callable<Locale> CURRENT_LOCALE = LocaleContextHolder::getLocale;

  @AfterEach
  void clear() {
    LocaleContextHolder.resetLocaleContext();
    raw.shutdown();
  }

  @Test
  void 감싸지_않으면_로케일이_사라진다() throws Exception {
    LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(Locale.ENGLISH));

    Locale seen = raw.submit(CURRENT_LOCALE).get();

    assertThat(seen).as("이 전제가 깨지면 이 감싸개는 필요 없다").isNotEqualTo(Locale.ENGLISH);
  }

  @Test
  void 작업_스레드가_요청_로케일을_본다() throws Exception {
    LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(Locale.ENGLISH));

    assertThat(wrapped.submit(CURRENT_LOCALE).get()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void 실행_경로마다_모두_실어_보낸다() throws Exception {
    LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(Locale.FRANCE));

    var viaSubmit = wrapped.submit(CURRENT_LOCALE).get();
    var viaInvokeAll = wrapped.invokeAll(List.of(CURRENT_LOCALE)).get(0).get();
    var viaInvokeAny = wrapped.invokeAny(List.of(CURRENT_LOCALE));

    assertThat(viaSubmit).isEqualTo(Locale.FRANCE);
    assertThat(viaInvokeAll).isEqualTo(Locale.FRANCE);
    assertThat(viaInvokeAny).isEqualTo(Locale.FRANCE);
  }

  @Test
  void 작업이_끝나면_스레드의_로케일을_되돌린다() throws Exception {
    LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(Locale.ENGLISH));
    wrapped.submit((Callable<Locale>) () -> null).get();

    LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(Locale.KOREA));
    assertThat(wrapped.submit(CURRENT_LOCALE).get())
        .as("가상 스레드는 매번 새로 뜨지만 풀 스레드로 바뀌어도 남지 않아야 한다")
        .isEqualTo(Locale.KOREA);
  }

  @Test
  void 실행기가_로케일_감싸개를_거친다() throws Exception {
    String config =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/config/GateStockConfig.java"),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(config)
        .as("보안 컨텍스트만 실어 보내면 메시지가 기본 로케일로 풀린다")
        .contains("new LocaleContextExecutorService(");
    assertThat(config).contains("DelegatingSecurityContextExecutorService");
  }
}
