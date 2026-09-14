package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 관리 화면의 "지급 시기" 는 코드가 아니라 말로 적는다.
 *
 * <p>실측 2026-09-12(관리 &rarr; 월배당 기준 데이터, 프로필 8 행): 표의 "지급 시기" 칸이 {@code MID_MONTH} · {@code
 * MONTH_END} 로 코드 그대로였다. 그런데 <b>같은 화면</b>의 편집 폼 드롭다운은 예전부터 "월중 / 월말 / 기타 / 미확인" 을 쓰고 있었다 &mdash; 한
 * 화면이 같은 값을 두 말로 적었다.
 *
 * <p>문구는 폼이 쓰는 키를 그대로 재사용한다. 모르는 코드는 감추지 않고 그대로 보여 준다.
 */
class StockPayoutWindowLabelUtilTest {

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  @Test
  void 네_가지_코드를_말로_바꾼다() {
    assertThat(StockPayoutWindowLabelUtil.label("MID_MONTH")).isEqualTo("월중");
    assertThat(StockPayoutWindowLabelUtil.label("MONTH_END")).isEqualTo("월말");
    assertThat(StockPayoutWindowLabelUtil.label("OTHER")).isEqualTo("기타");
    assertThat(StockPayoutWindowLabelUtil.label("UNKNOWN")).isEqualTo("미확인");
  }

  /** 모르는 코드를 빈 칸으로 만들면 관리 화면에서 무엇이 들어 있는지 알 수 없다. */
  @Test
  void 모르는_코드는_그대로_보여_준다() {
    assertThat(StockPayoutWindowLabelUtil.label("WEEKLY")).isEqualTo("WEEKLY");
    assertThat(StockPayoutWindowLabelUtil.label(null)).isEmpty();
    assertThat(StockPayoutWindowLabelUtil.label("  ")).isEmpty();
  }

  @Test
  void 표가_이_규칙을_거친다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);
    assertThat(flatten(template))
        .as("코드를 그대로 찍으면 같은 화면의 드롭다운과 어긋난다")
        .contains("StockPayoutWindowLabelUtil.label(row.payoutWindow())")
        .doesNotContain("<td>${row.payoutWindow()}</td>");
  }

  /** 폼 드롭다운은 계속 같은 키를 써야 한다 - 한쪽만 바뀌면 다시 두 말이 된다. */
  @Test
  void 폼_드롭다운도_같은_키를_쓴다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);
    assertThat(template)
        .contains("stock.page.dividend.monthly.reference.profile.payout.window.mid.month")
        .contains("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
