package net.luversof.web.gate.bookkeeping.httpexchange;

import java.util.List;
import net.luversof.web.gate.bookkeeping.domain.AssetType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "/api/bookkeeping/assetGroup", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface AssetGroupClient {

    @PostExchange
    AssetType create(@RequestBody AssetType assetGroup);

    @GetExchange
    List<AssetType> findByBookkeepingId(@RequestParam String bookkeepingId);

    @PutExchange
    AssetType update(@RequestBody AssetType assetGroup);

    @DeleteExchange
    void delete(@RequestBody AssetType assetGroup);
}
