package net.luversof.web.gate.stock.httpexchange;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.stock.domain.StockItem;

@HttpExchange(url = "/api/stockItem", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface StockItemClient {

  @PostExchange
  StockItem createStockItem(@RequestBody StockItem stockItem);

  @GetExchange("/{id}")
  Optional<StockItem> getStockItemById(@PathVariable UUID id);

  @GetExchange("/search/findByName/{name}")
  StockItem findByName(@PathVariable String name);

  @GetExchange("/search/findAll")
  java.util.List<StockItem> getStockItems();
}
