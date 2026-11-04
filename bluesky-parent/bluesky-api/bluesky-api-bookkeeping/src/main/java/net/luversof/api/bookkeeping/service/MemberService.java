package net.luversof.api.bookkeeping.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.BookInvite;
import net.luversof.api.bookkeeping.domain.BookMember;
import net.luversof.api.bookkeeping.repository.BookInviteRepository;
import net.luversof.api.bookkeeping.repository.BookMemberRepository;
import net.luversof.api.bookkeeping.repository.BookRepository;

/** 멤버 목록 · 나가기 · 추방, 초대 링크 발급 · 미리보기 · 수락. */
@Service
public class MemberService {

  static final Duration INVITE_TTL = Duration.ofDays(7);
  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  /** 초대 링크를 연 사람에게 보여 줄 것. 다른 멤버 정보는 담지 않는다. */
  public record InvitePreview(UUID bookId, String bookName, boolean expired, boolean used) {}

  private final BookMemberRepository bookMemberRepository;
  private final BookInviteRepository bookInviteRepository;
  private final BookRepository bookRepository;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final BookAccess bookAccess;

  public MemberService(
      BookMemberRepository bookMemberRepository,
      BookInviteRepository bookInviteRepository,
      BookRepository bookRepository,
      JdbcAggregateOperations jdbcAggregateOperations,
      BookAccess bookAccess) {
    this.bookMemberRepository = bookMemberRepository;
    this.bookInviteRepository = bookInviteRepository;
    this.bookRepository = bookRepository;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.bookAccess = bookAccess;
  }

  /** OWNER 먼저. 이름은 게이트가 api-user findByIdIn 으로 붙인다. */
  public List<BookMember> members(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return bookMemberRepository.findByBookId(bookId);
  }

  /** 본인이면 나가기(OWNER 는 불가), 남이면 추방(OWNER 만). 나간 사람이 쓴 내역은 남는다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void remove(UUID bookId, UUID userId, UUID targetUserId) {
    var me = bookAccess.requireMember(bookId, userId);
    if (userId.equals(targetUserId)) {
      if (me.role() == BookRole.OWNER) {
        throw BookkeepingErrorCode.OWNER_CANNOT_LEAVE.exception();
      }
      bookMemberRepository.delete(bookId, userId);
      return;
    }
    bookAccess.requireOwner(bookId, userId);
    if (targetUserId == null || bookMemberRepository.delete(bookId, targetUserId) == 0) {
      throw BookkeepingErrorCode.NOT_EXIST_MEMBER.exception();
    }
  }

  /**
   * OWNER 넘기기(OWNER 만). 대상 MEMBER 가 OWNER 가 되고 나는 MEMBER 가 된다.
   *
   * <p>OWNER 는 가계부당 1명(uk_bookMember_owner)이라 내 역할을 먼저 내린 뒤 대상을 올린다. 한 트랜잭션이라 중간에 실패하면 둘 다 되돌아간다.
   */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public BookMember transferOwner(UUID bookId, UUID userId, UUID targetUserId) {
    bookAccess.requireOwner(bookId, userId);
    if (targetUserId == null || targetUserId.equals(userId)) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var target =
        bookMemberRepository
            .findByBookIdAndUserId(bookId, targetUserId)
            .orElseThrow(BookkeepingErrorCode.NOT_EXIST_MEMBER::exception);
    bookMemberRepository.updateRole(bookId, userId, BookRole.MEMBER);
    bookMemberRepository.updateRole(bookId, targetUserId, BookRole.OWNER);
    return new BookMember(bookId, targetUserId, BookRole.OWNER, target.joinedDate());
  }

  /** OWNER 만. 1회용, 7일 뒤 만료. 토큰은 32바이트 난수(base64url 43자). */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public BookInvite createInvite(UUID bookId, UUID userId) {
    bookAccess.requireOwner(bookId, userId);
    var bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    var now = Timestamps.now();
    return jdbcAggregateOperations.insert(
        new BookInvite(
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
            bookId,
            userId,
            now,
            now.plus(INVITE_TTL),
            null,
            null));
  }

  /** 로그인한 누구나(토큰을 아는 사람). */
  public InvitePreview preview(String token) {
    var invite = findInvite(token);
    var book =
        bookRepository
            .findById(invite.bookId())
            .orElseThrow(BookkeepingErrorCode.NOT_EXIST_INVITE::exception);
    return new InvitePreview(
        book.id(),
        book.name(),
        !invite.expiresAt().isAfter(Instant.now()),
        invite.usedBy() != null);
  }

  /** 수락하면 MEMBER 로 들어간다. 이미 멤버면 링크를 쓰지 않고 거절한다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public BookMember accept(String token, UUID userId) {
    if (userId == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var invite = findInvite(token);
    var now = Timestamps.now();
    if (invite.usedBy() != null) {
      throw BookkeepingErrorCode.USED_INVITE.exception();
    }
    if (!invite.expiresAt().isAfter(now)) {
      throw BookkeepingErrorCode.EXPIRED_INVITE.exception();
    }
    if (bookMemberRepository.findByBookIdAndUserId(invite.bookId(), userId).isPresent()) {
      throw BookkeepingErrorCode.ALREADY_BOOK_MEMBER.exception();
    }
    // 위 확인과 이 사이에 다른 요청이 먼저 썼으면 0 행
    if (bookInviteRepository.markUsed(token, userId, now) == 0) {
      throw BookkeepingErrorCode.USED_INVITE.exception();
    }
    var member = new BookMember(invite.bookId(), userId, BookRole.MEMBER, now);
    bookMemberRepository.insert(member);
    return member;
  }

  private BookInvite findInvite(String token) {
    if (token == null || token.isBlank()) {
      throw BookkeepingErrorCode.NOT_EXIST_INVITE.exception();
    }
    return bookInviteRepository
        .findById(token)
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_INVITE::exception);
  }
}
