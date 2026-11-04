package net.luversof.web.gate.bookkeeping.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.support.AssetGroups;
import net.luversof.web.gate.bookkeeping.support.CardBillView;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 자산 화면: 종류별 자산 · 잔액, 카드는 결제 예정액(이번 달 · 다음 달, 달력 월 기준). 자산을 누르면 월 화면의 자산 필터로 그 자산의 내역을 본다. 편집은 설정
 * 화면에서.
 */
@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingAssetViewController {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(BookkeepingAssetViewController.class);

  private final BookClient bookClient;
  private final AssetClient assetClient;

  public BookkeepingAssetViewController(BookClient bookClient, AssetClient assetClient) {
    this.bookClient = bookClient;
    this.assetClient = assetClient;
  }

  @GetMapping("/{bookId}/assets")
  public String assets(
      @PathVariable UUID bookId,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    var userId = UserUtil.requireUserId();
    try {
      model.addAttribute("book", bookClient.get(bookId, userId));
    } catch (BlueskyException e) {
      return BookkeepingViewController.forbidden(e, response, model);
    }
    var assets = assetClient.list(bookId, userId);
    model.addAttribute("page", AssetGroups.of(assets));
    if (assets.stream().anyMatch(a -> a.asset().kind() == AssetKind.CREDIT_CARD)) {
      model.addAttribute("cardBills", cardBills(bookId, userId));
    }
    return "bookkeeping/assets";
  }

  /** 곁가지 - 못 불러오면 안내만. 결제 달은 오늘이 속한 달과 다음 달. */
  private CardBillView cardBills(UUID bookId, UUID userId) {
    var month = java.time.YearMonth.now();
    try {
      return CardBillView.of(
          assetClient.cardBills(bookId, userId, month.toString()),
          assetClient.cardBills(bookId, userId, month.plusMonths(1).toString()));
    } catch (RuntimeException e) {
      log.warn("bookkeeping card bills unavailable", e);
      return CardBillView.unavailable();
    }
  }
}
