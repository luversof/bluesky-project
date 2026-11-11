package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;

/**
 * 월배당 출처 가져오기가 부르는 곳은 https 운용사 사이트뿐이고, 로그인 사용자 토큰을 싣지 않는다(2026-10-27 감사).
 *
 * <p>감사 전에는 (1) 공용 빌더의 OAuth 인터셉터가 운용사 사이트로 가는 요청에도 {@code Authorization: Bearer} 를 붙였고, (2) 호스트를
 * {@code contains} 로 봐서 {@code timeetf.co.kr.attacker.example} 같은 주소로도 요청이 나갔다 - 둘이 겹치면 토큰이 남의 서버로
 * 간다.
 */
class MonthlyDividendSourceSecurityTest {

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

  /** 공용 빌더처럼 사용자 토큰을 붙이는 빌더 + 가짜 서버. 서비스는 이 빌더로 제 RestClient 를 만든다. */
  private static RestClient.Builder tokenAddingBuilder() {
    return RestClient.builder()
        .requestInterceptor(
            (request, body, execution) -> {
              request.getHeaders().setBearerAuth("user-access-token");
              return execution.execute(request, body);
            });
  }

  private static MonthlyDividendPayoutSourceImportService service(RestClient.Builder builder) {
    return new MonthlyDividendPayoutSourceImportService(
        builder, null, null, null, null, null, null, null, null, null);
  }

  @Test
  void 호스트는_그_도메인이거나_하위_도메인일_때만_맞다() {
    assertThat(PayoutSourceText.hostIs("timeetf.co.kr", "timeetf.co.kr")).isTrue();
    assertThat(PayoutSourceText.hostIs("WWW.TimeETF.co.kr", "timeetf.co.kr")).isTrue();
    assertThat(PayoutSourceText.hostIs("timeetf.co.kr.attacker.example", "timeetf.co.kr"))
        .as("도메인을 앞에 붙인 남의 호스트")
        .isFalse();
    assertThat(PayoutSourceText.hostIs("eviltimeetf.co.kr", "timeetf.co.kr"))
        .as("점 없이 이어 붙인 호스트")
        .isFalse();
    assertThat(PayoutSourceText.hostIs(null, "timeetf.co.kr")).isFalse();
    assertThat(TimeMonthlyDividendPayoutSourceParser.supportsHost("timeetf.co.kr.attacker.example"))
        .isFalse();
    assertThat(
            SolMonthlyDividendPayoutSourceParser.supportsHost("www.soletf.co.kr.10.0.0.5.nip.io"))
        .isFalse();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("kbam.co.kr.attacker.example"))
        .isFalse();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("www.riseetf.co.kr")).isTrue();
  }

  /** 저장하는 출처 주소는 http · https 만 - 화면이 링크로 건다. */
  @Test
  void 출처_주소는_웹_주소만_저장한다() {
    assertThat(MonthlyDividendReferenceSupport.isWebUrl("https://www.timeetf.co.kr/m11_view.php"))
        .isTrue();
    assertThat(MonthlyDividendReferenceSupport.isWebUrl(" HTTP://www.plusetf.co.kr/x")).isTrue();
    assertThat(MonthlyDividendReferenceSupport.isWebUrl("javascript:alert(1)")).isFalse();
    assertThat(MonthlyDividendReferenceSupport.isWebUrl("JavaScript://%0aalert(1)")).isFalse();
    assertThat(MonthlyDividendReferenceSupport.isWebUrl("data:text/html,x")).isFalse();
    assertThat(MonthlyDividendReferenceSupport.isWebUrl(null)).isFalse();
  }

  @Test
  void 운용사로_가는_요청에는_사용자_토큰이_실리지_않는다() {
    RestClient.Builder builder = tokenAddingBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://www.timeetf.co.kr/m11_view.php"))
        .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
        .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

    RestClient client = (RestClient) ReflectionTestUtils.getField(service(builder), "restClient");
    client.get().uri("https://www.timeetf.co.kr/m11_view.php").retrieve().body(String.class);

    server.verify();
  }

  @Test
  void https_가_아니거나_남의_호스트면_부르지_않는다() {
    RestClient.Builder builder = tokenAddingBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    MonthlyDividendPayoutSourceImportService service = service(builder);

    assertThatThrownBy(() -> service.fetchImport("0000A0", "http://www.timeetf.co.kr/m11_view.php"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.fetchImport("0000A0", "file:///etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> service.fetchImport("0000A0", "https://timeetf.co.kr.attacker.example/x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.fetchMeta(URI.create("http://www.plusetf.co.kr/x")))
        .isInstanceOf(IllegalArgumentException.class);

    // 기대한 요청이 없다 - 하나라도 나갔으면 가짜 서버가 "No further requests expected" 로 실패시킨다.
    server.verify();
  }
}
