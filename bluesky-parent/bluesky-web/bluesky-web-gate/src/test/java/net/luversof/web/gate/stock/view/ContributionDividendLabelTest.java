package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간이 걸린 표의 열 이름이 "누적" 이면 안 된다.
 *
 * <p>자산 성장의 <b>종목별 기여</b> 표는 기간이 걸린다. 그런데 배당 열이 자산 현황과 <b>같은 메시지 키</b> ({@code
 * stock.asset.status.col.dividend.total} = "누적 배당")를 쓰고 있었다.
 *
 * <p>실측 2026-09-12: 이 표의 배당 합이 전체 기간 65,652,134 / 2025 년 14,524,375 로 <b>기간을 따라갔다</b> (2025 년 배당
 * 세후와 같은 값). 같은 이름을 쓰던 자산 현황은 두 기간 모두 59,067,537 로 같아 그쪽은 진짜 누적이다 &mdash; 한 키를 범위가 다른 두 자리가 나눠 쓰면서
 * 한쪽이 거짓말을 하고 있었다.
 */
class ContributionDividendLabelTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/htmx/fragments/stockContributionTable.jte";

  @Test
  void 기여_표는_기간_이름을_쓴다() throws IOException {
    String jte = Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8);
    assertThat(jte)
        .as("기간이 걸린 표가 누적 이름을 쓰면 안 된다")
        .doesNotContain("stock.asset.status.col.dividend.total");
    assertThat(jte).contains("stock.asset.growth.contribution.col.dividend");
  }

  /** 자산 현황은 진짜 누적이므로 그쪽 이름은 그대로 둔다. */
  @Test
  void 자산_현황은_누적_이름을_유지한다() throws IOException {
    String jte =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte"), StandardCharsets.UTF_8);
    assertThat(jte).contains("stock.asset.status.col.dividend.total");
  }

  @Test
  void 두_번들에_새_이름이_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(
              Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
      assertThat(text).as(bundle).contains("stock.asset.growth.contribution.col.dividend");
    }
  }
}
