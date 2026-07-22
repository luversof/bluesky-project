package net.luversof.api.stock.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemTag;

public interface StockItemTagRepository extends CrudRepository<StockItemTag, UUID> {

  /** 지정 종목들의 태그만 조회. (기존엔 findAll 후 자바 필터라 호출마다 태그 테이블 전량을 읽었다.) */
  List<StockItemTag> findByStockItemIdIn(Collection<UUID> stockItemIds);

  /** 태그 값으로 조회. uk_stockItemTag_stockItemId_tag 인덱스를 탄다. */
  List<StockItemTag> findByTag(String tag);
}
