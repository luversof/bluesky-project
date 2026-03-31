package net.luversof.api.board.support;

import io.github.luversof.boot.jdbc.datasource.support.RoutingDataSourceLookupKeyResolver;
import org.springframework.stereotype.Component;

@Component
public class BoardRoutingDataSourceLookupKeyResolver implements RoutingDataSourceLookupKeyResolver {

    @Override
    public String getLookupKey() {
        return "board_postgresql";
    }
}
