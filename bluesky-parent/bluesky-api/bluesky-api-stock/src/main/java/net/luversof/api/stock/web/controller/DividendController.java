package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.response.DividendResponse;

@RestController
@RequestMapping("/api/dividend")
public class DividendController {

	@Autowired
	private DividendService dividendService;

	@GetMapping
	public List<DividendResponse> findDividends(DividendSearchRequest request) {
		// Map domain Dividend to API response DTO DividendResponse
		return dividendService.findDividends(request).stream()
				.map(d -> new DividendResponse(
						d.getId(),
						d.getAccountId(),
						d.getStockItemId(),
						d.getStockItemName(),
						d.getType(),
						d.getQuantity(),
						d.getAmountPerShare(),
						d.getTaxPerShare(),
						d.getGrossAmount(),
						d.getFee(),
						d.getTax(),
						d.getNetAmount(),
						d.getRecordDate(),
						d.getPayDate()))
				.toList();
	}
}
