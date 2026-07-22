package net.luversof.api.stock.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemTag;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockItemTagRepository;

@Service
public class StockItemService {

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockItemTagRepository stockItemTagRepository;

  public void setStockItemRepository(StockItemRepository stockItemRepository) {
    this.stockItemRepository = stockItemRepository;
  }

  @CacheEvict(value = "stockItems", allEntries = true)
  public StockItem createStockItem(StockItem stockItem) {
    return stockItemRepository.save(stockItem);
  }

  @Cacheable(value = "stockItems", key = "#id")
  public Optional<StockItem> findById(UUID id) {
    return stockItemRepository.findById(id).map(this::attachTags);
  }

  @Cacheable(value = "stockItems", key = "#name")
  public StockItem findByName(String name) {
    return attachTags(stockItemRepository.findByName(name));
  }

  public Iterable<StockItem> findAllById(Iterable<UUID> ids) {
    List<StockItem> list = new ArrayList<>();
    stockItemRepository.findAllById(ids).forEach(list::add);
    attachTags(list);
    return list;
  }

  public List<StockItem> findAll() {
    List<StockItem> list = new ArrayList<>();
    stockItemRepository.findAll().forEach(list::add);
    attachTags(list);
    return list;
  }

  public List<StockItem> findAllByTag(String tag) {
    if (!StringUtils.hasText(tag)) {
      return List.of();
    }

    String normalizedTag = tag.trim();
    List<UUID> stockItemIds =
        stockItemTagRepository.findByTag(normalizedTag).stream()
            .map(StockItemTag::getStockItemId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (stockItemIds.isEmpty()) {
      return List.of();
    }

    List<StockItem> list = new ArrayList<>();
    stockItemRepository.findAllById(stockItemIds).forEach(list::add);
    attachTags(list);
    return list;
  }

  private StockItem attachTags(StockItem stockItem) {
    if (stockItem == null) {
      return null;
    }
    attachTags(List.of(stockItem));
    return stockItem;
  }

  private void attachTags(List<StockItem> stockItems) {
    if (stockItems == null || stockItems.isEmpty()) {
      return;
    }

    Set<UUID> stockItemIds =
        stockItems.stream()
            .map(StockItem::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (stockItemIds.isEmpty()) {
      stockItems.forEach(stockItem -> stockItem.setTags(List.of()));
      return;
    }

    // 대상 종목의 태그만 조회한다. (findAll 후 자바 필터는 호출마다 태그 테이블 전량을 읽었다 —
    // 손익/시계열 API 가 요청마다 attachTags 를 타므로 모든 요청에 얹히던 비용)
    Map<UUID, List<String>> tagsByStockItemId =
        stockItemTagRepository.findByStockItemIdIn(stockItemIds).stream()
            .filter(tag -> tag.getStockItemId() != null)
            .filter(tag -> StringUtils.hasText(tag.getTag()))
            .collect(
                Collectors.groupingBy(
                    StockItemTag::getStockItemId,
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                        Collectors.mapping(
                            stockItemTag -> stockItemTag.getTag().trim(),
                            Collectors.toCollection(LinkedHashSet::new)),
                        ArrayList::new)));

    stockItems.forEach(
        stockItem ->
            stockItem.setTags(tagsByStockItemId.getOrDefault(stockItem.getId(), List.of())));
  }
}
