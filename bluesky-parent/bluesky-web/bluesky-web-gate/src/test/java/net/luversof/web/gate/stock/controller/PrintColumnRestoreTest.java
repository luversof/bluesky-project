package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 폭 때문에 숨긴 열은 인쇄에서 되살려야 한다.
 *
 * <p>{@code hidden min-[1300px]:table-cell} 같은 클래스는 화면 폭이 그만큼 넓을 때만 열을 보인다. 인쇄 지면은 그보다 좁아 이 질의가 결코
 * 참이 되지 않으므로, {@code print:table-cell} 이 없으면 <b>인쇄본에서 그 열이 통째로 빠진다</b>.
 *
 * <p>실측 2026-09-11(1280px, print 미디어): 매매 상세 목록에서 <b>수수료·거래세</b> 2 열이, 자산 현황 종목별 표에서
 * <b>평균단가·실현손익·누적배당</b> 3 열이 인쇄에서도 숨은 채였다. 저장소는 이미 같은 상황에 {@code print:table-cell} 을 붙여
 * 왔다(assetStatus 의 min-[850px] 열, dividendYieldAnalytics 의 md 열 등).
 *
 * <p>한 열은 머리글·본문·합계가 함께 움직여야 하므로 클래스가 붙은 모든 칸을 검사한다.
 */
class PrintColumnRestoreTest {

  private static final Path FRAGMENTS = Path.of("src/main/jte/stock/htmx/fragments");
  private static final String OPEN = "hidden min-[";
  private static final String CLOSE = "px]:table-cell";
  private static final String PRINT = " print:table-cell";

  /** {@code hidden min-[NNNpx]:table-cell} 을 찾아 (픽셀, 인쇄 복원 여부) 로 돌려준다. 정규식을 쓰지 않는다. */
  private List<int[]> wideOnly(String template) {
    List<int[]> found = new ArrayList<>();
    int at = template.indexOf(OPEN);
    while (at >= 0) {
      int close = template.indexOf(CLOSE, at);
      if (close < 0) {
        break;
      }
      String digits = template.substring(at + OPEN.length(), close);
      if (!digits.isEmpty() && digits.chars().allMatch(Character::isDigit)) {
        boolean restored = template.startsWith(PRINT, close + CLOSE.length());
        found.add(new int[] {Integer.parseInt(digits), restored ? 1 : 0});
      }
      at = template.indexOf(OPEN, at + 1);
    }
    return found;
  }

  @Test
  void 넓은_폭에서만_보이는_열은_인쇄_복원을_단다() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(FRAGMENTS)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        for (int[] hit : wideOnly(Files.readString(p, StandardCharsets.UTF_8))) {
          // 인쇄 지면은 900px 를 넘지 않는다 - 그보다 넓어야 보이는 열은 인쇄에서 영영 안 보인다.
          if (hit[0] > 900 && hit[1] == 0) {
            offenders.add(p.getFileName() + " min-[" + hit[0] + "px]");
          }
        }
      }
    }

    assertThat(offenders).as("인쇄에서 사라지는 열").isEmpty();
  }

  /**
   * 인쇄 폭에 들어가는 표는 lg/xl 열도 되살린다. 다만 되살리면 지면을 넘는 표는 그대로 둔다.
   *
   * <p>실측 2026-09-11(816px, print, 숨은 열을 강제로 보이게 해 흉내 냄): 표가 폭에 맞춰 줄어들어 <b>칸 잘림은 0</b> 이었지만 자산 현황
   * 종목별 표만 782 → <b>903px</b> 로 지면(816)을 넘었다. 나머지는 750~784px 로 들어갔다. 그래서 아래 넷만 되살린다.
   */
  @Test
  void 폭에_들어가는_표는_lg_xl_열도_되살린다() throws IOException {
    String[] restored = {
      "dividend/dividendYieldAnalytics.jte",
      "dividend/dividendTable.jte",
      "stockContributionTable.jte",
    };
    for (String name : restored) {
      String template = Files.readString(FRAGMENTS.resolve(name), StandardCharsets.UTF_8);
      assertThat(template)
          .as(name + " 에 인쇄 복원이 없는 lg 열이 남았다")
          .doesNotContain("hidden lg:table-cell\"");
      assertThat(template)
          .as(name + " 에 인쇄 복원이 없는 xl 열이 남았다")
          .doesNotContain("hidden xl:table-cell\"");
    }
  }

  /**
   * 지면을 넘던 표도 글자를 한 단계 더 조여 되살렸다(2026-09-11 갱신).
   *
   * <p>앞선 실측은 숨은 열을 강제로 보이게 한 <b>렌더 폭</b> 903px 였다. 이번에는 표를 떼어내 {@code width:min-content} 로 재 <b>열
   * 최소 폭 합</b>을 봤더니 824px 였고(지면 782px), 셀 글자를 0.70rem 으로 내리면 722px 로 들어간다. 그래서 열을 숨기는 대신 {@code
   * print-dense} 를 붙였다 &mdash; 자세한 근거와 수치는 {@code AssetStatusPrintColumnTest}.
   */
  @Test
  void 넘치던_표는_숨기는_대신_조여서_되살린다() throws IOException {
    String template =
        Files.readString(FRAGMENTS.resolve("assetStatus.jte"), StandardCharsets.UTF_8);

    assertThat(template).as("인쇄 복원이 없는 lg 열이 남으면 안 된다").doesNotContain("hidden lg:table-cell\"");
    assertThat(template).as("인쇄 복원이 없는 xl 열이 남으면 안 된다").doesNotContain("hidden xl:table-cell\"");
    assertThat(template).as("넓은 표는 인쇄에서 한 단계 더 조인다").contains("print-dense");
  }

  @Test
  void 실제로_인쇄_복원을_달아_둔_열이_있다() throws IOException {
    int withPrint = 0;
    try (Stream<Path> walk = Files.walk(FRAGMENTS)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String template = Files.readString(p, StandardCharsets.UTF_8);
        int at = template.indexOf("print:table-cell");
        while (at >= 0) {
          withPrint++;
          at = template.indexOf("print:table-cell", at + 1);
        }
      }
    }

    assertThat(withPrint).as("매매 상세 6 + 자산 현황 9 + 기존 15").isGreaterThanOrEqualTo(30);
  }
}
