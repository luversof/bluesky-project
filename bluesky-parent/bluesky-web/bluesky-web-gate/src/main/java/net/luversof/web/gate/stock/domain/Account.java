package net.luversof.web.gate.stock.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Account(
    UUID id, UUID userId, String name, Instant createdDate, Map<String, Object> jsonConfig) {

  /**
   * 세금을 떼지 않는 계좌(ISA · 연금 등)인가 - 계좌 설정의 isTaxDeferred. api-stock 이 계좌 이름에서 파생해 넣는다. 배당 화면 · 금융소득 기준
   * · 월배당 과세 비율이 같은 판정을 쓴다(예전엔 셋이 같은 식을 따로 적었다 - 2026-10-22 정리).
   */
  public boolean isTaxDeferred() {
    return jsonConfig != null && Boolean.TRUE.equals(jsonConfig.get("isTaxDeferred"));
  }
}
