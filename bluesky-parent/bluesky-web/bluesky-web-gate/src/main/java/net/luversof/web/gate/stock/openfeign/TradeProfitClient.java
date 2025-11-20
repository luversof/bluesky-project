package net.luversof.web.gate.stock.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;

@FeignClient(name = "bluesky-api-stock", contextId="api-stock-tradeprofit", path = "/api/tradeProfit", url = "${gate.feign-client.url.stock:}")
public interface TradeProfitClient {

	@GetMapping("/calculateProfit")
	List<TradeProfit> calculateProfit(@SpringQueryMap TradeProfitRequest request);

	@GetMapping("/timeSeries")
	List<TradeProfitTimeSeriesPoint> timeSeries(@SpringQueryMap TradeProfitRequest request);
	
}