package net.luversof.web.gate.bookkeeping.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;

@HttpExchange(url = "/api/bookkeeping", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BookkeepingClient {

    @PostExchange
    Bookkeeping create(@RequestBody Bookkeeping bookkeeping);

    @GetExchange
    List<Bookkeeping> findByUserId(@RequestParam String userId);

    @PutExchange
    Bookkeeping update(@RequestBody Bookkeeping bookkeeping);

    @DeleteExchange
    void delete(@RequestBody Bookkeeping bookkeeping);
}
