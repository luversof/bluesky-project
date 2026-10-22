package net.luversof.web.gate.stock.httpexchange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.domain.Account;

@HttpExchange(
    url = "/api/account",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface AccountClient {

  @GetExchange("/{id}")
  Optional<Account> getAccountById(@PathVariable UUID id);

  @GetExchange("/search/findByUserId/{userId}")
  List<Account> getAccountsByUserId(@PathVariable UUID userId);
}
