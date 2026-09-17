package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.MultiValueMap;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport.TaxableRatioBasis;

/**
 * 시뮬레이터의 과세표준 비중 칸은 <b>무엇을 합친 값인지</b> 밝힌다.
 *
 * <p>이 비중은 api-stock 이 원장 최근 1 년의 (과세금액 합 / 세전 합)을 모든 계좌 합쳐 낸다(2026-08-24 결정). 과세이연 계좌(ISA · 연금저축)는
 * 과세금액이 0 으로 적힌다 &mdash; 실측 2026-09-17: PLUS 고배당주위클리고정커버드콜 · TIGER 코리아배당다우존스위클리커버드콜은 과세이연 계좌 세 곳에서만
 * 받아 <b>0%</b>, TIGER 리츠부동산인프라는 네 계좌 중 과세 계좌 한 곳만 100% 라 <b>13.42%</b>. 칸 이름은 "1 년 평균 과세 표준 비중" 이라
 * 종목의 성질로 읽혀 사용자가 "0 인데 실제는 100%" 라고 물었다.
 *
 * <p>사용자 결정(2026-09-17): 계산은 두고 표기만 바로잡는다 &mdash; 칸 이름에 '내 계좌', 칸 아래에 과세이연 계좌에서만 받았는지 또는 과세 계좌만의
 * 비중. 함께 적던 "지급 이력 기준 N%" 는 경고색과 "저장값을 갱신합니다" 링크를 달고 있었는데, 지급 이력을 가져올 때마다 비중을 원장 실적으로 다시 덮으므로 갱신해도
 * 그대로였다(막다른 안내) &mdash; 차이는 낡은 값이 아니라 기준 차이다.
 */
class MonthlyDividendTaxableRatioBasisTest {

  private static final UUID ISA = UUID.randomUUID();

  private static final UUID PENSION = UUID.randomUUID();

  private static final UUID BROKERAGE = UUID.randomUUID();

  private static final UUID PLUS = UUID.randomUUID();

  private static final UUID REIT = UUID.randomUUID();

  private static final UUID RISE = UUID.randomUUID();

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static DividendResponse dividend(UUID account, UUID item, String gross, String taxable) {
    return new DividendResponse(
        UUID.randomUUID(),
        account,
        item,
        "name",
        "DIVIDEND",
        1,
        null,
        null,
        new BigDecimal(gross),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        taxable != null ? new BigDecimal(taxable) : null,
        new BigDecimal(gross),
        Instant.parse("2026-08-01T00:00:00Z"),
        Instant.parse("2026-08-04T00:00:00Z"));
  }

  /** 2026-09-17 원장에서 옮긴 금액(계좌 이름만 바꿨다). */
  private static List<DividendResponse> ledger() {
    return List.of(
        dividend(ISA, PLUS, "37317", "0"),
        dividend(PENSION, PLUS, "6951", "0"),
        dividend(ISA, REIT, "3474042", "0"),
        dividend(PENSION, REIT, "688809", "0"),
        dividend(BROKERAGE, REIT, "765435", "765435"),
        dividend(BROKERAGE, RISE, "9167582", "392908"),
        dividend(BROKERAGE, null, "1000", "1000"));
  }

  @Test
  void 계좌를_과세이연과_그밖으로_나눠_더한다() {
    Map<UUID, TaxableRatioBasis> basis =
        MonthlyDividendReferenceSupport.taxableRatioBasis(ledger(), Set.of(ISA, PENSION));

    assertThat(basis).as("종목 없는 배당은 뺀다").containsOnlyKeys(PLUS, REIT, RISE);

    assertThat(basis.get(PLUS).deferredOnly()).isTrue();
    assertThat(basis.get(PLUS).mixed()).isFalse();
    assertThat(basis.get(PLUS).taxableAccountRatioPct()).isNull();

    assertThat(basis.get(REIT).deferredOnly()).isFalse();
    assertThat(basis.get(REIT).mixed()).isTrue();
    assertThat(basis.get(REIT).taxableAccountRatioPct()).isEqualByComparingTo("100.00");

    assertThat(basis.get(RISE).deferredOnly()).as("과세이연 몫이 없으면 설명할 것이 없다").isFalse();
    assertThat(basis.get(RISE).mixed()).isFalse();
    assertThat(basis.get(RISE).taxableAccountRatioPct()).isEqualByComparingTo("4.29");
  }

  @Test
  void 과세이연_표시는_계좌_설정에서_읽고_기간은_최근_365일이다() {
    AccountClient accounts = org.mockito.Mockito.mock(AccountClient.class);
    UUID user = UUID.randomUUID();
    org.mockito.Mockito.when(accounts.getAccountsByUserId(user))
        .thenReturn(
            List.of(
                new Account(ISA, user, "ISA", null, Map.of("isTaxDeferred", true)),
                new Account(PENSION, user, "pension", null, Map.of("isTaxDeferred", true)),
                new Account(BROKERAGE, user, "brokerage", null, null)));
    DividendClient dividends = org.mockito.Mockito.mock(DividendClient.class);
    org.mockito.ArgumentCaptor<MultiValueMap<String, String>> params =
        org.mockito.ArgumentCaptor.captor();
    org.mockito.Mockito.when(dividends.findDividends(params.capture())).thenReturn(ledger());

    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setAccountClient(accounts);
    support.setDividendClient(dividends);
    Map<UUID, TaxableRatioBasis> basis = support.loadTaxableRatioBasis(user);

    assertThat(basis.get(PLUS).deferredOnly()).isTrue();
    assertThat(basis.get(REIT).taxableAccountRatioPct()).isEqualByComparingTo("100.00");
    assertThat(params.getValue().getFirst("userId")).isEqualTo(user.toString());
    Instant start = Instant.parse(params.getValue().getFirst("startDate"));
    Instant expected = Instant.now().minus(java.time.Duration.ofDays(365));
    assertThat(java.time.Duration.between(start, expected).abs())
        .as("api-stock 의 비중 계산과 같은 기간이라야 나눔이 그 비중을 설명한다")
        .isLessThan(java.time.Duration.ofMinutes(1));
    assertThat(params.getValue().containsKey("endDate")).as("끝은 열어 둔다(api-stock 과 같다)").isFalse();
  }

