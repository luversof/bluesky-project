package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 금액 숨김(hide-amounts)은 금액과 수량을 함께 가린다.
 *
 * <p>실측 2026-09-11(12 화면, 숨김을 켠 채 흐려지지 않고 보이는 숫자): <b>48 곳 8 종류</b>가 남아 있었다.
 *
 * <ul>
 *   <li>은퇴 시뮬레이터 연도별 · 월별 상세의 <b>수량 칸 40 개</b> &mdash; 금액 칸은 {@code amount-value} 인데 수량만 빠졌다.
 *   <li>대시보드 지표 카드의 보조줄 "매도 3억 4,657만 · 7건" &mdash; {@code sub} 가 통째로 글자라 가려지지 않았다.
 *   <li>종목 상세 '수량' 카드 &mdash; 이 카드만 {@code amount = false} 였다.
 *   <li>월배당 시뮬레이터의 "현재 N"(원장 기준 보유 수량 · 평단) 3 곳.
 * </ul>
 *
 * <p>수량도 가리는 이유: 공개 시세 x 수량 = 평가금액이라, 수량만 남으면 가린 뜻이 없다. 자산 현황 · 활동 목록은 이미 수량을 가린다. 고친 뒤 남은 것은 <b>3
 * 곳</b>이고 둘 다 사용자 자료가 아니다 &mdash; 복리 계산기의 "+1,000만" 빠른 입력 버튼, 관리 화면의 시세 행 수.
 */
class HideAmountsCoverageTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 시뮬레이터_수량_칸도_가린다() throws IOException {
    String built = read("src/main/resources/static/js/stock/stockSimulator.js");

    assertThat(built)
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .doesNotContain("<td>${formatShares(");
    assertThat(built.replace(" ", "")).contains("amount-value" + '"' + ">${formatShares(");
  }

  @Test
  void 지표_카드_보조줄의_금액을_가릴_수_있다() throws IOException {
    String statCard = read("src/main/jte/_components/ui/statCard.jte");
    String summary = read("src/main/jte/stock/htmx/fragments/summary.jte");

    assertThat(statCard).contains("@param String subAmount");
    assertThat(statCard)
        .contains("<span class=" + '"' + "amount-value" + '"' + ">${subAmount}</span>");
    assertThat(summary).as("대시보드 매도 금액").contains("subAmount = StockFormatUtil.compactKrw(");
  }

  @Test
  void 종목_상세_수량_카드도_가린다() throws IOException {
    String template = read("src/main/jte/stock/htmx/stockItemDetailContent.jte");

    // 주석에도 "amount = false" 라는 낱말이 있으므로 호출 자체를 본다 - 값 인자에서 괄호가 닫히면 뒤에 인자가 없다.
    assertThat(template)
        .as("이 카드만 amount = false 였다")
        .contains(
            "MessageUtil.getMessage("
                + (char) 34
                + "stock.activity.label.shares"
                + (char) 34
                + "))");
  }

  @Test
  void 월배당_시뮬레이터의_현재값도_가린다() throws IOException {
    String template = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

    assertThat(template).contains("text-warning amount-value");
  }
}
