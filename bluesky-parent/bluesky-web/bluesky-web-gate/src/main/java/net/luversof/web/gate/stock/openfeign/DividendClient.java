package net.luversof.web.gate.stock.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import net.luversof.api.stock.web.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.request.DividendRequest;

@FeignClient(name = "bluesky-api-stock", contextId = "api-stock-dividend", path = "/api/dividend", url = "${gate.feign-client.url.stock:}")
public interface DividendClient {

	@GetMapping
	List<DividendResponse> findDividends(@SpringQueryMap DividendRequest request);
}
