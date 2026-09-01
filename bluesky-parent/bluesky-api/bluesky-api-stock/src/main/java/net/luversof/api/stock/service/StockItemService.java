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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemTag;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockItemTagRepository;

@Service
public class StockItemService {

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockItemTagRepository stockItemTagRepository;

  @Autowired
  private net.luversof.api.stock.repository.StockPriceHistoryRepository stockPriceHistoryRepository;

  public void setStockPriceHistoryRepository(
      net.luversof.api.stock.repository.StockPriceHistoryRepository stockPriceHistoryRepository) {
    this.stockPriceHistoryRepository = stockPriceHistoryRepository;
  }

  /**
   * 한 종목의 일별 종가. 종목 상세의 주가 차트가 쓴다.
   *
   * <p>보유 평가액 추이 차트만으로는 <b>주가 자체</b>를 볼 수 없다 &mdash; 평가액은 수량이 바뀌면 같이 움직이므로, 산 뒤로 주가가 어떻게 됐는지는 그 선에서
   * 읽어낼 수 없다.
   *
   * <p>기간을 주지 않으면 전 구간이다.
   */
  public java.util.List<net.luversof.api.stock.web.dto.response.StockPriceHistoryPoint>
      findDailyClosePrices(
          java.util.UUID stockItemId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
    if (stockItemId == null) {
      return java.util.List.of();
    }
    java.util.List<net.luversof.api.stock.web.dto.response.StockPriceHistoryPoint> points =
        new java.util.ArrayList<>();
    for (var row :
        stockPriceHistoryRepository.findDailyClosePrices(stockItemId, startDate, endDate)) {
      points.add(
          new net.luversof.api.stock.web.dto.response.StockPriceHistoryPoint(
              row.tradeDate(), row.closePrice()));
    }
    return points;
  }

  public void setStockItemRepository(StockItemRepository stockItemRepository) {
    this.stockItemRepository = stockItemRepository;
  }

  /**
   * 종목을 만든다.
   *
   * <p>심볼과 이름은 반드시 있어야 한다. 심볼이 비면 시세 조회({@code findBySymbol})와 월배당 지급이력 등록이 그 종목을 영영 찾지 못하는데, 저장
   * 자체는 성공해서 화면에는 이름 없는 종목만 남는다.
   */
  @CacheEvict(value = "stockItems", allEntries = true)
  public StockItem createStockItem(StockItem stockItem) {
    if (stockItem == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockItem is required");
    }
    if (!StringUtils.hasText(stockItem.getSymbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }
    if (!StringUtils.hasText(stockItem.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
    }
    return stockItemRepository.save(stockItem);
  }

  /**
   * 캐시는 "찾은 것"만 담는다.
   *
   * <p>{@code stockItems} 캐시는 TTL 도 크기 제한도 없는 {@code ConcurrentHashMap} 이라, 못 찾은 결과까지 담으면 존재하지 않는
   * 키를 부르는 만큼 <b>무한히 자란다</b>. 실측: 임의 UUID 100 개를 조회하자 100 개가 전부 캐시에 남아 재조회 시 DB 를 한 번도 타지 않았다. 아이디는
   * 호출자가 얼마든지 만들어낼 수 있으므로 미스를 담아서는 안 된다.
   *
   * <p>주의: {@code unless} 를 평가할 때 스프링은 {@code Optional} 을 이미 벗겨낸다. {@code #result} 는 {@link
   * StockItem} 이거나 {@code null} 이므로 {@code #result.isPresent()} 를 쓰면 {@code
   * SpelEvaluationException} 으로 500 이 난다 (실측: EL1004E Method isPresent() cannot be found on type
   * StockItem).
   */
  @Cacheable(value = "stockItems", key = "#id", unless = "#result == null")
  public Optional<StockItem> findById(UUID id) {
    return stockItemRepository.findById(id).map(this::attachTags);
  }

  /**
   * 이름으로 찾는다. {@link #findById(UUID)} 와 같은 이유로 못 찾은 결과는 담지 않는다.
   *
   * <p>실측: 존재하지 않는 이름 200 개를 조회하자 200 개가 전부 캐시에 남았다(재조회 DB 획득 0). 이름은 화면 검색으로 들어오는 값이라 특히 위험하다.
   */
  @Cacheable(value = "stockItems", key = "#name", unless = "#result == null")
  public StockItem findByName(String name) {
    return attachTags(stockItemRepository.findByName(name));
  }

  public Iterable<StockItem> findAllById(Iterable<UUID> ids) {
    List<StockItem> list = new ArrayList<>();
    stockItemRepository.findAllById(ids).forEach(list::add);
    attachTags(list);
    return list;
  }

  /**
   * 태그를 붙이지 않는 종목 조회.
   *
   * <p>손익·시뮬레이션 경로는 종목을 심볼/이름(그룹 키)으로만 쓰는데, {@link #findAllById} 는 늘 태그 테이블을 한 번 더 읽는다(실측:
   * holdingsSnapshotBatch 스택 샘플의 15% 가 attachTags). 그 경로에서는 이 메서드를 쓴다.
   */
  public Iterable<StockItem> findAllByIdWithoutTags(Iterable<UUID> ids) {
    return stockItemRepository.findAllById(ids);
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
