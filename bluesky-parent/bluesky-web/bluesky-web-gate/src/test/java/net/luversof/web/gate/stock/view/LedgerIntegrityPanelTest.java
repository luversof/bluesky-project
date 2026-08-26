package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse;

/**
 * 관리 화면이 원장의 산술 모순을 드러내는지 본다.
 *
 * <p>이 앱의 원장은 증권사 화면을 사람이 옮겨 담은 것이라 잘못된 값이 실제로 들어와 있다(실측 2026-08-22: 배당 193 건 중 8 건이 세금 &gt; 과세표준
 * &mdash; KODEX 한국부동산리츠인프라, 과세표준이 77 주 기준인데 기록 수량은 10,256 주). 화면은 그 값을 그대로 더할 뿐이라 여기서 드러내지 않으면 잘못된
 * 값이 계속 합계에 섞인다.
 *
 * <p>"이상 0 건"과 "검사가 못 돌았다"는 반드시 구분돼야 한다. 둘을 같은 모양으로 그리면 사용자는 확인되지 않은 상태를 안전하다고 읽는다.
 */
class LedgerIntegrityPanelTest {

  // 렌더된 HTML 에서 숫자를 찾을 때는 <b>자릿수가 짧을수록 위험하다</b>.
  //
  // 실측 2026-08-23: contains("8") 로 발견 건수를 확인하던 검사는 건수를 5 로 바꿔도 통과했다. 한두 자리
  // 숫자는 날짜·클래스명 어디에나 있기 때문이다. 같은 이유로 contains("7")(어긋난 종목 수)도 무의미했다.
  // 그런 값은 data-* 속성을 붙여 그 자리를 직접 본다.
  //
  // 반대로 여섯 자리 금액·행수(57,459 / 1,352 / 297424)와 날짜(2026-08-19), 세 자리 수량(857 / 879)은
  // 표본으로 확인한 결과 픽스처를 바꾸면 모두 검출됐다 - 그쪽은 그대로 둔다.

  private static final String TEMPLATE = "stock/htmx/fragments/adminActions.jte";

