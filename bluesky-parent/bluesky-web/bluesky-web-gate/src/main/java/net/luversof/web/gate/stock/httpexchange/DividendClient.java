package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;

@HttpExchange(url = "/api/dividend", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface DividendClient {

	@GetExchange
	List<DividendResponse> findDividends(DividendRequest request);
}
