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
 * 범위를 말하는 문구의 <b>수</b>는 실제 규칙의 상수와 같다.
 *
 * <p>실측 2026-09-16: 월배당 기준 카드가 "최근 12건 기준" 을 고정으로 달아, 이력이 11 건뿐인 종목에도 12 라고 말했다. 값은 맞는데 문구가 거짓이면
 * 사용자는 11 건 평균을 1 년 평균으로 읽는다. 그 자리는 자리표시자로 고쳤고, <b>고칠 수 없는(진짜 고정인) 수</b>는 여기서 상수와 묶어 둔다 &mdash; 상수를
 * 바꾸면 이 검사가 문구도 바꾸라고 말한다.
 *
 * <p>짝은 <b>손으로</b> 적는다. 수가 같다고 자동으로 이으면 엉뚱한 짝이 생긴다.
 */
class ScopePhraseConstantTest {

  /** {키, 문구에 박힌 수, 그 수가 있어야 할 파일, 그 파일에서 찾을 조각} */
  private static final List<String[]> PAIRS =
      List.of(
          new String[] {
            "stock.summary.trend.caption",
            "6",
            "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java",
            "minusMonths(6)"
          },
          new String[] {
            "stock.simulator.alert.max.scenarios",
            "5",
            "src/main/frontend/src/stock/stockSimulator.ts",
            "MAX_SCENARIOS = 5"
          },
          new String[] {
            "stock.simulator.alert.max.scenarios",
            "5",
            "src/main/resources/static/js/stock/stockSimulator.js",
            "MAX_SCENARIOS=5"
          },
          new String[] {
            "stock.simulator.compound.quickadd.small",
            "1000000",
            "src/main/jte/stock/fragments/compoundSimulator.jte",
            "data-amount-add=\"1000000\""
          },
          new String[] {
            "stock.simulator.compound.quickadd.large",
            "10000000",
            "src/main/jte/stock/fragments/compoundSimulator.jte",
            "data-amount-add=\"10000000\""
          },
          new String[] {
            "stock.error.badrequest.pagesize.desc",
            "200",
            "src/main/java/net/luversof/web/gate/stock/util/StockPageSizeUtil.java",
            "MAX_PAGE_SIZE = 200"
          },
          new String[] {
            "stock.dividend.calendar.short.history",
            "12",
            "src/main/java/net/luversof/web/gate/stock/service/MonthlyDividendCalculator.java",
            "limit(12)"
          },
          // TTM 은 '그 달을 끝으로 하는 12 개월' 이라 뒤로 11 달을 본다 - 수는 다르지만 같은 규칙이다.
          new String[] {
            "stock.dividend.chart.ttm.label",
            "12",
            "src/main/java/net/luversof/web/gate/stock/util/StockDividendTtmUtil.java",
            "minusMonths(11)"
          },
          new String[] {
            "stock.simulator.assumption.four",
            "12",
            "src/main/frontend/src/stock/stockSimulator.ts",
            "MONTHS_PER_YEAR = 12"
          });

  @Test
  void 문구에_박힌_수는_실제_상수와_같다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int checked = 0;
    for (String[] pair : PAIRS) {
      String key = pair[0];
      String number = pair[1];
      Path source = Path.of(pair[2]);
      String needle = pair[3];

      String phrase = message(key);
      if (phrase == null) {
        offenders.add(key + " 문구가 없다");
        continue;
      }
      // 문구가 그 수를 (숫자로든 낱말로든) 말하고 있는지 먼저 확인한다.
      if (!saysNumber(phrase, number)) {
        offenders.add(key + " 가 " + number + " 을 말하지 않는다: " + phrase);
        continue;
      }
      if (!Files.exists(source)) {
        offenders.add(key + " 의 짝 파일이 없다: " + source);
        continue;
      }
      String code = Files.readString(source, StandardCharsets.UTF_8);
      if (!code.contains(needle)) {
        offenders.add(
            key + " 가 말하는 " + number + " 이 " + source.getFileName() + " 의 " + needle + " 와 끊겼다");
        continue;
      }
      checked++;
    }

