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

import net.luversof.web.gate.bookkeeping.domain.EntryGroup;

@HttpExchange(url = "/api/bookkeeping/entryGroup", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface EntryGroupClient {

  @PostExchange
  EntryGroup create(@RequestBody EntryGroup entryGroup);

  @GetExchange
  List<EntryGroup> findByBookkeepingId(@RequestParam String bookkeepingId);

  @PutExchange
  EntryGroup update(@RequestBody EntryGroup entryGroup);

  @DeleteExchange
  void delete(@RequestBody EntryGroup entryGroup);
}
