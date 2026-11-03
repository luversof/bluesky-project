package net.luversof.web.gate.bookkeeping.httpexchange;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.bookkeeping.constant.BookRole;

/** api-bookkeeping 멤버 · 초대. 멤버 이름은 BoardUserInfoService.fetchUsernames 로 붙인다. */
@HttpExchange
public interface MemberClient {

  record BookMember(UUID bookId, UUID userId, BookRole role, Instant joinedDate) {}

  record BookInvite(
      String token,
      UUID bookId,
      UUID createdBy,
      Instant createdDate,
      Instant expiresAt,
      UUID usedBy,
      Instant usedDate) {}

  record InvitePreview(UUID bookId, String bookName, boolean expired, boolean used) {}

  @GetExchange("/api/books/{bookId}/members")
  List<BookMember> members(@PathVariable UUID bookId, @RequestParam UUID userId);

  /** 본인이면 나가기, 남이면 추방(OWNER 만). */
  @DeleteExchange("/api/books/{bookId}/members/{memberUserId}")
  void remove(
      @PathVariable UUID bookId, @PathVariable UUID memberUserId, @RequestParam UUID userId);

  @PostExchange("/api/books/{bookId}/invites")
  BookInvite createInvite(@PathVariable UUID bookId, @RequestParam UUID userId);

  @GetExchange("/api/invites/{token}")
  InvitePreview preview(@PathVariable String token);

  @PostExchange("/api/invites/{token}/accept")
  BookMember accept(@PathVariable String token, @RequestParam UUID userId);
}
