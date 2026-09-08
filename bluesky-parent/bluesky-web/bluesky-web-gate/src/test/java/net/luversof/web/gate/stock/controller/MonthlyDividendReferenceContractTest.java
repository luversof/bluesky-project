package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;

/**
 * 월배당 기준 관리 화면이 <b>바깥에 약속한 것</b>을 고정한다.
 *
 * <p>이 구역은 폼 아홉 개가 저장·삭제·가져오기를 하는데, 컨트롤러 층 검사가 지급이력 폼 검증 넷뿐이었다. 나머지 &mdash; 종목코드를 어떻게 다듬는지, 무엇을
 * 거절하는지, 성공하면 어디로 되돌리는지, 실패 문구를 어디서 가져오는지 &mdash; 는 소스를 읽어야만 알 수 있었다.
 *
 * <p>이 파일은 <b>컨트롤러를 쪼개기 전에</b> 그 약속들을 못 박으려고 쓴다. 옮긴 뒤 같은 검사가 그대로 통과해야 "옮기기만 했다" 를 말할 수 있다.
 */
class MonthlyDividendReferenceContractTest {

  // 문구가 메시지 키로 옮겨가서(2026-09-08) 검사도 번들을 켜야 한다. 아래 단정은 한국어 문구를 본다.
  @org.junit.jupiter.api.BeforeAll
  static void primeMessages() {
    var source = new org.springframework.context.support.ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    io.github.luversof.boot.context.support.MessageUtil.setMessageSourceAccessor(
        new org.springframework.context.support.MessageSourceAccessor(source));
    org.springframework.context.i18n.LocaleContextHolder.setDefaultLocale(java.util.Locale.KOREAN);
  }

  @org.junit.jupiter.api.AfterAll
  static void clearMessages() {
    io.github.luversof.boot.context.support.MessageUtil.setMessageSourceAccessor(null);
    org.springframework.context.i18n.LocaleContextHolder.setDefaultLocale(null);
  }

  private static final String KNOWN_SYMBOL = "005930";

  /** 심볼 검증이 종목 목록과 월배당 프로필을 훑는다. 그 조회만 대신한다. */
  private MonthlyDividendReferenceSupport supportWithKnownSymbol(String symbol) {
    var support = new MonthlyDividendReferenceSupport();
    support.setMonthlyDividendViewSupport(new MonthlyDividendViewSupport());
    support.setStockItemClient(
        new StockItemClient() {
          @Override
          public List<StockItem> getStockItems() {
            return List.of(new StockItem(UUID.randomUUID(), symbol, "삼성전자", "KOSPI", List.of()));
          }

          @Override
          public Optional<StockItem> getStockItemById(UUID id) {
            return Optional.empty();
          }

          @Override
          public StockItem findByName(String name) {
            return null;
          }

          @Override
          public List<StockItem> getStockItemsByTag(String tag) {
            return List.of();
          }

          @Override
          public List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint>
              getPriceHistory(
                  UUID id, org.springframework.util.MultiValueMap<String, String> params) {
            return List.of();
          }

          @Override
          public StockItem createStockItem(StockItem stockItem) {
            return stockItem;
          }
        });
    support.setMonthlyDividendProfileClient(
        new net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient() {
          @Override
          public List<net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse>
              findProfiles(org.springframework.util.MultiValueMap<String, String> request) {
            return List.of();
          }

          @Override
          public net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse
              upsertProfile(
                  net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest
                      request) {
            return null;
          }

          @Override
          public void reorderProfiles(
              net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileReorderRequest
                  request) {
            // 이 검사에서는 쓰지 않는다
          }

          @Override
          public void deleteProfile(String symbol2) {
            // 이 검사에서는 쓰지 않는다
          }
        });
    return support;
  }

  private StockDividendViewController controller() {
    var created = new StockDividendViewController();
    created.setMonthlyDividendReferenceSupport(supportWithKnownSymbol(KNOWN_SYMBOL));
    created.setMonthlyDividendViewSupport(new MonthlyDividendViewSupport());
    return created;
  }

