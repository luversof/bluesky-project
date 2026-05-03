package net.luversof.api.stock.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemTag;

public interface StockItemTagRepository extends CrudRepository<StockItemTag, UUID> {}
