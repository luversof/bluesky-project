package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 원장 점검 규칙의 문구.
 *
 * <p>규칙 코드는 <b>다른 서비스</b>(api-stock)가 정하는데 문구는 게이트가 갖고 있다. 그래서 저쪽에 규칙이 하나 늘면 이쪽은 모르는 코드를 받는다.
 *
 * <p>그때 <b>조용히 빈 칸이 된다</b>는 것이 문제였다 &mdash; {@link MessageUtil#getMessage(String)} 은 못 찾은 키에 예외를
 * 던지지도, 코드를 돌려주지도 않고 <b>빈 문자열</b>을 준다(한 인자 형태가 기본값으로 {@code ""} 를 넘긴다). 관리 화면에는 이름 없는 경고 "(3)" 만
 * 남고, 사유가 여럿인 행은 ", , " 가 된다. 아래 {@code 못_찾은_키는_빈_문자열이다} 가 그 사실을 못박는다.
 *
 * <p>실측 2026-09-14: api-stock 의 코드 20 개가 en·ko 양쪽에 다 있다. 지금 깨진 것은 없고 <b>깨졌을 때 조용한 것</b>을 고친 것이다.
 */
class StockLedgerRuleLabelTest {

  private static final String PREFIX = "stock.admin.ledger.rule.";

  private static final Path EN = Path.of("src/main/resources/uiMessage.properties");

  private static final Path KO = Path.of("src/main/resources/uiMessage_ko.properties");

  /** 형제 모듈. 워크스페이스를 통째로 받지 않았으면 없을 수 있다. */
  private static final Path API_RULES =
      Path.of(
          "../../bluesky-api/bluesky-api-stock/src/main/java/net/luversof/api/stock/service"
              + "/LedgerIntegrityService.java");

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    // useCodeAsDefaultMessage 를 켜면 못 찾은 키가 키 이름으로 나와 이 검사의 의미가 사라진다 - 운영과 같은 기본값으로 둔다.
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  /** 이 가드가 존재하는 이유. 이 동작이 바뀌면(예외를 던지게 되면) 아래 대비책도 다시 볼 것. */
  @Test
  void 못_찾은_키는_빈_문자열이다() {
    assertThat(MessageUtil.getMessage(PREFIX + "NO_SUCH_RULE_ZZZ")).isEmpty();
  }

  @Test
  void 모르는_코드는_코드_그대로_보여준다() {
    assertThat(StockLedgerRuleLabelUtil.label("NO_SUCH_RULE_ZZZ")).isEqualTo("NO_SUCH_RULE_ZZZ");
    assertThat(StockLedgerRuleLabelUtil.label(null)).isEmpty();
    assertThat(StockLedgerRuleLabelUtil.label("  ")).isEmpty();
  }

  @Test
  void 아는_코드는_문구로_보여준다() {
    String label = StockLedgerRuleLabelUtil.label("TRADE_ON_WEEKEND");

    assertThat(label).isNotEmpty().isNotEqualTo("TRADE_ON_WEEKEND");
  }

  /** api-stock 이 내는 코드는 두 로케일 모두에 문구가 있어야 한다. */
  @Test
  void api_stock_의_규칙이_모두_문구를_갖는다() throws IOException {
    assumeTrue(Files.isRegularFile(API_RULES), "형제 모듈 api-stock 이 없다 - 이 검사는 건너뛴다");

    Set<String> codes = apiRuleCodes();
    assertThat(codes).as("코드를 하나도 못 읽었다면 이 검사는 공짜로 통과한다").hasSizeGreaterThan(10);

    Set<String> en = definedCodes(EN);
    Set<String> ko = definedCodes(KO);
    List<String> missing = new ArrayList<>();
    for (String code : codes) {
      if (!en.contains(code)) {
        missing.add(code + " (en)");
      }
      if (!ko.contains(code)) {
        missing.add(code + " (ko)");
      }
    }

    assertThat(missing).as("문구가 없으면 관리 화면에 코드가 그대로 나간다").isEmpty();
  }

  /** 템플릿이 키를 직접 조립하면 대비책을 건너뛴다. */
  @Test
  void 템플릿은_키를_직접_조립하지_않는다() throws IOException {
    String template = Files.readString(FRAGMENT, StandardCharsets.UTF_8);

    assertThat(template).contains("StockLedgerRuleLabelUtil.label(");
    assertThat(template)
        .as("getMessage 로 직접 찾으면 모르는 코드가 다시 빈 칸이 된다")
        .doesNotContain("getMessage(" + q() + PREFIX);
  }

  private static String q() {
    return String.valueOf((char) 34);
  }

  /** {@code "TRADE_ON_WEEKEND"} 같은 대문자 상수 문자열을 코드로 본다. */
  private static Set<String> apiRuleCodes() throws IOException {
    String source = Files.readString(API_RULES, StandardCharsets.UTF_8);
    Matcher matcher = Pattern.compile(q() + "([A-Z][A-Z0-9_]{5,})" + q()).matcher(source);
    Set<String> codes = new LinkedHashSet<>();
    while (matcher.find()) {
      codes.add(matcher.group(1));
    }
    return codes;
  }

  private static Set<String> definedCodes(Path path) throws IOException {
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    Set<String> codes = new LinkedHashSet<>();
    for (String name : properties.stringPropertyNames()) {
      if (name.startsWith(PREFIX)) {
        codes.add(name.substring(PREFIX.length()));
      }
    }
    return codes;
  }
}
