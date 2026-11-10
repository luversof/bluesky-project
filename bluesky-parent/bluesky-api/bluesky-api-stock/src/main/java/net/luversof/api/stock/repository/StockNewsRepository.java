package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockNews;

public interface StockNewsRepository extends CrudRepository<StockNews, UUID> {

  /** idx_stockNews_publishedDate 를 탄다. 수집 중복 확인과 분석 입력 모두 최근 며칠만 본다. */
  List<StockNews> findByPublishedDateAfter(Instant publishedDate);

  List<StockNews> findByPublishedDateBetween(Instant from, Instant to);
}