  private Object call(String name, Class<?>[] types, Object... args) throws Exception {
    Method method = StockDividendViewController.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      return method.invoke(controller(), args);
    } catch (InvocationTargetException ex) {
      throw (Exception) ex.getCause();
    }
  }

  // 두 메서드는 MonthlyDividendReferenceSupport 로 옮겨 public 이 됐다 - 리플렉션이 필요 없다.
  private String normalize(String symbol) {
    return supportWithKnownSymbol(KNOWN_SYMBOL).normalizeMonthlyDividendSymbol(symbol);
  }

  private void validate(String symbol) {
    supportWithKnownSymbol(KNOWN_SYMBOL).validateMonthlyDividendSymbol(symbol);
  }

  /** 종목코드는 다듬어서 쓴다. 사용자가 소문자로 넣거나 공백을 흘려도 같은 종목이어야 한다. */
  @Test
  void 종목코드는_공백을_떼고_대문자로_맞춘다() {
    assertThat(normalize("  005930  ")).isEqualTo("005930");
    assertThat(normalize("tiger")).isEqualTo("TIGER");
  }

  /** 빈 값은 null 이다. 빈 문자열로 두면 아래 검증이 "값이 있다" 로 읽는다. */
  @Test
  void 빈_종목코드는_null_이_된다() {
    assertThat(normalize("   ")).isNull();
    assertThat(normalize(null)).isNull();
  }

  @Test
  void 등록된_종목코드는_통과한다() {
    validate(KNOWN_SYMBOL);
  }

  @Test
  void 종목코드가_비면_거절한다() {
    assertThatThrownBy(() -> validate("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("필수");
  }

  /**
   * URL 을 넣으면 <b>왜 안 되는지</b>까지 말해 준다.
   *
   * <p>출처 URL 칸과 종목코드 칸이 붙어 있어 실제로 섞여 들어왔다. "등록되지 않은 종목코드" 라고만 하면 어디를 고쳐야 할지 알 수 없다.
   */
  @Test
  void 종목코드_칸에_URL_을_넣으면_어디에_넣어야_하는지_알려준다() {
    assertThatThrownBy(() -> validate("https://www.example.com/etf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("출처 URL");
  }

  /** 종목 마스터에도 월배당 프로필에도 없는 코드는 거절한다. 오타로 새 종목이 생기면 되돌리기 어렵다. */
  @Test
  void 등록되지_않은_종목코드는_거절한다() {
    assertThatThrownBy(() -> validate("999999"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("등록되지 않은");
  }

  /**
   * 성공하면 되돌아가는 곳.
   *
   * <p>관리 화면의 월배당 기준 탭으로 돌아가되, 보고 있던 종목·정렬·날짜를 유지해야 한다. 유지하지 않으면 저장할 때마다 목록 맨 위로 튕긴다.
   */
  @Test
  void 저장_뒤에는_보던_자리로_되돌린다() throws Exception {
    String redirect =
        (String)
            call(
                "buildMonthlyDividendReferencePageRedirect",
                new Class<?>[] {
                  String.class, String.class, String.class, LocalDate.class, LocalDate.class
                },
                KNOWN_SYMBOL,
                null,
                null,
                LocalDate.parse("2026-08-19"),
                LocalDate.parse("2026-08-21"));

    assertThat(redirect)
        .as("관리 화면의 월배당 기준 탭으로 돌아가야 한다")
        .startsWith("redirect:/stock/admin?tab=")
        .as("보던 종목과 날짜를 잃으면 저장할 때마다 목록 맨 위로 튕긴다")
        .contains("symbol=" + KNOWN_SYMBOL)
        .contains("payoutRecordDate=2026-08-19")
        .contains("payoutPayDate=2026-08-21")
        .as("정렬은 비워 보내도 기본값이 실려야 화면이 같은 순서로 열린다")
        .contains("profileSort=")
        .contains("profileDirection=");
  }

  /** 값이 없는 항목은 주소에 붙이지 않는다. 빈 파라미터가 붙으면 화면이 '빈 값으로 걸러라' 로 읽는다. */
  @Test
  void 값이_없는_항목은_주소에_붙이지_않는다() throws Exception {
    String redirect =
        (String)
            call(
                "buildMonthlyDividendReferencePageRedirect",
                new Class<?>[] {
                  String.class, String.class, String.class, LocalDate.class, LocalDate.class
                },
                null,
                null,
                null,
                null,
                null);

    assertThat(redirect).doesNotContain("symbol=").doesNotContain("payoutRecordDate=");
  }

  /**
   * 실패 문구는 <b>원격이 준 것을 우선</b>한다.
   *
   * <p>api-stock 이 "이미 등록된 지급일입니다" 처럼 사람이 읽을 수 있는 사유를 주는데, 그것을 버리고 "저장하지 못했습니다" 로 덮으면 사용자가 무엇을 고쳐야
   * 할지 알 수 없다.
   */
  @Test
  void 실패_문구는_원격이_준_사유를_먼저_쓴다() {
    String fallback = "월배당 프로필을 저장하지 못했습니다.";

    // failureMessage 는 StockViewSupport 로 옮겨 public static 이 됐다.
    String plain =
        net.luversof.web.gate.stock.support.StockViewSupport.failureMessage(
            new IllegalStateException("boom"), fallback);

    assertThat(plain).as("사유를 알 수 없으면 기본 문구를 쓴다").isEqualTo(fallback);
  }
}
