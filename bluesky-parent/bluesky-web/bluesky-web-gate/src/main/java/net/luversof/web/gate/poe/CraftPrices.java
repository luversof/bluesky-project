package net.luversof.web.gate.poe;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/**
 * 사용자 가격(10-21 C529, craftofexile 'custom pricing') — 이 요청 동안의 '가격 직접 입력' 값(이름 → 카오스). 게이트 가로채기가 요청
 * 인자 prices(JSON)로 걸고 끝나면 풀며, 계산 · 쓴 화폐 · 최적 조합 요청 DTO 가 기본값으로 싣는다.
 */
public final class CraftPrices {

  private static final ThreadLocal<Map<String, Double>> CURRENT = new ThreadLocal<>();

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private CraftPrices() {}

  /** 지금 요청의 사용자 가격(없으면 null). */
  public static Map<String, Double> current() {
    return CURRENT.get();
  }

  public static void set(Map<String, Double> prices) {
    if (prices == null || prices.isEmpty()) {
      CURRENT.remove();
    } else {
      CURRENT.set(prices);
    }
  }

  public static void clear() {
    CURRENT.remove();
  }

  /** 요청 인자 JSON → 맵(이름 80자 · 값 0 초과 1천만 미만 · 60개까지). 비었거나 못 읽으면 null. */
  public static Map<String, Double> parse(String json) {
    if (json == null || json.isBlank() || json.length() > 8_000) {
      return null;
    }
    try {
      Map<String, Object> raw = JSON.readValue(json, Map.class);
      Map<String, Double> out = new LinkedHashMap<>();
      for (Map.Entry<String, Object> e : raw.entrySet()) {
        if (!(e.getValue() instanceof Number n) || e.getKey() == null || e.getKey().length() > 80) {
          continue;
        }
        double v = n.doubleValue();
        if (Double.isFinite(v) && v > 0 && v < 1e7 && out.size() < 60) {
          out.put(e.getKey(), v);
        }
      }
      return out.isEmpty() ? null : out;
    } catch (RuntimeException e) {
      return null;
    }
  }
}
