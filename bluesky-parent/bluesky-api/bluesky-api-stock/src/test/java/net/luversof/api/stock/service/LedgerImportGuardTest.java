package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 원장 가져오기는 지우기 전에 쓸 만한지 본다(2026-11-02 사용자 결정 P0) - 예전엔 먼저 지우고 시트를 읽어, 시트 읽기가 실패하면 빈 원장이 저장됐다. */
class LedgerImportGuardTest {

  @Test
  void 들어갈_행이_없으면_멈춘다() {
    assertThatThrownBy(() -> StockAdminService.requireUsableImport("trade", 274, 0, 0))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("current ledger 274");
    assertThatThrownBy(() -> StockAdminService.requireUsableImport("trade", 0, 10, 0))
        .as("처음 가져오기라도 들어갈 행이 0 이면 멈춘다(머리글 이름 바뀜 등)")
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void 지금_원장의_절반도_안_되면_멈추고_그_이상이면_넣는다() {
    assertThatThrownBy(() -> StockAdminService.requireUsableImport("dividend", 228, 120, 113))
        .isInstanceOf(ResponseStatusException.class);
    assertThatCode(() -> StockAdminService.requireUsableImport("dividend", 228, 120, 114))
        .doesNotThrowAnyException();
    assertThatCode(() -> StockAdminService.requireUsableImport("trade", 0, 5, 5))
        .as("처음 가져오기")
        .doesNotThrowAnyException();
  }

  @Test
  void 종목_이름은_앞뒤_공백을_다듬어_맞춘다() {
    assertThat(StockAdminService.sameStockName("삼성전자", " 삼성전자 ")).isTrue();
    assertThat(StockAdminService.sameStockName("삼성전자", "삼성전자우")).isFalse();
    assertThat(StockAdminService.sameStockName(null, "삼성전자")).isFalse();
  }
}
