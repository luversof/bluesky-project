package net.luversof.api.stock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.AccountService;
import net.luversof.api.stock.service.StockItemService;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

class TradeProfitTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(TradeProfitTest.class);

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	AccountService accountService;

	@Autowired
	private TradeProfitService tradeProfitService;

	@Autowired
	private StockItemService stockItemService;

	@Test
	void calculateTradeProfitByStock() {
		var account = accountService.findByUserId(TestConstant.USER_ID).get(0);

		var stockItem = stockItemService.findByName("현대차");
		var tradeList = tradeRepository.findByAccountIdInAndStockItemIdIn(List.of(account.getId()),
				List.of(stockItem.getId()));
		var request = new TradeProfitRequest(TestConstant.USER_ID, List.of(account.getId()), List.of(stockItem.getId()), null, null, null);
		var tradeProfitList = tradeProfitService.calculateProfitByStock(tradeList, request);
		log.debug("tradeProfitList : {}", tradeProfitList);
	}

	@Test
	void testCalculateHoldingProfit() {
	}

}