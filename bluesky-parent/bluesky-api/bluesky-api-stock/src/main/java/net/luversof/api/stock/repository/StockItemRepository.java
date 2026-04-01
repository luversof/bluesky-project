package net.luversof.api.stock.repository;

import java.util.UUID;
import net.luversof.api.stock.domain.StockItem;
import org.springframework.data.repository.CrudRepository;

public interface StockItemRepository extends CrudRepository<StockItem, UUID> {

    StockItem findByName(String name);
}
