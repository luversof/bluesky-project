package net.luversof.api.board.support;

import org.springframework.stereotype.Component;

import io.github.luversof.boot.jdbc.datasource.support.RoutingDataSourceLookupKeyResolver;

@Component
public class BoardRoutingDataSourceLookupKeyResolver implements RoutingDataSourceLookupKeyResolver {

	@Override
	public String getLookupKey() {
		return "board";
	}

}
