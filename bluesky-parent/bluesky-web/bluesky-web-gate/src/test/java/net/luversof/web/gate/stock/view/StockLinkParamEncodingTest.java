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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주소에 싣는 <b>값</b>은 인코딩해서 싣는다.
 *
 * <p>실측 2026-09-14: {@code monthlyDividendReference.jte} 한 파일 안에서 같은 파라미터를 어떤 줄은 {@code URLEncoder}
 * 로 싣고(자바 문자열로 주소를 만드는 75·85 행) 어떤 줄은 날것으로 싣고 있었다(표 안 링크 6 곳). 한 기능 안에서 규칙이 갈린 채였다.
 *
 * <p>지금 데이터로는 두 방식의 결과가 같다 &mdash; 등록된 86 종목이 모두 6 자리 {@code [0-9CDEGMNPR]} 이고 지급일은 {@code
 * 2026-09-14} 꼴이다. 즉 <b>화면으로는 영원히 드러나지 않는다</b>. 그래서 눈이 아니라 이 검사가 지킨다.
 *
 * <p>코드 모양을 강제하는 것은 게이트가 아니라 원장이다 &mdash; 월배당 기준 데이터의 검증은 "등록된 종목 코드인가" 만 보고 글자를 제한하지 않는다. 원장에 공백이나
 * {@code &} 가 든 코드가 한 번 들어오면 그 링크는 파라미터가 잘린 채 나간다.
 *
 * <p>대상은 <b>사용자·원장에서 온 값</b>만이다. UUID·정렬 키·탭 이름 같은 내부 상수는 뺀다.
 */
class StockLinkParamEncodingTest {

  private static final Path JTE_STOCK = Path.of("src/main/jte/stock");

  /** 사용자·원장에서 오는 값이라 모양을 장담할 수 없는 파라미터. */
  private static final List<String> MUST_ENCODE =
      List.of("symbol", "keyword", "payoutRecordDate", "payoutPayDate");

  private static final Pattern SITE =
      Pattern.compile(
          "("
              + String.join("|", MUST_ENCODE)
              + ")=(?:"
              + q()
              + "\\s*[+]\\s*|"
              + Pattern.quote("${")
              + ")");

  private static String q() {
    return String.valueOf((char) 34);
  }

  @Test
  void 주소에_싣는_사용자_값은_인코딩한다() throws IOException {
    List<String> raw = new ArrayList<>();
    int sites = 0;
    List<Path> templates;
    try (Stream<Path> files = Files.walk(JTE_STOCK)) {
      templates =
          files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jte")).toList();
    }
    for (Path path : templates) {
      String text =
          Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", " ");
      String[] lines = text.split(String.valueOf((char) 10), -1);
      for (int i = 0; i < lines.length; i++) {
        Matcher matcher = SITE.matcher(lines[i]);
        while (matcher.find()) {
          sites++;
          // 값 표현식은 그 자리에서 시작한다. 같은 줄의 다른 인코딩에 속지 않게 뒤쪽 120 자만 본다.
          int from = matcher.end();
          String value = lines[i].substring(from, Math.min(lines[i].length(), from + 120));
          if (!value.contains("URLEncoder")) {
            raw.add(
                path.getFileName()
                    + ":"
                    + (i + 1)
                    + " "
                    + matcher.group(1)
                    + " -> "
                    + value.split("[}&" + q() + "]")[0].trim());
          }
        }
      }
    }

    // 자가검사 - 자리를 하나도 못 찾으면 이 검사는 공짜로 통과한다.
    assertThat(templates).as("조각을 못 읽었다").hasSizeGreaterThan(20);
    assertThat(sites).as("주소에 값을 싣는 자리를 못 찾았다 - 훑기가 무력하다").isGreaterThanOrEqualTo(7);

    assertThat(raw).as("한 기능 안에서 인코딩 규칙이 갈리면 드문 값에서만 조용히 깨진다").isEmpty();
  }
}
