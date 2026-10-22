package net.luversof.api.stock.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.StockItemRepository;

/**
 * 요청에 실려 온 종목 심볼을 종목으로 바꾸고, 행들이 가리키는 종목을 한 번에 읽는다.
 *
 * <p>월배당 지급 이력 · 프로필 · 스냅샷 저장이 같은 규칙(앞뒤 공백 무시, 없으면 400)을 서비스마다 복사해 쓰고 있었다(2026-10-21 정리에서 모음). 저장소의
 * default 메서드로 두지 않은 것은 시험이 저장소를 Mockito 가짜로 바꿔 끼우기 때문이다 - 가짜는 default 메서드를 실행하지 않는다.
 */
final class StockItemLookup {

  private StockItemLookup() {}

  static StockItem requireBySymbol(StockItemRepository stockItemRepository, String symbol) {
    StockItem stockItem = stockItemRepository.findBySymbol(symbol.trim());
    if (stockItem == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown stock symbol: " + symbol);
    }
    return stockItem;
  }

  /**
   * id → 종목. null id 는 건너뛰고 남는 게 없으면 조회하지 않는다(빈 맵). 행마다 findById 하면 행 수만큼 SELECT 가 나가므로(N+1) 한 번에
   * 읽는다 - 월배당 지급 이력 · 프로필 · 스냅샷 · 카탈로그가 같은 몇 줄을 따로 들고 있었다(2026-10-22 정리).
   */
  static Map<UUID, StockItem> byIds(StockItemRepository stockItemRepository, Collection<UUID> ids) {
    Map<UUID, StockItem> byId = new HashMap<>();
    Set<UUID> nonNull = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (!nonNull.isEmpty()) {
      stockItemRepository.findAllById(nonNull).forEach(item -> byId.put(item.getId(), item));
    }
    return byId;
  }
}
