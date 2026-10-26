package net.luversof.api.stock.util;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * id 목록을 SQL 인자(쉼표로 이은 문자열)로 바꾼다.
 *
 * <p>보유 기간 · 연도별 비용 조회가 같은 규칙(null id 는 건너뛰고, 남는 게 없으면 null = "거르지 않음")을 따로 적고 있었다(2026-10-22 정리).
 * 같은 날 월배당 카탈로그 · 최근 종가 · 원주가 채우기의 손으로 이은 세 곳도 이리로(셋 다 null 없고 비지 않은 집합이라 결과 같음).
 */
public final class StockIds {

  private StockIds() {}

  /** 쉼표로 이은 id. 목록이 없거나 null 만 있으면 null - 조회 쪽은 null 을 "조건 없음" 으로 읽는다. */
  public static String joinOrNull(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    String joined =
        ids.stream().filter(Objects::nonNull).map(UUID::toString).collect(Collectors.joining(","));
    return joined.isEmpty() ? null : joined;
  }
}
