package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 목록 조각의 렌더 크기.
 *
 * <p>실측 2026-09-09: 전체 기간 배당 목록 조각이 371KB(204 행)였고 그중 30%가 줄 앞 들여쓰기 공백이었다. JTE 는 템플릿의 공백을 그대로 내보내므로
 * 4칸 공백 들여쓰기는 행마다 300 자 넘게 실린다. 배당 템플릿 7개를 탭 들여쓰기로 바꿔 줄였다(같은 화면 모양, 저장소의 다수 스타일). 누가 다시 공백으로 서식을
 * 맞추면 조용히 되돌아가므로 렌더 결과로 지킨다.
 */
class DividendTableCompactOutputTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendTable.jte";

  /** 줄 앞에 공백 4개 이상이 오고 태그가 이어지는 줄 - 공백 들여쓰기의 흔적. */
  private static final Pattern SPACE_INDENTED_TAG = Pattern.compile("(?m)^ {4,}<");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static DividendView row(String stock, String account, String net) {
    return new DividendView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        account,
        UUID.randomUUID(),
        stock,
        10,
        new BigDecimal("100"),
        new BigDecimal(net).add(new BigDecimal("150")),
        new BigDecimal("150"),
        new BigDecimal("1000"),
        null,
        new BigDecimal(net),
        Instant.parse("2026-08-01T00:00:00Z"),
        Instant.parse("2026-08-15T00:00:00Z"),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private String render(List<DividendView> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("dividendList", rows);
    model.put("zone", ZoneId.of("Asia/Seoul"));
    model.put("pagination", null);
    model.put("sort", "");
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    Function<BigDecimal, String> percent = v -> v == null ? "-" : v.toPlainString() + "%";
    model.put("percentFormat", percent);
    model.put("detailSummaryLabel", "상세");
    model.put("totalGrossAmount", new BigDecimal("300"));
    model.put("totalNetAmount", new BigDecimal("200"));
    model.put("totalTax", new BigDecimal("100"));
    model.put("totalTaxableAmount", new BigDecimal("2000"));
    for (String label :
        List.of(
            "accountLabel",
            "stockNameLabel",
            "grossAmountLabel",
            "netAmountLabel",
            "taxLabel",
            "taxableAmountLabel",
            "totalLabel")) {
      model.put(label, label);
    }
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 행은_공백_들여쓰기를_싣지_않는다() {
    String html = render(List.of(row("삼성전자", "KB증권", "1000"), row("TIGER 리츠", "미래에셋", "2000")));

    assertThat(html).contains("삼성전자").contains("TIGER 리츠");
    assertThat(SPACE_INDENTED_TAG.matcher(html).find())
        .as("줄 앞 공백 4칸 + 태그가 있으면 공백 들여쓰기가 되돌아온 것이다:\n%s", firstSpaceIndented(html))
        .isFalse();
  }

  /** 두 행의 크기 차이 = 행 하나의 비용. 들여쓰기 공백을 뺀 뒤에는 한 행이 1KB 를 넘지 않는다(실측: 줄이기 전 1,130 자). */
  @Test
  void 행_하나는_1KB_를_넘지_않는다() {
    int one = render(List.of(row("삼성전자", "KB증권", "1000"))).length();
    int two = render(List.of(row("삼성전자", "KB증권", "1000"), row("삼성전자", "KB증권", "1000"))).length();

    assertThat(two - one).as("행 하나의 렌더 크기").isBetween(300, 1000);
  }

  /**
   * 주식 화면 템플릿은 모두 탭 들여쓰기다 - 소스 쪽에서도 지킨다.
   *
   * <p>2026-09-09 실측: 공백 들여쓰기 템플릿 39개를 탭으로만 바꿔 활동 내역(전체) 1,273KB → 45% 가 줄 앞 공백, 배당 목록 371KB → 30%.
   * 화면 모양은 같다(공백 정규화 뒤 태그·글자열 동일). 새 템플릿을 공백으로 들여쓰면 여기서 걸린다.
   */
  @Test
  void 주식_템플릿은_탭으로_들여쓴다() throws IOException {
    Path base = Path.of("src/main/jte/stock");
    List<Path> offenders = new java.util.ArrayList<>();
    try (var stream = Files.walk(base)) {
      for (Path template : stream.filter(path -> path.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(template, StandardCharsets.UTF_8);
        if (SPACE_INDENTED_TAG.matcher(source).find()) {
          offenders.add(base.relativize(template));
        }
      }
    }
    assertThat(offenders).as("공백 4칸으로 들여쓴 태그가 있는 템플릿").isEmpty();
  }

  private static String firstSpaceIndented(String html) {
    var matcher = Pattern.compile("(?m)^ {4,}<.*$").matcher(html);
    return matcher.find() ? matcher.group() : "";
  }
}
