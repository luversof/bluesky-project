package net.luversof.web.gate.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 비과세 계좌 판정 한 곳(2026-10-22 정리 - 배당 화면 · 금융소득 기준 · 월배당 과세 비율이 같은 식을 셋이 따로 적고 있었다).
 *
 * <p>값은 api-stock 이 계좌 이름에서 파생해 jsonConfig.isTaxDeferred 에 넣는다. 참(Boolean.TRUE)일 때만 비과세다 - 문자열
 * "true" 나 빈 설정은 아니다(예전 세 사본과 같은 규칙).
 */
class AccountTaxDeferredTest {

  private static Account account(Map<String, Object> jsonConfig) {
    return new Account(UUID.randomUUID(), UUID.randomUUID(), "계좌", null, jsonConfig);
  }

  @Test
  void 설정이_참일_때만_비과세다() {
    assertThat(account(Map.of("isTaxDeferred", Boolean.TRUE)).isTaxDeferred()).isTrue();
    assertThat(account(Map.of("isTaxDeferred", Boolean.FALSE)).isTaxDeferred()).isFalse();
    assertThat(account(Map.of("isTaxDeferred", "true")).isTaxDeferred()).as("문자열은 아니다").isFalse();
    assertThat(account(Map.of("manualPrincipalAmount", 1)).isTaxDeferred()).isFalse();
    assertThat(account(new HashMap<>()).isTaxDeferred()).isFalse();
    assertThat(account(null).isTaxDeferred()).isFalse();
  }
}
