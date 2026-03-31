package net.luversof.web.gate.bookkeeping.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.bookkeeping.domain.Asset;

@HttpExchange(url = "/api/asset", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface AssetClient {

    @PostExchange
    Asset create(@RequestBody Asset asset);

    @GetExchange("/search/findByBookkeepingId/{bookkeepingId}")
    List<Asset> findByBookkeepingId(@PathVariable String bookkeepingId);

    @PutExchange
    Asset update(@RequestBody Asset asset);

    @DeleteExchange
    void delete(@RequestBody Asset asset);
}
