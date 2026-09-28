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
 * 월배당 ETF 표의 title 은 낭독기에도 닿는다(2026-09-24).
 *
 * <p>실측: 보유 배지의 "금액은 시뮬레이터에서 봅니다" 와 분배금 추세 % 의 기준("최근 3 회 평균 X 원 · 12 회 평균 Y 원 대비")이 title 에만 있어,
 * 낭독기에는 "보유" · "(+21.96%)" 만 읽혔다 &mdash; 화면 하나에 37 건. span 의 title 은 보조기술에 안정적으로 닿지 않는다(같은 실수를 이 앱이
 * 두 번 겪었다). 규칙: 이 템플릿에서 title="${식}" 을 단 요소는 같은 식을 sr-only 로도 싣는다. 금액 칸(amount-value)은 공용 스크립트가 가림
 * · 복원과 함께 따로 다룬다.
 */
class MonthlyEtfTitleReachesAtTest {

  // 식 안에 따옴표가 든다(getMessage("...")) - [^"]+ 로는 못 잡는다. 식의 끝 }" 까지 게으르게.
  private static final Pattern TITLE_EXPR = Pattern.compile("title=\"(\\$\\{.*?\\})\"");

  @Test
  void title_식은_sr_only_로도_실린다() throws IOException {
    String template =
        Files.readString(Path.of("src/main/jte/stock/monthlyEtf.jte"), StandardCharsets.UTF_8);
    String squashed = template.replaceAll("\\s+", "");
    List<String> checked = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    Matcher matcher = TITLE_EXPR.matcher(template);
    while (matcher.find()) {
      String tag = template.substring(template.lastIndexOf('<', matcher.start()), matcher.start());
      if (tag.contains("amount-value")) {
        continue;
      }
      String expr = matcher.group(1);
      checked.add(expr);
      if (!squashed.contains(
          ("<span class=\"sr-only\">" + expr + "</span>").replaceAll("\\s+", ""))) {
        missing.add(expr);
      }
    }
    assertThat(checked).as("보유 배지와 추세 % 는 반드시 검사 대상이다").hasSizeGreaterThanOrEqualTo(2);
    assertThat(missing).as("title 에만 있는 정보(마우스 전용)").isEmpty();
  }
}
