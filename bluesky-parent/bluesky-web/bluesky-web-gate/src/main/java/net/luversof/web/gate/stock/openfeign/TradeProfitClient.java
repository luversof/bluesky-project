package net.luversof.web.gate.stock.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;

@FeignClient(name = "bluesky-api-stock", contextId="api-stock-tradeprofit", path = "/api/tradeProfit", url = "${gate.feign-client.url.stock:}")
public interface TradeProfitClient {

	@PostMapping("/calculateProfit")
	List<TradeProfit> calculateProfit(@RequestBody TradeProfitRequest request);
	
}