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
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.support.AssetGroups;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 자산 화면: 종류별 자산 · 잔액. 자산을 누르면 월 화면의 자산 필터로 그 자산의 내역을 본다. 편집은 설정 화면에서. */
@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingAssetViewController {

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
    model.addAttribute("page", AssetGroups.of(assetClient.list(bookId, userId)));
    return "bookkeeping/assets";
  }
}
