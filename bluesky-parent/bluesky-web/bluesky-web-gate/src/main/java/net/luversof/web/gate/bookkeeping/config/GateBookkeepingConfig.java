package net.luversof.web.gate.bookkeeping.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetGroupClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryGroupClient;

@Configuration
@ImportHttpServices(group = "client-bookkeeping", types = { 
		AssetClient.class,
		AssetGroupClient.class,
		BookkeepingClient.class,
		EntryClient.class,
		EntryGroupClient.class
})
public class GateBookkeepingConfig {

}
