package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기준 가격·기준일 원금이 비는 줄은 왜 비었는지 그 자리에서 말해야 한다.
 *
 * <p>실측 2026-09-11(전체 기간 배당 202 건 중 5 건): 하나금융지주 2 건은 매매 기록 자체가 없고, 삼성SDI(2020-04-17) ·
 * NAVER(2021-04-08) · HK이노엔(2022-04-22) 3 건은 결산배당인데 배당 기준일이 기록돼 있지 않아 지급일로 되짚는다 &mdash; 그 사이에 팔았으니
 * 그 날 보유가 0 이다. api 의 원장 점검도 같은 이유로({@code hasOwnRecordDate}) 이 3 건을 지적하지 않는다.
 *
 * <p>이 줄들은 수익률 분자·분모에서 함께 빠지는데, 화면에는 "-" 만 있었다.
 */
class DividendBasisMissingNoteTest {

  private static final Path TABLE =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 원금이_있는_줄에는_붙이지_않는다() throws IOException {
    String template = read(TABLE);

    assertThat(template)
        .as("값이 있으면 null 을 돌려줘야 JTE 가 title 속성을 통째로 뺀다")
        .contains("if (item.principalCost() != null)");
    assertThat(template).contains("stock.dividend.basis.missing");
  }

  @Test
  void 기준일이_없으면_지급일로_되짚었다고_밝힌다() throws IOException {
    String template = read(TABLE);

    assertThat(template).contains("item.recordDate() != null ? item.recordDate() : item.payDate()");
    // 실측 2026-09-11: 이 5 건은 모두 기준일이 지급일과 같은 날로 적혀 있었다(비어 있지 않다).
    // api 의 원장 점검도 같은 기준(hasOwnRecordDate: 기준일이 있고 지급일과 다를 것)으로 이 줄들을 지적하지 않는다.
    assertThat(template)
        .as("기준일이 지급일과 같은 날이어도 '지급일로 되짚었다' 여야 한다")
        .contains("boolean ownRecordDate = item.recordDate() != null && item.payDate() != null");
    assertThat(template).contains("ownRecordDate ? note : note + ");
    assertThat(template).contains("stock.dividend.basis.missing.paydate");
  }

  /**
   * 근거는 한 칸에만 단다.
   *
   * <p>세 칸(기준 가격 · 기준일 원금 · 수익률)에 달았더니 행 하나가 1,146B 가 되어 행당 1KB 상한({@code
   * DividendTableCompactOutputTest})을 넘었다 &mdash; 실측 2026-09-11. 어느 폭에서나 보이는 수익률 칸에 남긴다(나머지 둘은 xl
   * 이상에서만 보이고, 그때는 이 칸이 바로 옆이다).
   */
  @Test
  void 근거는_항상_보이는_수익률_칸에만_단다() throws IOException {
    String template = read(TABLE);

    // 칸 수는 title= 자리의 호출만 센다. 2026-09-11 에 이 칸이 sr-only 도 함께 달면서 호출 자체는 3 번이 됐는데,
    // 그건 칸이 늘어난 게 아니라 같은 까닭을 눈과 보조기술 양쪽에 보낸 것이다.
    assertThat(count(template, "title=" + '"' + "${basisMissingTitle.apply(item)}"))
        .as("수익률 칸 하나")
        .isEqualTo(1);
    assertThat(count(template, "basisMissingTitle.apply(item)"))
        .as("툴팁 1 + sr-only(조건 + 본문) 2")
        .isEqualTo(3);
    assertThat(template)
        .as("그 칸은 폭에 따라 숨지 않는 칸이어야 한다")
        .contains(
            "text-right text-dividend font-medium" + '"' + " title=" + '"' + "${basisMissingTitle");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read(Path.of("src/main/resources").resolve(name));
      assertThat(bundle).contains("stock.dividend.basis.missing ");
      assertThat(bundle).contains("stock.dividend.basis.missing.paydate");
    }
  }
}
