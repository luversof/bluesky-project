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

/** 가계부 멤버 · 초대 */
@RestController
public class MemberController {

  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  /**
   * 멤버 목록 조회
   *
   * <p>OWNER 가 먼저 온다. 이름은 게이트가 api-user 로 붙인다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @return 멤버 목록
   */
  @GetMapping(value = "/api/books/{bookId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BookMember> members(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return memberService.members(bookId, userId);
  }

  /**
   * 멤버 내보내기 · 나가기
   *
   * <p>본인이면 나가기, 다른 멤버면 내보내기(OWNER 만).
   */
  @DeleteMapping("/api/books/{bookId}/members/{memberUserId}")
  public void remove(
      @PathVariable UUID bookId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
    memberService.remove(bookId, userId, memberUserId);
  }

  /**
   * OWNER 넘기기
   *
   * <p>OWNER 만 할 수 있다. 넘긴 사람은 MEMBER 가 된다.
   */
  @PostMapping(
      value = "/api/books/{bookId}/members/{memberUserId}/owner",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public BookMember transferOwner(
      @PathVariable UUID bookId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
    return memberService.transferOwner(bookId, userId, memberUserId);
  }

  /**
   * 초대 링크 생성
   *
   * <p>OWNER 만 할 수 있다. 한 번만 쓸 수 있고 7일 뒤 만료된다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @return 초대(토큰 포함)
   */
  @PostMapping(value = "/api/books/{bookId}/invites", produces = MediaType.APPLICATION_JSON_VALUE)
  public BookInvite createInvite(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return memberService.createInvite(bookId, userId);
  }

  /**
   * 초대 미리보기
   *
   * <p>가계부 이름과 초대 상태만 알려 준다. 로그인 확인은 게이트가 한다.
   */
  @GetMapping(value = "/api/invites/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
  public InvitePreview preview(@PathVariable String token) {
    return memberService.preview(token);
  }

  /**
   * 초대 수락
   *
   * <p>이미 멤버면 거절하고 링크도 쓰지 않는다.
   *
   * @param token 초대 토큰
   * @param userId 수락하는 사용자
   * @return 새 멤버
   */
  @PostMapping(value = "/api/invites/{token}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
  public BookMember accept(@PathVariable String token, @RequestParam UUID userId) {
    return memberService.accept(token, userId);
  }
}
