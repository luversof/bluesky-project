package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 낙폭이 어떤 기준으로 잰 값인지 화면에 적는다.
 *
 * <p>자산 성장의 "변동 위험" 절은 <b>입출금을 제거한 기준가(시간가중)</b> 로 낙폭을 잰다 (api-stock {@code TradeProfitService}:
 * {@code (timeWeightedFactor - peakFactor) / peakFactor}). 바로 위 "기간 최고 평가액" 옆의 비율은 <b>원시 평가액</b>(기간
 * 말 / 최고)이라 분모가 다르다.
 *
 * <p>실측 2026-09-12(전체 기간): 같은 화면에 최고 대비 <b>-22.71%</b>(1,622,109,770 / 2,098,800,125 - 1)와 현재 전고점
 * 대비 <b>-23.55%</b> 가 나란히 있었다. 둘 다 맞지만 화면에는 둘 다 "전고점 대비" 로 읽혀 어느 쪽이 맞는지 알 수 없었다 &mdash; 보유 평가액
 * 시계열(일별 6,186점 포함)의 어떤 정의로도 -23.55% 가 재현되지 않는다. 소스를 보고서야 시간가중 기준임이 확인됐다.
 *
 * <p>같은 화면의 "선택 기간 투자 수익률" 은 이미 "입출금 영향을 제거한 운용 성과" 라고 적고 있었다 &mdash; 같은 규칙을 쓴다.
 */
class DrawdownBasisNoteTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte";

  @Test
  void 위험_절이_기준을_적는다() throws IOException {
    String jte = Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8);
    assertThat(jte).contains("stock.asset.growth.summary.risk.desc");
    assertThat(jte).as("문구를 실제로 그려야 한다").contains("${riskDescription}");
  }

  @Test
  void 두_번들에_문구가_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(
              Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
      assertThat(text).as(bundle).contains("stock.asset.growth.summary.risk.desc");
    }
  }
}
