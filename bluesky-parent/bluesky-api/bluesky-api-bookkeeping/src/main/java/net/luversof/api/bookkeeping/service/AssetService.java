package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.AssetKind;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.EntryQuery;
import net.luversof.api.bookkeeping.repository.EntryQuery.CardExpense;

/** 자산 목록(잔액) · 추가 · 수정 · 삭제(내역 있으면 숨김) · 순서. MEMBER 이상. */
@Service
public class AssetService {

  /** 자산과 현재 잔액(openingBalance + 내역 증감, 자산 통화). */
  public record AssetBalance(Asset asset, BigDecimal balance) {}

  /** 추가 · 수정 입력. paymentDay 는 CREDIT_CARD 만 쓰고 나머지는 무시. hidden 이 null 이면 그대로. */
  public record AssetCommand(
      AssetKind kind,
      String name,
      String currency,
      BigDecimal openingBalance,
      Integer paymentDay,
      Boolean hidden) {}

  private final AssetRepository assetRepository;
  private final EntryQuery entryQuery;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;

  public AssetService(
      AssetRepository assetRepository,
      EntryQuery entryQuery,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess) {
    this.assetRepository = assetRepository;
    this.entryQuery = entryQuery;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
  }

  /** 숨긴 자산도 포함한다(화면이 거른다). */
  /** 결제 달 하나의 카드별 예정액(일시불 · 할부 몫). 숨긴 카드도 포함한다. */
  public record CardBill(
      UUID assetId,
      YearMonth month,
      BigDecimal lumpSum,
      BigDecimal installments,
      BigDecimal total) {}

  public List<CardBill> cardBills(UUID bookId, UUID userId, YearMonth month) {
    bookAccess.requireMember(bookId, userId);
    if (month == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var currencies = new LinkedHashMap<UUID, String>();
    assetRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
        .filter(a -> a.kind() == AssetKind.CREDIT_CARD)
        .forEach(a -> currencies.put(a.id(), a.currency()));
    var expenses =
        currencies.isEmpty()
            ? List.<CardExpense>of()
            : entryQuery.cardExpenses(
                bookId,
                CardBills.firstUsageMonth(month).atDay(1),
                month.minusMonths(1).atEndOfMonth());
    return CardBills.bills(month, currencies, expenses).stream()
        .map(b -> new CardBill(b.assetId(), b.month(), b.lumpSum(), b.installments(), b.total()))
        .toList();
  }

  public List<AssetBalance> list(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    var deltas = entryQuery.balanceDeltas(bookId);
    return assetRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
        .map(a ->
            new AssetBalance(
                a, a.openingBalance().add(deltas.getOrDefault(a.id(), BigDecimal.ZERO))))
        .toList();
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Asset create(UUID bookId, UUID userId, AssetCommand command) {
    bookAccess.requireMember(bookId, userId);
    if (command.kind() == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var currency = Money.currency(command.currency());
    var siblings = assetRepository.findByBookIdOrderByDisplayOrder(bookId);
    var order = siblings.stream().mapToInt(Asset::displayOrder).max().orElse(-1) + 1;
    return jdbcAggregateOperations.insert(
        new Asset(
            uuidGenerator.create(),
            bookId,
            command.kind(),
            BookService.name(command.name()),
            currency,
            Money.balance(command.openingBalance(), currency),
            paymentDay(command.kind(), command.paymentDay()),
            order,
            Boolean.TRUE.equals(command.hidden())));
  }

  /** 내역이 있으면 통화를 바꿀 수 없다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Asset update(UUID bookId, UUID userId, UUID assetId, AssetCommand command) {
    bookAccess.requireMember(bookId, userId);
    var asset = find(bookId, assetId);
    if (command.kind() == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var currency = Money.currency(command.currency());
    if (!currency.equals(asset.currency()) && entryQuery.existsByAssetId(assetId)) {
      throw BookkeepingErrorCode.ASSET_CURRENCY_LOCKED.exception();
    }
    return assetRepository.save(
        new Asset(
            asset.id(),
            bookId,
            command.kind(),
            BookService.name(command.name()),
            currency,
            Money.balance(command.openingBalance(), currency),
            paymentDay(command.kind(), command.paymentDay()),
            asset.displayOrder(),
            command.hidden() == null ? asset.hidden() : command.hidden()));
  }

  /** 내역이 없으면 지우고 true, 있으면 숨기고 false. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public boolean delete(UUID bookId, UUID userId, UUID assetId) {
    bookAccess.requireMember(bookId, userId);
    var asset = find(bookId, assetId);
    if (entryQuery.existsByAssetId(assetId)) {
      assetRepository.save(withOrder(asset, asset.displayOrder(), true));
      return false;
    }
    assetRepository.delete(asset);
    return true;
  }

  /** 같은 kind 안에서 한 칸 위(up) / 아래로. 끝이면 그대로. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void move(UUID bookId, UUID userId, UUID assetId, boolean up) {
    bookAccess.requireMember(bookId, userId);
    var asset = find(bookId, assetId);
    var group =
        new ArrayList<>(
            assetRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
                .filter(a -> a.kind() == asset.kind())
                .toList());
    if (Ordering.swap(group, Asset::id, assetId, up)) {
      saveOrders(group);
    }
  }

  Asset find(UUID bookId, UUID assetId) {
    return assetRepository
        .findById(assetId)
        .filter(a -> a.bookId().equals(bookId))
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSET::exception);
  }

  // 순서는 그룹 안 위치를 기준으로 다시 매긴다(같은 값이 겹쳐도 바로잡힌다). 다른 그룹과 값이 겹쳐도 화면은 kind 별로 묶어 보여 준다.
  private void saveOrders(List<Asset> group) {
    for (int i = 0; i < group.size(); i++) {
      var a = group.get(i);
      if (a.displayOrder() != i) {
        assetRepository.save(withOrder(a, i, a.hidden()));
      }
    }
  }

  private static Asset withOrder(Asset a, int order, boolean hidden) {
    return new Asset(
        a.id(),
        a.bookId(),
        a.kind(),
        a.name(),
        a.currency(),
        a.openingBalance(),
        a.paymentDay(),
        order,
        hidden);
  }

  private static Integer paymentDay(AssetKind kind, Integer paymentDay) {
    if (kind != AssetKind.CREDIT_CARD || paymentDay == null) {
      return null;
    }
    if (paymentDay < 1 || paymentDay > 31) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    return paymentDay;
  }
}
