package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 월배당 스냅샷 붙여넣기 파싱을 고정한다.
 *
 * <p>가장 위험한 것은 <b>조용히 잘못 저장되는 것</b>이었다. 콤마로 구분한 줄에 천단위 콤마가 있으면 열이 갈라져 값이 한 칸씩 밀리는데, 밀린 값도 전부 숫자라 아무
 * 오류 없이 통과했다 &mdash; 실측: {@code 005930,2026-08-22,100,95,50,1,000,71,887} 이 9 열이 되어 보유 수량 {@code
 * 1,000 -> 1}, 매수 평단가 {@code 71,887 -> 000}(=0) 으로 저장되고 뒤의 {@code 71}, {@code 887} 은 버려졌다.
 *
 * <p>탭으로 구분한 줄은 콤마가 값 안에 그대로 남아 안전하다(파서가 숫자에서 콤마를 떼어낸다).
 */
class BulkInputParseTest {

  private final StockViewController controller = new StockViewController();
  private static final UUID USER = UUID.randomUUID();

  private static final String HEADER = "종목코드\t기준일\t최근\t평균\t비중\t수량\t평단가";

  @Test
  void 탭_구분은_천단위_콤마가_있어도_정확히_읽는다() {
    var rows = controller.parseBulkInput("005930\t2026-08-22\t100\t95\t50\t1,000\t71,887", USER);

    assertThat(rows).hasSize(1);
    var row = rows.get(0);
    assertThat(row.getSymbol()).isEqualTo("005930");
    assertThat(row.getAsOfDate()).hasToString("2026-08-22");
    assertThat(row.getHeldQuantity()).isEqualTo(1000);
    assertThat(row.getAverageBuyPrice()).isEqualByComparingTo("71887");
  }

  @Test
  void 콤마_구분도_천단위_콤마가_없으면_읽는다() {
    var rows = controller.parseBulkInput("005930,2026-08-22,100,95,50,1000,71887", USER);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getHeldQuantity()).isEqualTo(1000);
    assertThat(rows.get(0).getAverageBuyPrice()).isEqualByComparingTo("71887");
  }

  /** 이게 예전에 조용히 틀린 값을 저장하던 경우다. */
  @Test
  void 콤마_구분에_천단위_콤마가_있으면_거부한다() {
    assertThatThrownBy(
            () -> controller.parseBulkInput("005930,2026-08-22,100,95,50,1,000,71,887", USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("천단위 콤마");
  }

  @Test
  void 열이_모자라면_거부한다() {
    assertThatThrownBy(() -> controller.parseBulkInput("005930\t2026-08-22\t100", USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7개 열");
  }

  @Test
  void 헤더_줄은_건너뛴다() {
    var rows =
        controller.parseBulkInput(HEADER + "\n005930\t2026-08-22\t100\t95\t50\t1000\t71887", USER);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSymbol()).isEqualTo("005930");
  }

  @Test
  void 빈_줄은_건너뛰고_빈_입력은_거부한다() {
    var rows =
        controller.parseBulkInput("\n005930\t2026-08-22\t100\t95\t50\t1000\t71887\n\n", USER);
    assertThat(rows).hasSize(1);

    assertThatThrownBy(() -> controller.parseBulkInput("   ", USER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 종목코드는_대문자로_정규화된다() {
    var rows = controller.parseBulkInput("aapl\t2026-08-22\t100\t95\t50\t1000\t71887", USER);
    assertThat(rows.get(0).getSymbol()).isEqualTo("AAPL");
    assertThat(rows.get(0).getUserId()).isEqualTo(USER);
  }

  @Test
  void 날짜는_점과_슬래시_표기도_받는다() {
    assertThat(
            controller
                .parseBulkInput("005930\t2026/08/22\t100\t95\t50\t1000\t71887", USER)
                .get(0)
                .getAsOfDate())
        .hasToString("2026-08-22");
    assertThat(
            controller
                .parseBulkInput("005930\t2026.08.22\t100\t95\t50\t1000\t71887", USER)
                .get(0)
                .getAsOfDate())
        .hasToString("2026-08-22");
  }

  @Test
  void 보유수량이_0이면_거부한다() {
    assertThatThrownBy(
            () -> controller.parseBulkInput("005930\t2026-08-22\t100\t95\t50\t0\t71887", USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("보유 수량");
  }

  /** 자바 split 은 뒤쪽 빈 열을 버린다. 마지막 값이 비면 열이 모자란 것으로 잡혀야 한다. */
  @Test
  void 마지막_열이_비면_열_부족으로_잡힌다() {
    assertThat(controller.splitBulkColumns("a\tb\tc\t")).hasSize(3);
    assertThatThrownBy(
            () -> controller.parseBulkInput("005930\t2026-08-22\t100\t95\t50\t1000\t", USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7개 열");
  }
}
