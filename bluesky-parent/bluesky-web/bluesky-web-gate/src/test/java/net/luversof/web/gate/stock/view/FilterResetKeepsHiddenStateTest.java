package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 필터 폼의 '초기화' 는 보이는 필터만 비우고, 폼이 숨은 입력으로 싣고 다니는 상태(탭 · 보기 · 기간 · 정렬)는 지킨다.
 *
 * <p>실측 2026-09-24: 월배당 ETF 의 '이번 적립' 보기에서 초기화를 누르면 전체 보기 · 기간 1 년으로 튕겼고, 필터를 적용하면 고른 기간(예: 3 개월)이
 * 풀렸다 &mdash; 정렬 링크는 둘 다 지키는데(템플릿 주석: "정렬 링크를 눌러도 기간이 풀리지 않는다") 폼만 달랐다. 그래서 규칙으로 고정한다: 폼의 숨은 입력 이름은
 * 모두 초기화 링크에도 실린다. 숨은 입력을 새로 달고 초기화를 빠뜨리면 여기서 걸린다.
 */
class FilterResetKeepsHiddenStateTest {

  private static final Pattern HIDDEN_NAME =
      Pattern.compile("type=\"hidden\"\\s+name=\"([A-Za-z]+)\"");

  private static final Pattern RESET_HREF =
      Pattern.compile("data-filter-reset\\s+href=\"([^\"]+)\"");

  @Test
  void 월배당_ETF_초기화는_보기_기간_정렬을_지킨다() throws IOException {
    String template = read("src/main/jte/stock/monthlyEtf.jte");
    String form = formBlock(template, "<form method=\"get\" action=\"/stock/monthly-etf\"");

    List<String> hidden = hiddenNames(form);
    assertThat(hidden)
        .as("필터를 적용해도 이번 적립 보기와 기간이 이어져야 한다")
        .contains("sort", "direction", "view", "period");

    String reset = resetTarget(template, form);
    for (String name : hidden) {
      assertThat(reset).as("초기화 링크가 숨은 입력 " + name + " 을 잃는다").contains(name + "=");
    }
    assertThat(reset)
        .as("초기화는 보이는 필터를 비운다")
        .doesNotContain("keyword=")
        .doesNotContain("payoutWindow=")
        .doesNotContain("account=")
        .doesNotContain("holding=");
  }

  @Test
  void 시뮬_월배당_초기화는_탭과_정렬을_지킨다() throws IOException {
    String template = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");
    String form = formBlock(template, "data-monthly-filter-form");

    List<String> hidden = hiddenNames(form);
    assertThat(hidden).contains("tab", "sort", "direction");

    String reset = resetTarget(template, form);
    for (String name : hidden) {
      assertThat(reset).as("초기화 링크가 숨은 입력 " + name + " 을 잃는다").contains(name + "=");
    }
  }

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private static String formBlock(String template, String marker) {
    int start = template.indexOf(marker);
    assertThat(start).as("필터 폼을 못 찾음: " + marker).isGreaterThanOrEqualTo(0);
    int end = template.indexOf("</form>", start);
    assertThat(end).isGreaterThan(start);
    return template.substring(start, end);
  }

  private static List<String> hiddenNames(String form) {
    List<String> names = new ArrayList<>();
    Matcher matcher = HIDDEN_NAME.matcher(form);
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
    return names;
  }

  /** 초기화 링크의 목적지. ${변수} 면 템플릿 머리의 그 변수 정의(세미콜론까지)를 돌려준다. */
  private static String resetTarget(String template, String form) {
    Matcher matcher = RESET_HREF.matcher(form);
    assertThat(matcher.find()).as("폼 안에 초기화 링크가 없다").isTrue();
    String href = matcher.group(1);
    if (!href.startsWith("${")) {
      return href;
    }
    String variable = href.substring(2, href.length() - 1).trim();
    int start = template.indexOf("String " + variable + " =");
    assertThat(start).as("초기화 주소 변수 정의를 못 찾음: " + variable).isGreaterThanOrEqualTo(0);
    return template.substring(start, template.indexOf(';', start));
  }
}