    assertThat(offenders).as("문구와 상수가 어긋난 자리").isEmpty();
    assertThat(checked).as("확인한 짝").isEqualTo(PAIRS.size());
  }

  /**
   * 빠른 추가 버튼이 쓰는 금액의 <b>집합</b>이 문구가 말하는 금액과 같다.
   *
   * <p>같은 금액이 여러 버튼에 있어(초기 원금 · 정기 납입액), 한 곳만 어긋나도 들어 있는지만 보면 통과한다 &mdash; 실측 2026-09-16: 그 변이를
   * 놓쳤다. 집합으로 맞대면 한 곳만 바뀌어도 드러난다.
   */
  @Test
  void 빠른_추가_버튼은_문구가_말하는_금액만_쓴다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/compoundSimulator.jte"), StandardCharsets.UTF_8);
    Matcher m = Pattern.compile("data-amount-add=\"([0-9]+)\"").matcher(template);
    java.util.Set<String> used = new java.util.TreeSet<>();
    int buttons = 0;
    while (m.find()) {
      buttons++;
      if (!"0".equals(m.group(1))) {
        used.add(m.group(1));
      }
    }

    assertThat(buttons).as("빠른 추가·초기화 버튼").isGreaterThanOrEqualTo(4);
    assertThat(used).as("버튼이 쓰는 금액(0 은 초기화라 뺀다)").containsExactlyInAnyOrder("1000000", "10000000");
    // 그 두 금액을 문구가 그대로 말한다.
    assertThat(saysNumber(message("stock.simulator.compound.quickadd.small"), "1000000"))
        .as("작은 버튼 문구")
        .isTrue();
    assertThat(saysNumber(message("stock.simulator.compound.quickadd.large"), "10000000"))
        .as("큰 버튼 문구")
        .isTrue();
  }

  /**
   * 문구가 그 수를 말하는가.
   *
   * <p>천 단위는 화면에서 압축해 적는다 &mdash; {@code 1000000} 은 "+1M" 이자 "+100만" 이다. 숫자를 그대로 찾지 못하면 압축 표기도 본다.
   */
  private static boolean saysNumber(String phrase, String number) {
    if (phrase.contains(number)) {
      return true;
    }
    long value = Long.parseLong(number);
    List<String> forms = new ArrayList<>();
    if (value >= 10000L && value % 10000L == 0) {
      forms.add(String.format("%,d만", value / 10000L)); // 100만 · 1,000만
      forms.add((value / 10000L) + "만");
    }
    if (value >= 1000000L && value % 1000000L == 0) {
      forms.add((value / 1000000L) + "M");
    }
    if (value == 5L) {
      forms.add("five");
    }
    for (String form : forms) {
      if (phrase.contains(form)) {
        return true;
      }
    }
    return false;
  }

  /** ko 와 en 문구를 이어 하나로 본다(한쪽만 수를 말해도 그 수를 말한 것으로 친다). */
  private static String message(String key) throws IOException {
    String english = lookup("src/main/resources/uiMessage.properties", key, false);
    String korean = lookup("src/main/resources/uiMessage_ko.properties", key, true);
    if (english == null && korean == null) {
      return null;
    }
    return (english == null ? "" : english) + " || " + (korean == null ? "" : korean);
  }

  private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

  private static String lookup(String file, String key, boolean decode) throws IOException {
    for (String line : Files.readString(Path.of(file), StandardCharsets.UTF_8).split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("#") || !line.contains("=")) {
        continue;
      }
      String[] parts = line.split("=", 2);
      if (!parts[0].trim().equals(key)) {
        continue;
      }
      String value = parts[1].trim();
      if (!decode) {
        return value;
      }
      Matcher m = ESCAPE.matcher(value);
      StringBuilder sb = new StringBuilder();
      while (m.find()) {
        m.appendReplacement(sb, String.valueOf((char) Integer.parseInt(m.group(1), 16)));
      }
      m.appendTail(sb);
      return sb.toString();
    }
    return null;
  }
}
