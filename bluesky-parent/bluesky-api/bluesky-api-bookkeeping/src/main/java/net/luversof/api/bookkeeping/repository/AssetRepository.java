package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Asset;

public interface AssetRepository extends CrudRepository<Asset, UUID> {

  List<Asset> findByBookIdOrderByDisplayOrder(UUID bookId);

  long deleteByBookId(UUID bookId);
}
