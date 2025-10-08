package net.luversof.api.stock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.AccountService;
import net.luversof.api.stock.service.StockItemService;
import net.luversof.api.stock.service.TradeProfitService;

@Slf4j
class TradeProfitTest implements GeneralTest {
	
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
		var tradeList = tradeRepository.findByAccountIdAndStockItemId(account.getId(), stockItem.getId());
		var tradeProfitList = tradeProfitService.calculateStockProfitByStock(tradeList);
		log.debug("tradeProfitList : {}", tradeProfitList);
	}

	@Test
	void testCalculateHoldingProfit() {
	}

}