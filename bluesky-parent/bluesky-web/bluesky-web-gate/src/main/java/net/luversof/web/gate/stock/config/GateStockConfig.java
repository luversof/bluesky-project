package net.luversof.web.gate.stock.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Configuration
@ImportHttpServices(group = "client-stock", types = { 
		AccountClient.class,
		DividendClient.class,
		StockItemClient.class,
		TradeProfitClient.class
})
public class GateStockConfig {

}
