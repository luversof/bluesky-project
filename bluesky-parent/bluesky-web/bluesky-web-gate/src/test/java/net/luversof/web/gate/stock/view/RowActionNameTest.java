package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.frontend.TsSource;

/**
 * 줄마다 서 있는 링크·버튼은 어느 줄의 것인지 이름으로 말한다.
 *
 * <p>실측 2026-09-12(접근성 트리, 13 화면):
 *
 * <pre>
 *   관리 &gt; 월배당 기준  "이 값으로 시뮬레이터 채우기" 9개 · "이 종목 보기" 8개 · "링크 열기 (새 창에서 열림)" 8개
 *   자산 현황            "보유 종목 보기 (3) ▼" 2개 · "보유 종목 보기 (4) ▼" 3개
 * </pre>
 *
 * <p>목적지는 모두 다른데 이름은 같았다. 눈으로는 같은 줄을 보면 알지만, 링크 목록으로 건너뛰는 쪽에는 스물다섯 개가 같은 말이다 &mdash; 대시보드의 같은 이름
 * 카드, 자산 현황의 같은 이름 표 다섯 개와 같은 부류다.
 */
class RowActionNameTest {

  private static final Path REFERENCE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte");
  private static final Path ASSET_STATUS =
      Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");

  @Test
  void 기준_데이터의_줄_링크_셋은_종목을_달고_나온다() throws IOException {
    String template = Files.readString(REFERENCE, StandardCharsets.UTF_8);

    assertThat(
            countOf(
                template,
                "aria-label=\"${java.text.MessageFormat.format(profileActionAriaPattern,"))
        .as("이 종목 보기 · 시뮬레이터 채우기 · 링크 열기")
        .isEqualTo(3);
    assertThat(template)
        .as("이름이 비면 종목 대신 심볼이라도 들어가야 한다")
        .contains(
            "row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol()");
    assertThat(template)
        .as("새 창 안내는 이름에도 남아야 한다 - aria-label 이 sr-only 를 덮어쓴다")
        .contains("+ \" (\" + opensNewWindowLabel + \")\"");
  }

  @Test
  void 자산현황_펼침_버튼은_계좌를_달고_나온다() throws IOException {
    String template = Files.readString(ASSET_STATUS, StandardCharsets.UTF_8);

    assertThat(template).contains("accountHoldingsToggleAriaPattern");
    // 계좌 표의 "보유 종목 보기" 와 종목 표의 "보유 계좌 보기"(2026-09-17). 둘 다 그 줄의 이름을 단다.
    int toggles =
        countOf(template, "data-account-detail-toggle=")
            + countOf(template, "data-stock-detail-toggle=");
    assertThat(toggles).as("펼침 버튼 수").isEqualTo(2);
    assertThat(template)
        .as("종목 표의 펼침은 종목 이름을 단다")
        .contains(
            "MessageFormat.format(accountHoldingsToggleAriaPattern, showStockAccountsLabel, stockRowName)")
        .contains(
            "MessageFormat.format(accountHoldingsToggleAriaPattern, hideStockAccountsLabel, stockRowName)");
    assertThat(countOf(template, "data-expand-aria=")).as("펼칠 때 이름").isEqualTo(toggles);
    assertThat(countOf(template, "data-collapse-aria=")).as("접을 때 이름").isEqualTo(toggles);
    assertThat(
            countOf(
                template,
                "aria-label=\"${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern"))
        .as("처음 이름(펼치기)")
        .isEqualTo(toggles);
  }

  /** 보이는 글자만 바꾸면 펼친 뒤에도 "보기" 라고 읽힌다. */
  @Test
  void 펼친_뒤에는_이름도_접기로_바뀐다() throws IOException {
    String script =
        Files.readString(
            Path.of("src/main/frontend/src/stock/assetStatus.ts"), StandardCharsets.UTF_8);

    assertThat(TsSource.n(script))
        .contains(
            TsSource.n(
                "var nextAria = nextExpanded ? button.dataset.collapseAria : button.dataset.expandAria;"));
    assertThat(TsSource.n(script))
        .contains(TsSource.n("button.setAttribute('aria-label', nextAria);"));
  }

  @Test
  void 문구는_두_언어에_다_있고_두_자리를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      String line = null;
      for (String candidate : text.split(String.valueOf((char) 10))) {
        if (candidate.startsWith("stock.page.dividend.monthly.reference.profile.action.aria")) {
          line = candidate;
          break;
        }
      }
      assertThat(line).as(bundle).isNotNull();
      assertThat(line).as(bundle + " 에 두 자리가 있어야 한다").contains("{0}").contains("{1}");
    }
  }

  private int countOf(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }
}
