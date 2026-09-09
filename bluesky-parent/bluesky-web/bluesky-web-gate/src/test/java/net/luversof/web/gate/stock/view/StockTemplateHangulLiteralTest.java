package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 템플릿은 화면 문구를 한글 리터럴로 박지 않는다(문구는 메시지 번들에서 온다).
 *
 * <p>실측 2026-09-09: 영어 로케일로 화면 10개를 렌더하니 남은 한글은 종목명·계좌명·태그 같은 사용자 데이터와 로케일 메뉴의 "한국어" 뿐이었다 (문구 누수
 * 0). 소스에서도 주석·선언·스크립트를 뺀 자리에 한글이 0 줄이었다. 이 상태가 유지되게 한다 &mdash; 2026-09-08 에 99 줄을 번들로 옮긴 뒤 다시 새지
 * 않도록.
 *
 * <p>검사에서 빼는 것: JTE 주석({@code <%-- --%>}), HTML 주석(JTE 가 출력에서 지운다, 실측 0),
 * {@code @import}/{@code @param} 줄, 자바 코드 블록 ({@code !{ }}, 주석이 들어 있다), 인라인 스크립트(주석·차트 라벨은 별도 관심사),
 * 블록/줄 주석.
 */
class StockTemplateHangulLiteralTest {

  private static final Path STOCK_TEMPLATES = Path.of("src/main/jte/stock");
  private static final Pattern HANGUL = Pattern.compile("[\\uAC00-\\uD7A3]");

  static String strippedForScan(String source) {
    String s = source;
    s = s.replaceAll("(?s)<%--.*?--%>", "");
    s = s.replaceAll("(?s)<!--.*?-->", "");
    s = s.replaceAll("(?m)^\\s*@(import|param).*$", "");
    s = s.replaceAll("(?s)!\\{.*?\\}", "");
    s = s.replaceAll("(?s)<script\\b.*?</script>", "");
    s = s.replaceAll("(?s)/\\*.*?\\*/", "");
    s = s.replaceAll("(?m)^\\s*//.*$", "");
    return s;
  }

  static List<String> hangulLines(String source) {
    List<String> hits = new ArrayList<>();
    String[] lines = strippedForScan(source).split("\\R");
    for (int i = 0; i < lines.length; i++) {
      if (HANGUL.matcher(lines[i]).find()) {
        hits.add((i + 1) + ": " + lines[i].strip());
      }
    }
    return hits;
  }

  @Test
  void 주식_템플릿에는_주석_밖_한글_리터럴이_없다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int scanned = 0;
    try (Stream<Path> walk = Files.walk(STOCK_TEMPLATES)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        scanned++;
        for (String hit : hangulLines(Files.readString(p, StandardCharsets.UTF_8))) {
          offenders.add(STOCK_TEMPLATES.relativize(p) + " " + hit);
        }
      }
    }
    assertThat(scanned).as("템플릿을 하나도 못 찾았다 - 경로가 옮겨졌다").isGreaterThan(30);
    assertThat(offenders)
        .as("화면 문구는 메시지 번들(uiMessage*.properties)로 옮긴다. 영어 로케일에서 그대로 새어 나간다")
        .isEmpty();
  }

  /** 검사가 무력하지 않은지: 주석은 걸러지고, 마크업 속 한글은 잡힌다. */
  @Test
  void 검사_자체가_한글을_가려낸다() {
    String ok =
        "<%-- 주석 --%>\n<!-- 주석 -->\n@param String 설명\n<script>var a = '주석';</script>\n<div>${x}</div>\n";
    assertThat(hangulLines(ok)).isEmpty();
    assertThat(hangulLines("<div>합계</div>\n")).hasSize(1);
    assertThat(hangulLines("<input placeholder=\"검색\">\n")).hasSize(1);
  }
}
