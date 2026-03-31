package net.luversof.api.stock.repository;

import java.util.Optional;
import java.util.UUID;
import net.luversof.api.stock.domain.StockPrice;
import org.springframework.data.repository.CrudRepository;

public interface StockPriceRepository extends CrudRepository<StockPrice, UUID> {

    Optional<StockPrice> findByStockItemId(UUID stockItemId);
}
