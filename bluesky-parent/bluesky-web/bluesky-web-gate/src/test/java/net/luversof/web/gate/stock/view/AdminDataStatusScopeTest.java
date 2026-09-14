package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 안 쓰는 탭에서는 데이터 상태를 묻지 않는다.
 *
 * <p>{@code dataStatus} 와 {@code ledgerIntegrity} 는 관리 화면의 <b>데이터 관리</b> 탭에서만 쓰인다 (admin.jte 의
 * {@code @else} 가지에서 adminActions 조각에 넘긴다). 그런데 컨트롤러는 탭과 무관하게 늘 두 조회를 던졌고, 월배당 기준 데이터 탭에서는 받아서 버렸다.
 *
 * <p>실측 2026-09-12: 월배당 기준 데이터 탭 문서가 89ms(64~96), 데이터 관리 탭이 67ms 였다. api-stock 쪽 dataStatus 는 82ms
 * 로 그 서비스에서 가장 느린 조회다(다음이 46ms).
 */
class AdminDataStatusScopeTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java";

  @Test
  void 월배당_기준_탭에서는_묻지_않는다() throws IOException {
    String source = flatten(Files.readString(Path.of(CONTROLLER), StandardCharsets.UTF_8));
    assertThat(source)
        .as("탭을 보고 건너뛰는 조건이 있어야 한다")
        .contains(
            flatten(
                "boolean adminDataStatusNeeded = !MonthlyDividendReferenceSupport.DIVIDEND_TAB_MONTHLY_REFERENCE.equals(adminTab);"));
    assertThat(source)
        .as("그 조건이 실제로 조회를 감싸야 한다")
        .contains(flatten("if (dataStatusUserId != null && adminDataStatusNeeded) {"));
  }

  /** 화면 쪽 전제: 이 두 값은 데이터 관리 탭에서만 그려진다. 전제가 깨지면 위 최적화는 값을 지운다. */
  @Test
  void 두_값은_데이터_관리_탭에서만_그린다() throws IOException {
    String jte =
        flatten(Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8));
    assertThat(jte).contains(flatten("@if(\"monthly-reference\".equals(adminTab))"));
    assertThat(jte)
        .as("adminActions 는 @else 가지에서만 dataStatus 를 받는다")
        .contains(flatten("@else @template.stock.htmx.fragments.adminActions("));
    assertThat(jte).contains(flatten("dataStatus = dataStatus"));
    assertThat(jte).contains(flatten("ledgerIntegrity = ledgerIntegrity"));
  }

  /** spotless·들여쓰기에 묶지 않는다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
