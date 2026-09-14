package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 구간별 성과 표의 '수익률' 이 무엇 기준인지 밝히는 각주.
 *
 * <p>같은 줄의 '손익' 은 금액이고 '수익률' 은 <b>시간가중</b>(입출금 영향 제거)이라 축이 다르다. 실현·배당이 큰 해에는 부호까지 갈려 한 줄이 모순처럼 읽힌다
 * - 실측 2026-09-13: 삼성전자 상세 2021 년이 손익 <b>+9,445,170</b> 인데 수익률 <b>-0.71%</b> 였다(평가 변동이 -12,301,000
 * 이고 실현+배당이 +21,746,170). 두 값 다 맞으므로 고칠 것은 계산이 아니라 <b>기준 표기</b>다.
 *
 * <p>같은 회차 실측(산술은 이상 없음): 일곱 줄 모두 손익 = 평가 변동 + 실현·배당, 세 열의 합이 합계 줄과 일치, 수익률 연쇄곱 422.34% 가 합계 줄과
 * 같았다.
 */
class BreakdownReturnBasisTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/htmx/fragments/periodBreakdownTable.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  /** 그 키의 값 한 줄만 떼어 낸다. */
  private String valueOf(String properties) {
    int at = properties.indexOf("stock.item.detail.breakdown.return.basis");
    if (at < 0) {
      return "";
    }
    return properties.substring(properties.indexOf("=", at) + 1, properties.indexOf((char) 10, at));
  }

  private String squeezed(String path) throws IOException {
    return read(path).replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 표_아래에_기준을_적는다() throws IOException {
    String jte = squeezed(FRAGMENT);

    assertThat(jte)
        .as("문구를 읽어 오고")
        .contains("MessageUtil.getMessage(\"stock.item.detail.breakdown.return.basis\")");
    assertThat(jte).as("표 아래에 그린다").contains("data-breakdown-return-basis");
  }

  /** 각주는 표가 있을 때 함께 나와야 한다 - 표 없는 안내 분기에만 있으면 정작 필요한 자리에서 안 보인다. */
  @Test
  void 각주는_표와_같은_분기에_있다() throws IOException {
    String jte = read(FRAGMENT);
    int table = jte.indexOf("</table>");
    int note = jte.indexOf("data-breakdown-return-basis");

    assertThat(table).isGreaterThanOrEqualTo(0);
    assertThat(note).as("표 뒤에 온다").isGreaterThan(table);
    int elseBranch = jte.indexOf("@elseif(periodBreakdownNote");
    assertThat(elseBranch).isGreaterThanOrEqualTo(0);
    assertThat(note).as("표가 없는 분기로 넘어가기 전이다").isLessThan(elseBranch);
  }

  @Test
  void 문구가_두_로케일에_있고_기준을_말한다() throws IOException {
    String ko = read("src/main/resources/uiMessage_ko.properties");
    String en = read("src/main/resources/uiMessage.properties");

    // 값 안에서만 본다 - 파일 전체로 보면 같은 낱말을 쓰는 다른 메시지에 걸려 통과한다.
    assertThat(valueOf(ko)).as("한글 문구가 기준을 말한다(시간가중)").contains("\\uC2DC\\uAC04\\uAC00\\uC911");
    assertThat(valueOf(en).toLowerCase()).contains("time-weighted");
  }

  /** 열 이름 자체는 그대로다 - 좁은 칸에 긴 이름을 넣으면 다른 표와 어긋난다. */
  @Test
  void 열_이름은_건드리지_않는다() throws IOException {
    assertThat(squeezed(FRAGMENT))
        .contains("MessageUtil.getMessage(\"stock.item.detail.breakdown.col.return\")");
  }

  /**
   * 자산 성장 화면의 '연도별 성과' 도 같은 표다 - 손익(금액)과 투자 수익률(시간가중)이 한 줄에 있다. 실측 2026-09-13: 2021 년 손익
   * +12,459,027 인데 투자 수익률 -1.93% 였다(그 해 원금 변동 +136,526,730). 기준은 위 요약 카드에만 적혀 있어, 표만 보는 사람에게는 닿지
   * 않았다.
   */
  @Test
  void 자산_성장_연도별_표에도_기준을_적는다() throws IOException {
    String jte = squeezed("src/main/jte/stock/htmx/fragments/assetGrowthYearlySummary.jte");

    assertThat(jte).contains("MessageUtil.getMessage(\"stock.asset.growth.yearly.return.basis\")");
    assertThat(jte).contains("data-yearly-return-basis");
  }

  @Test
  void 자산_성장_문구도_두_로케일에_있다() throws IOException {
    for (String path :
        new String[] {
          "src/main/resources/uiMessage_ko.properties", "src/main/resources/uiMessage.properties"
        }) {
      String source = read(path);
      int at = source.indexOf("stock.asset.growth.yearly.return.basis");
      assertThat(at).as(path).isGreaterThanOrEqualTo(0);
      String value =
          source.substring(source.indexOf("=", at) + 1, source.indexOf((char) 10, at)).trim();
      assertThat(value).as(path + " 값").isNotEmpty();
    }
  }
}
