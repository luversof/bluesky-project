package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.bookkeeping.httpexchange.BookkeepingClient;

@RestController
@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingApiController {

  private BookkeepingClient bookkeepingClient;

  @Autowired
  public void setBookkeepingClient(BookkeepingClient bookkeepingClient) {
    this.bookkeepingClient = bookkeepingClient;
  }

  @BlueskyPreAuthorize
  @PostMapping
  public Bookkeeping create(@RequestBody Bookkeeping bookkeeping) {
    return bookkeepingClient.create(
        bookkeeping.toBuilder().userId(UserUtil.requireUserId()).build());
  }

  @BlueskyPreAuthorize
  @GetMapping
  public List<Bookkeeping> findByUserId() {
    // 세션 사용자만 - 예전엔 ?userId= 를 그대로 넘겨 남의 가계부를 물을 수 있었다(2026-10-28 감사)
    return bookkeepingClient.findByUserId(UserUtil.requireUserId().toString());
  }

  @BlueskyPreAuthorize
  @PutMapping
  public Bookkeeping update(@RequestBody Bookkeeping bookkeeping) {
    return bookkeepingClient.update(
        bookkeeping.toBuilder().userId(UserUtil.requireUserId()).build());
  }

  @BlueskyPreAuthorize
  @DeleteMapping
  public void delete(@RequestBody Bookkeeping bookkeeping) {
    bookkeepingClient.delete(bookkeeping.toBuilder().userId(UserUtil.requireUserId()).build());
  }
}
