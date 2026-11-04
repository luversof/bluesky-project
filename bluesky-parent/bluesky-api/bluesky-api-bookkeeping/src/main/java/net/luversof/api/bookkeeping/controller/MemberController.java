package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.BookInvite;
import net.luversof.api.bookkeeping.domain.BookMember;
import net.luversof.api.bookkeeping.service.MemberService;
import net.luversof.api.bookkeeping.service.MemberService.InvitePreview;

/** 멤버 · 초대. */
@RestController
public class MemberController {

  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping(value = "/api/books/{bookId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BookMember> members(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return memberService.members(bookId, userId);
  }

  /** 본인이면 나가기, 남이면 추방(OWNER 만). */
  @DeleteMapping("/api/books/{bookId}/members/{memberUserId}")
  public void remove(
      @PathVariable UUID bookId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
    memberService.remove(bookId, userId, memberUserId);
  }

  /** OWNER 넘기기(OWNER 만). 나는 MEMBER 가 된다. */
  @PostMapping(
      value = "/api/books/{bookId}/members/{memberUserId}/owner",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public BookMember transferOwner(
      @PathVariable UUID bookId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
    return memberService.transferOwner(bookId, userId, memberUserId);
  }

  @PostMapping(value = "/api/books/{bookId}/invites", produces = MediaType.APPLICATION_JSON_VALUE)
  public BookInvite createInvite(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return memberService.createInvite(bookId, userId);
  }

  /** 로그인 확인은 게이트가 한다. 가계부 이름과 상태만 알려 준다. */
  @GetMapping(value = "/api/invites/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
  public InvitePreview preview(@PathVariable String token) {
    return memberService.preview(token);
  }

  @PostMapping(value = "/api/invites/{token}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
  public BookMember accept(@PathVariable String token, @RequestParam UUID userId) {
    return memberService.accept(token, userId);
  }
}
