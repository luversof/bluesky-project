package net.luversof.web.gate.stock.view;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 배당 수익 분석 조각을 <b>화면에 그려지는 모양 그대로</b> 읽는다 &mdash; 원문을 세는 시험들이 쓴다.
 *
 * <p>2026-10-22 까지 {@code dividendYieldAnalytics.jte} 가 종목별 · 계좌별 카드(각 290 줄)를 한 벌씩 들고 있었고, 원문을 세는
 * 시험 16 개가 그 두 벌을 합쳐 셌다("본문 6 칸" 등). 카드를 {@code dividendYieldGroupCard.jte} 로 모은 뒤에도 같은 숫자가 같은 뜻이
 * 되도록, 부모 원문의 카드 호출 자리마다 카드 본문을 끼워 넣어 돌려준다(종목 · 계좌 두 번). 카드의 머리(인자 · 정의 블록)는 끼우지 않는다 &mdash; 문구 정의는
 * 부모에도 있다.
 */
public final class YieldAnalyticsSource {

  public static final Path PARENT =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte");

  public static final Path CARD =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldGroupCard.jte");

  private static final String CALL =
      "@template.stock.htmx.fragments.dividend.dividendYieldGroupCard(";

  /** 카드 머리의 정의 블록 끝(줄 처음의 닫는 중괄호). */
  private static final Pattern HEADER_END = Pattern.compile("(?m)^[}]\\r?$");

  private YieldAnalyticsSource() {}

  /** 카드 두 번을 끼워 넣은 부모 원문. 호출이 정확히 두 번(종목 · 계좌)이 아니면 실패한다 - 펼침이 헛돌지 않게. */
  public static String expanded() throws IOException {
    String parent = Files.readString(PARENT, StandardCharsets.UTF_8);
    String card = Files.readString(CARD, StandardCharsets.UTF_8);
    Matcher end = HEADER_END.matcher(card);
    if (!end.find()) {
      throw new IllegalStateException("카드 조각의 정의 블록 끝을 못 찾았다");
    }
    String body = card.substring(end.end());
    StringBuilder out = new StringBuilder();
    int from = 0;
    int calls = 0;
    for (int at = parent.indexOf(CALL); at >= 0; at = parent.indexOf(CALL, from)) {
      int close = parent.indexOf("\n\t\t)", at);
      if (close < 0) {
        throw new IllegalStateException("카드 호출이 닫히지 않았다");
      }
      out.append(parent, from, at).append(body);
      from = close + "\n\t\t)".length();
      calls++;
    }
    out.append(parent.substring(from));
    if (calls != 2) {
      throw new IllegalStateException("카드 호출이 종목 · 계좌 두 번이 아니다: " + calls);
    }
    return out.toString();
  }

  /**
   * 부모와 카드가 한 벌씩 들고 있는 정의 블록(문구 · 셈 도우미 - 부모의 연도별 표도 같은 것을 쓴다)을 보려면 두 원문을 <b>따로</b> 본다. "이 식이 있다" 는
   * 둘 다에서 참이어야 하고, "이 글자는 없다" 는 둘을 이은 글자에서 참이면 된다. {@link #expanded()} 는 카드의 정의 블록을 끼우지 않아 카드 쪽 식이
   * 검사 밖이었다(검토 2026-10-22).
   */
  public static java.util.List<String> definitionSources() throws IOException {
    return java.util.List.of(
        Files.readString(PARENT, StandardCharsets.UTF_8),
        Files.readString(CARD, StandardCharsets.UTF_8));
  }

  /** 파일을 훑는 시험용: 부모는 펼친 원문, 카드 조각 파일은 빈 글자(부모 펼침에서 이미 두 번 셌다), 나머지는 그대로. */
  public static String readForScan(Path path) throws IOException {
    Path normalized = path.toAbsolutePath().normalize();
    if (normalized.equals(PARENT.toAbsolutePath().normalize())) {
      return expanded();
    }
    if (normalized.equals(CARD.toAbsolutePath().normalize())) {
      return "";
    }
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
