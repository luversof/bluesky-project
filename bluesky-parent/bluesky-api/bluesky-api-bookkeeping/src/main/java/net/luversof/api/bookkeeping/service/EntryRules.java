package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;

import net.luversof.api.bookkeeping.constant.AssetKind;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Category;

/**
 * 내역 검증 규칙(순수 함수). 자산 · 분류는 서비스가 같은 가계부에서 찾아 넘긴다(없으면 null).
 *
 * <p>DB 의 ck_entry_typeColumns 와 같은 칼럼 조합을 먼저 막고, DB 가 못 보는 통화 · 분류 종류를 여기서 본다.
 */
final class EntryRules {

  static final int MAX_CONTENT = 200;
  static final int MAX_MEMO = 1000;
  static final int MIN_INSTALLMENT = 2;
  static final int MAX_INSTALLMENT = 36;

  private EntryRules() {}

  /**
   * 검증하고 저장할 toAmount 를 돌려준다(수입 · 지출은 null, 같은 통화 이체에서 비우면 amount).
   *
   * @param asset 나가는(수입이면 받는) 자산
   * @param toAsset 이체에서 받는 자산, 이체가 아니면 null
   * @param category 분류, 이체면 null
   */
  static BigDecimal check(EntryCommand c, Asset asset, Asset toAsset, Category category) {
    if (c.type() == null || c.entryDate() == null || c.assetId() == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    if (length(c.content()) > MAX_CONTENT || length(c.memo()) > MAX_MEMO) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    if (asset == null) {
      throw BookkeepingErrorCode.NOT_EXIST_ASSET.exception();
    }
    // 수입 · 지출은 0원도 받는다(복지카드 등). 이체 0원은 뜻이 없다
    var amount =
        c.type() == EntryType.TRANSFER
            ? Money.positive(c.amount(), asset.currency())
            : Money.nonNegative(c.amount(), asset.currency());
    if (c.installmentMonths() != null
        && (c.type() != EntryType.EXPENSE
            || asset.kind() != AssetKind.CREDIT_CARD
            || c.installmentMonths() < MIN_INSTALLMENT
            || c.installmentMonths() > MAX_INSTALLMENT)) {
      throw BookkeepingErrorCode.INVALID_INSTALLMENT.exception();
    }

    if (c.type() != EntryType.TRANSFER) {
      if (c.toAssetId() != null || c.toAmount() != null || c.categoryId() == null) {
        throw BookkeepingErrorCode.INVALID_ENTRY_COLUMNS.exception();
      }
      if (category == null) {
        throw BookkeepingErrorCode.NOT_EXIST_CATEGORY.exception();
      }
      if (category.kind() != c.type().categoryKind()) {
        throw BookkeepingErrorCode.CATEGORY_KIND_MISMATCH.exception();
      }
      return null;
    }

    if (c.categoryId() != null || c.toAssetId() == null || c.toAssetId().equals(c.assetId())) {
      throw BookkeepingErrorCode.INVALID_ENTRY_COLUMNS.exception();
    }
    if (toAsset == null) {
      throw BookkeepingErrorCode.NOT_EXIST_ASSET.exception();
    }
    boolean sameCurrency = asset.currency().equals(toAsset.currency());
    if (c.toAmount() == null) {
      if (!sameCurrency) {
        throw BookkeepingErrorCode.INVALID_AMOUNT.exception();
      }
      return amount;
    }
    var toAmount = Money.positive(c.toAmount(), toAsset.currency());
    if (sameCurrency && toAmount.compareTo(amount) != 0) {
      throw BookkeepingErrorCode.TRANSFER_AMOUNT_MISMATCH.exception();
    }
    return toAmount;
  }

  private static int length(String s) {
    return s == null ? 0 : s.length();
  }
}
