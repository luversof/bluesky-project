package net.luversof.api.bookkeeping.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.BookMember;
import net.luversof.api.bookkeeping.repository.BookMemberRepository;

/**
 * 가계부 멤버십 · 역할 검사. 모든 서비스 메서드가 맨 먼저 부른다.
 *
 * <p>api-* 는 인증이 없고 게이트가 세션 userId 를 넘긴다. 멤버가 아니면 가계부가 있는지도 알리지 않고 403 을 낸다.
 */
@Component
public class BookAccess {

  private static final int FORBIDDEN = 403;

  private final BookMemberRepository bookMemberRepository;

  public BookAccess(BookMemberRepository bookMemberRepository) {
    this.bookMemberRepository = bookMemberRepository;
  }

  public BookMember requireMember(UUID bookId, UUID userId) {
    if (bookId == null || userId == null) {
      throw BookkeepingErrorCode.NOT_BOOK_MEMBER.exceptionWithStatus(FORBIDDEN);
    }
    return bookMemberRepository
        .findByBookIdAndUserId(bookId, userId)
        .orElseThrow(() -> BookkeepingErrorCode.NOT_BOOK_MEMBER.exceptionWithStatus(FORBIDDEN));
  }

  public BookMember requireOwner(UUID bookId, UUID userId) {
    var member = requireMember(bookId, userId);
    if (member.role() != BookRole.OWNER) {
      throw BookkeepingErrorCode.NOT_BOOK_OWNER.exceptionWithStatus(FORBIDDEN);
    }
    return member;
  }
}
