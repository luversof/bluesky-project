package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;

/**
 * 출처 사이트에서 가져올 때는 <b>잘못된 행만 건너뛴다</b>.
 *
 * <p>실측 2026-09-17: RISE 44J2(코리아밸류업위클리고정커버드콜) 표의 지급기준일 2026-05-15 행 실지급일이 <b>2025-05-19</b>(연도
 * 오타)였고, 그 한 줄 때문에 가져오기가 통째로 실패했다 &mdash; 사이트가 정정할 때까지 가져올 때마다 난다. 사용자 결정(2026-09-17): 잘못된 행만 건너뛰고
 * 결과에 밝힌다(설정 저장 없이). 저장된 이력에는 그 행의 올바른 값(2026-05-19)이 이미 있었다 &mdash; 저장은 행마다 덮어쓰기라 건너뛴 행이 이력을 지우지
 * 않는다.
 *
 * <p>사람이 붙여넣는 가져오기는 그대로 엄격하다 &mdash; 붙여넣은 사람은 그 자리에서 고칠 수 있다({@code
 * MonthlyDividendPayoutImportParserTest} 의 거절 검사가 그 약속이다).
 */
class MonthlyDividendSourceImportSkipTest {

  private final MonthlyDividendPayoutImportParser parser = new MonthlyDividendPayoutImportParser();

  /** 2026-09-17 RISE 44J2 표의 앞 다섯 행 그대로 - 둘째 행이 오타다. */
  private static final String RISE_TABLE =
      String.join(
          "\n",
          "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)",
          "2026-06-15\t2026-06-17\t420\t7",
          "2026-05-15\t2025-05-19\t390\t8",
          "2026-04-15\t2026-04-17\t340\t54",
          "2026-03-13\t2026-03-17\t400\t1",
          "2026-02-13\t2026-02-20\t310\t1");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
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
  void 출처_가져오기는_잘못된_행만_건너뛴다() {
    MonthlyDividendPayoutImportParser.LenientParseResult result =
        parser.parseLenient("0094M0", RISE_TABLE);

    assertThat(result.requests())
        .extracting(MonthlyDividendPayoutUpsertRequest::getRecordDate)
        .containsExactly(
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 4, 15),
            LocalDate.of(2026, 3, 13),
            LocalDate.of(2026, 2, 13));
    assertThat(result.skipped()).hasSize(1);
    assertThat(result.skipped().get(0).row()).as("출처 표의 둘째 행(머리글 제외)").isEqualTo(2);
    assertThat(result.skipped().get(0).reason())
        .as("사이트에서 그 행을 찾을 수 있게 두 날짜를 그대로 적는다")
        .contains("2025-05-19")
        .contains("2026-05-15")
        .startsWith("2");
  }

  @Test
  void 붙여넣기_가져오기는_그대로_엄격하다() {
    assertThatThrownBy(() -> parser.parse("0094M0", RISE_TABLE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2025-05-19");
  }

  /** 저장 단계가 거절할 값을 여기서 거르지 않으면, 앞쪽 행만 저장된 채 가져오기가 중간에 멈춘다. */
  @Test
  void 저장이_거절할_값도_건너뛴다() {
    MonthlyDividendPayoutImportParser.LenientParseResult result =
        parser.parseLenient(
            "0094M0",
            String.join(
                "\n",
                "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)",
                "2026-06-15\t2026-06-17\t420\t7",
                "2026-05-15\t2026-05-19\t390\t391",
                "2026-04-15\t2026-04-17\t-5\t0",
                "2026-03-13\t2026-03-17\t-\t1"));

    assertThat(result.requests()).hasSize(1);
    assertThat(result.skipped())
        .extracting(MonthlyDividendPayoutImportParser.SkippedRow::row)
        .containsExactly(2, 3, 4);
    assertThat(result.skipped().get(0).reason())
        .contains(
            MessageUtil.getMessage("stock.monthly.reference.error.taxable.base.exceeds.dividend"));
    assertThat(result.skipped().get(1).reason())
        .contains(
            MessageUtil.getMessage("stock.monthly.reference.error.dividend.per.share.negative"));
    assertThat(result.skipped().get(2).reason())
        .contains(MessageUtil.getMessage("stock.monthly.reference.field.dividend.per.share"));
  }

  @Test
  void 칸이_모자란_줄도_건너뛴다() {
    MonthlyDividendPayoutImportParser.LenientParseResult result =
        parser.parseLenient(
            "0094M0",
            String.join(
                "\n",
                "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)",
                "2026-06-15\t2026-06-17",
                "2026-05-15\t2026-05-19\t390\t8"));

    assertThat(result.requests()).hasSize(1);
    assertThat(result.skipped()).hasSize(1);
    assertThat(result.skipped().get(0).reason())
        .isEqualTo(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.monthly.reference.import.skipped.row.unreadable"),
                1));
  }

  /** 가져올 것이 하나도 없으면 예전처럼 실패한다 - 그래야 일괄 가져오기가 실패 종목으로 센다. */
  @Test
  void 모든_행이_잘못되면_사유와_함께_실패한다() {
    assertThatThrownBy(
            () ->
                parser.parseLenient(
                    "0094M0",
                    String.join(
                        "\n",
                        "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)",
                        "2026-05-15\t2025-05-19\t390\t8")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2025-05-19");
  }

  private static String flatten(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
  }

  /** 두 가져오기 경로(단건 · 일괄)가 모두 관대한 결과를 받고, 건너뛴 행을 경고로 넘긴다. */
  @Test
  void 출처_가져오기_두_경로가_건너뛴_행을_알린다() throws IOException {
    String controller =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java");
    String service =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/util/MonthlyDividendPayoutSourceImportService.java");

    assertThat(service)
        .contains("monthlyDividendPayoutImportParser.parseLenient(symbol, bulkInput)");
    assertThat(controller).doesNotContain("fetchImportRequests(");
    assertThat(controller.split("monthlyDividendPayoutSourceImportService.fetchImport\\(", -1))
        .as("단건 + 일괄")
        .hasSize(3);
    // 2026-09-21 링크 등록이 세 번째 경로가 됐다(건너뛴 행 수를 같은 자리로 알린다).
    // 2026-09-28 총보수 · 상장일 새로 가져오기가 네 번째다(못 읽은 종목을 같은 자리로 알린다).
    assertThat(controller.split("\"monthlyDividendReferenceWarningMessage\"", -1))
        .as("단건 · 일괄 · 링크 등록 · 총보수 새로 가져오기 모두 경고를 넘긴다")
        .hasSize(5);
    assertThat(controller).contains("stock.monthly.reference.import.skipped.rows");
    assertThat(controller).contains("stock.monthly.reference.import.skipped.bulk");

    String admin = flatten("src/main/jte/stock/admin.jte");
    assertThat(admin)
        .contains("@param String monthlyDividendReferenceWarningMessage = \"\"")
        .contains(
            "monthlyDividendReferenceWarningMessage = monthlyDividendReferenceWarningMessage");
  }

  private static String renderReference(String warning) {
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendReferenceWarningMessage", warning);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendReference.jte", model, output);
    return output.toString();
  }

  @Test
  void 화면이_건너뛴_행_경고를_보인다() {
    String shown = renderReference("SKIPPED-ROWS-NOTICE");
    int at = shown.indexOf("data-import-skipped-warning");
    assertThat(at).as("경고 상자가 없다").isGreaterThan(0);
    assertThat(shown.substring(at, shown.indexOf("</div>", at))).contains("SKIPPED-ROWS-NOTICE");

    assertThat(renderReference("")).doesNotContain("data-import-skipped-warning");
  }
}