  @Test
  void 조회에_실패하면_나눔을_지어내지_않는다() {
    AccountClient accounts = org.mockito.Mockito.mock(AccountClient.class);
    org.mockito.Mockito.when(accounts.getAccountsByUserId(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("api-stock down"));
    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setAccountClient(accounts);
    support.setDividendClient(org.mockito.Mockito.mock(DividendClient.class));

    assertThat(support.loadTaxableRatioBasis(UUID.randomUUID())).isEmpty();
  }

  private static MonthlyDividendSnapshotResponse row(UUID item, String symbol, String ratio) {
    BigDecimal ten = BigDecimal.TEN;
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        null,
        item,
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-09-02"),
        ten,
        ten,
        new BigDecimal(ratio),
        100,
        ten,
        ten,
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        new BigDecimal("100"),
        BigDecimal.ONE,
        BigDecimal.ONE,
        null);
  }

  private static String cellOf(String html, String symbol) {
    int at = html.indexOf("data-symbol=\"" + symbol + "\"");
    assertThat(at).as(symbol + " 행을 못 찾았다").isGreaterThan(0);
    return html.substring(at, html.indexOf("</tr>", at));
  }

  @Test
  void 표가_합친_기준과_나눔을_밝힌다() {
    List<MonthlyDividendSnapshotResponse> rows =
        List.of(row(PLUS, "PLUS", "0"), row(REIT, "REIT", "13.42"), row(RISE, "RISE", "4.29"));
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendRows", rows);
    model.put(
        "monthlyDividendSummary", new MonthlyDividendCalculator().buildSimulatorSummary(rows));
    model.put(
        "monthlyDividendTaxableRatioBasis",
        MonthlyDividendReferenceSupport.taxableRatioBasis(ledger(), Set.of(ISA, PENSION)));
    model.put(
        "monthlyDividendReferenceTaxableRatios",
        Map.of(PLUS, new BigDecimal("25.05"), REIT, new BigDecimal("100")));
    model.put("monthlyDividendHasSavedRows", true);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendSimulator.jte", model, output);
    String html = output.toString();

    assertThat(html)
        .as("칸 이름이 종목의 성질이 아니라 내 계좌 실적임을 말한다")
        .contains(
            MessageUtil.getMessage(
                "stock.simulator.monthly.table.header.taxable.base.ratio.line.one"))
        .contains("내 계좌");

    String plus = cellOf(html, "PLUS");
    assertThat(plus)
        .contains("data-taxable-ratio-basis=\"deferred-only\"")
        .contains(
            MessageUtil.getMessage(
                "stock.simulator.monthly.table.cell.taxable.ratio.deferred.only"));

    String reit = cellOf(html, "REIT");
    assertThat(reit)
        .contains("data-taxable-ratio-basis=\"taxable-accounts\"")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage(
                    "stock.simulator.monthly.table.cell.taxable.ratio.taxable.accounts"),
                "100.00"));

    assertThat(cellOf(html, "RISE"))
        .as("과세이연 몫이 없으면 덧붙일 말이 없다")
        .doesNotContain("data-taxable-ratio-basis");

    int ref = plus.indexOf("data-reference-taxable-ratio");
    assertThat(ref).as("지급 이력 기준 차이 표기는 그대로 있다").isGreaterThan(0);
    String refDiv = plus.substring(plus.lastIndexOf("<div", ref), ref);
    assertThat(refDiv).as("기준이 다를 뿐 경고할 일이 아니다").doesNotContain("text-warning");
    assertThat(
            MessageUtil.getMessage(
                "stock.simulator.monthly.table.cell.taxable.ratio.reference.link"))
        .as("갱신해도 그대로라 '갱신' 을 약속하지 않는다")
        .doesNotContain("갱신");
  }

  private static String flatten(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
  }

  /** 모델 이름 · 페이지 전달이 끊기면 조각이 기본값(빈 맵)을 받아 설명이 통째로 사라진다. */
  @Test
  void 컨트롤러와_페이지가_나눔을_조각까지_넘긴다() throws IOException {
    assertThat(
            flatten(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"))
        .contains("monthlyDividendReferenceSupport.loadTaxableRatioBasis(userId)")
        .contains(
            "\"monthlyDividendTaxableRatioBasis\", net.luversof.web.gate.stock.support.StockAsyncSupport.join(taxableRatioBasisFuture)");
    assertThat(flatten("src/main/jte/stock/simulator.jte"))
        .contains("monthlyDividendTaxableRatioBasis = monthlyDividendTaxableRatioBasis,");
  }
}