  /** 서버가 내는 규칙 코드. 하나라도 문구가 빠지면 화면에 빈 칸이 나간다. */
  private static final List<String> RULE_CODES =
      List.of(
          "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
          "DIVIDEND_PER_SHARE_MISMATCH",
          "DIVIDEND_NEGATIVE_AMOUNT",
          "DIVIDEND_QUANTITY_NOT_POSITIVE",
          "DIVIDEND_WITHOUT_TRADE",
          "DIVIDEND_TAX_IN_TAX_DEFERRED_ACCOUNT",
          "TRADE_NEGATIVE_FEE_OR_TAX",
          "TRADE_BUY_WITH_TAX",
          "TRADE_SELL_WITHOUT_REALIZED_PROFIT",
          "TRADE_BUY_WITH_REALIZED_PROFIT",
          "TRADE_QUANTITY_NOT_POSITIVE",
          "TRADE_NEGATIVE_PRICE");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String render(LedgerIntegrityResponse ledgerIntegrity) {
    Map<String, Object> params = new HashMap<>();
    params.put("isAuthenticated", true);
    params.put("ledgerIntegrity", ledgerIntegrity);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  @Test
  void 발견이_있으면_규칙_문구와_건수와_예시를_적는다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            0L,
            List.of(),
            List.of(),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
                    8,
                    List.of(
                        new LedgerIntegrityResponse.Example(
                            "2026-08-19", "KODEX 한국부동산리츠인프라", "taxable=2000, gross=1000")))));

    String html = render(response);

    assertThat(html).containsPattern("(Taxable amount exceeds|과세표준이 세전)");
    // "8" 만 찾으면 날짜·클래스명 어디에나 걸려 항상 통과한다(실측: 건수를 5로 바꿔도 통과했다).
    assertThat(html).as("건수가 없으면 한 건인지 여덟 건인지 알 수 없다").contains("data-ledger-count=\"8\"");
    assertThat(html)
        .as("원장에서 그 줄을 찾아갈 단서가 없다")
        .contains("2026-08-19")
        .contains("KODEX 한국부동산리츠인프라")
        .contains("taxable=2000, gross=1000");
  }

  /** 검사가 돌아서 깨끗한 것과, 검사 자체가 못 돈 것은 다른 상태다. */
  @Test
  void 이상이_없을_때와_검사가_못_돌았을_때를_구분한다() {
    String clean =
        render(new LedgerIntegrityResponse(193L, 250L, 0L, List.of(), List.of(), List.of()));
    assertThat(clean).containsPattern("(No arithmetic problems found|이상한 기록 없음)");
    assertThat(clean).as("검사한 건수를 밝히지 않으면 안심할 근거가 못 된다").contains("193").contains("250");

    String unavailable = render(null);
    assertThat(unavailable).containsPattern("(could not run|실행하지 못했습니다)");
    assertThat(unavailable).doesNotContain("No arithmetic problems found");
  }

  @Test
  void 모든_규칙_코드에_두_로케일_문구가_있다() throws IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      String source =
          Files.readString(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8);
      for (String code : RULE_CODES) {
        String key = "stock.admin.ledger.rule." + code;
        assertThat(source).as(bundle + " 에 " + key + " 이 없다").contains(key + " ");
      }
      for (String key :
          List.of(
              "stock.admin.ledger.title",
              "stock.admin.ledger.clean",
              "stock.admin.ledger.checked",
              "stock.admin.ledger.unavailable")) {
        assertThat(source).as(bundle + " 에 " + key + " 이 없다").contains(key + " ");
      }
    }
  }

  /** 키가 없으면 MessageUtil 이 빈 문자열을 돌려주므로 화면에 빈 칸만 남는다. */
  @Test
  void 규칙_문구가_실제로_비어_있지_않다() {
    for (String code : RULE_CODES) {
      String rendered =
          render(
              new LedgerIntegrityResponse(
                  1L,
                  1L,
                  1L,
                  List.of(),
                  List.of(),
                  List.of(new LedgerIntegrityResponse.Finding(code, 1, List.of()))));
      assertThat(rendered)
          .as(code + " 의 문구가 화면에 나오지 않는다")
          .doesNotContain("stock.admin.ledger.rule." + code);
    }
  }

  private LedgerIntegrityResponse withExamples(int count, int total) {
    List<LedgerIntegrityResponse.Example> examples = new java.util.ArrayList<>();
    for (int index = 0; index < count; index++) {
      examples.add(
          new LedgerIntegrityResponse.Example(
              "2026-08-" + String.format("%02d", index + 1), "종목" + index, "상세" + index));
    }
    return new LedgerIntegrityResponse(
        193L,
        250L,
        0L,
        List.of(),
        List.of(),
        List.of(new LedgerIntegrityResponse.Finding("SELL_WITHOUT_FEE_AND_TAX", total, examples)));
  }

  /**
   * 예시가 3건을 넘으면 나머지를 접어서 <b>보여 준다</b>.
   *
   * <p>실측 2026-08-23: 예시를 3건만 받던 시절 발견 45건 중 25건(55%)이 화면에 아예 없었다. 어느 행인지 모르면 고칠 수 없다.
   */
  @Test
  void 예시가_많으면_나머지를_접어서_보여_준다() {
    String html = render(withExamples(12, 12));

    assertThat(html).as("앞 3건은 펼쳐 둔다").contains("종목0").contains("종목1").contains("종목2");
    assertThat(html).as("나머지도 화면에 있어야 한다").contains("종목11");
    assertThat(html).as("길어지지 않도록 접는다").contains("<details");
    assertThat(html).as("몇 건이 접혀 있는지 알려야 한다").contains("data-ledger-collapsed=\"9\"");
  }

  @Test
  void 예시가_3건_이하면_접이식을_만들지_않는다() {
    String html = render(withExamples(3, 3));

    assertThat(html).contains("종목2");
    assertThat(html).doesNotContain("data-ledger-collapsed");
  }

  /** 상한에 걸려 아예 받아오지 못한 건수가 있으면 그 사실을 밝힌다. */
  @Test
  void 받아오지_못한_건수가_있으면_밝힌다() {
    String html = render(withExamples(20, 45));

    assertThat(html).contains("data-ledger-truncated=\"25\"");
    assertThat(html).as("접힌 것과 아예 못 받아온 것은 다른 얘기다").contains("data-ledger-collapsed=\"17\"");
  }

  /**
   * 한 행이 여러 규칙에 걸리면 그 사실을 밝힌다.
   *
   * <p>실측 2026-08-23: 발견 45 건이 실제로는 29 개 행이었다 &mdash; KODEX 한국부동산리츠인프라 배당 8 건이 각각 2~4 개 규칙에 걸려 16
   * 건이 중복이었다. 건수만 보면 할 일이 실제보다 커 보인다.
   */
  @Test
  void 규칙_중복이_있으면_서로_다른_행_수를_밝힌다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            8L,
            List.of(),
            List.of(),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
                    8,
                    List.of(new LedgerIntegrityResponse.Example("2026-08-19", "종목", "상세"))),
                new LedgerIntegrityResponse.Finding(
                    "DIVIDEND_TAXABLE_COMPUTED_WITH_OTHER_QUANTITY",
                    8,
                    List.of(new LedgerIntegrityResponse.Example("2026-08-19", "종목", "상세")))));

    assertThat(response.totalFindingCount()).isEqualTo(16);
    assertThat(response.hasOverlappingRows()).isTrue();
    assertThat(render(response)).contains("data-ledger-distinct=\"8\"");
  }

  @Test
  void 중복이_없으면_그_안내를_내지_않는다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            2L,
            List.of(),
            List.of(),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "DIVIDEND_WITHOUT_TRADE",
                    2,
                    List.of(new LedgerIntegrityResponse.Example("2020-04-08", "종목", "상세")))));

    assertThat(response.hasOverlappingRows()).isFalse();
    assertThat(render(response)).doesNotContain("data-ledger-distinct");
  }

  /**
   * 계좌별 집계를 보여 준다.
   *
   * <p>실측 2026-08-23: 발견 45 건이 계좌 3 개로 깨끗하게 갈렸다 &mdash; KB증권 위탁 24 · 동양증권 12 · 한국투자증권 위탁 7. 계좌를 모르면
   * 45 건이 뒤섞여 보여 어디부터 손대야 할지 알 수 없다.
   */
  /**
   * 한 행에 사유가 여러 개면 행으로 묶어 먼저 보여 준다.
   *
   * <p>실측 2026-08-23: 발견 48 건이 30 행이고 그중 10 행이 사유를 2~4 개씩 달고 있었다. KODEX 한국부동산리츠인프라 배당 8 행이 전부 여기
   * 속하는데 원인은 하나다 &mdash; 과세표준을 옛 수량 77 주로 잡은 것. 규칙별로만 나열하면 사용자가 같은 행을 규칙 그룹마다 다시 만난다.
   */
  @Test
  void 사유가_여러_개인_행을_묶어_보여_준다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            30L,
            List.of(),
            List.of(
                new LedgerIntegrityResponse.RowFindingSummary(
                    "2026-07-20",
                    "KODEX 한국부동산리츠인프라",
                    "KB증권 위탁",
                    List.of(
                        "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
                        "DIVIDEND_PER_SHARE_MISMATCH",
                        "DIVIDEND_QUANTITY_NOT_POSITIVE",
                        "DIVIDEND_NEGATIVE_AMOUNT"))),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
                    8,
                    List.of(
                        new LedgerIntegrityResponse.Example(
                            "2026-07-20", "KODEX 한국부동산리츠인프라", "taxable=2000, gross=1000")))));

    String html = render(response);
    assertThat(html).as("행 묶음 자체가 없다").contains("data-ledger-multi-rows=\"1\"");
    assertThat(html).as("그 행에 사유가 몇 개인지 없다").contains("data-ledger-row-codes=\"4\"");
    assertThat(html).contains("2026-07-20").contains("KB증권 위탁");
    // 코드가 아니라 로케일 문구로 그려야 한다.
    assertThat(html).doesNotContain("DIVIDEND_TAXABLE_EXCEEDS_GROSS");
  }

  /** 겹치는 행이 없으면 이 묶음을 내지 않는다. 항상 켜져 있으면 자리만 차지한다. */
  @Test
  void 겹치는_행이_없으면_묶음을_내지_않는다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            2L,
            List.of(),
            List.of(),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "SELL_WITHOUT_FEE_AND_TAX",
                    2,
                    List.of(new LedgerIntegrityResponse.Example("2020-01-28", "카카오", "상세")))));

    assertThat(render(response)).doesNotContain("data-ledger-multi-rows");
  }

  @Test
  void 계좌별_집계를_보여_준다() {
    LedgerIntegrityResponse response =
        new LedgerIntegrityResponse(
            193L,
            250L,
            29L,
            List.of(
                new LedgerIntegrityResponse.AccountFindingSummary("KB증권 위탁", 24L, 8L),
                new LedgerIntegrityResponse.AccountFindingSummary("동양증권", 12L, 12L),
                new LedgerIntegrityResponse.AccountFindingSummary("한국투자증권 위탁", 7L, 7L)),
            List.of(),
            List.of(
                new LedgerIntegrityResponse.Finding(
                    "SELL_WITHOUT_FEE_AND_TAX",
                    12,
                    List.of(new LedgerIntegrityResponse.Example("2020-01-28", "카카오", "상세")))));

    String html = render(response);
    assertThat(html).contains("data-ledger-accounts=\"3\"");
    assertThat(html).contains("KB증권 위탁 24").contains("동양증권 12").contains("한국투자증권 위탁 7");
  }

  @Test
  void 계좌_집계가_없으면_그_줄을_내지_않는다() {
    assertThat(render(withExamples(2, 2))).doesNotContain("data-ledger-accounts");
  }
}
