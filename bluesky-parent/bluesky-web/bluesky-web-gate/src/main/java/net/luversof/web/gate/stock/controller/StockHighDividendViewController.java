package net.luversof.web.gate.stock.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.HighDividendDesignationUpsertRequest;
import net.luversof.web.gate.stock.httpexchange.HighDividendDesignationClient;
import net.luversof.web.gate.stock.service.HighDividendAdminSupport;
import net.luversof.web.gate.stock.support.MonthlyReferenceEditors;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 관리 화면 "고배당기업 지정" 탭의 저장 · 삭제. 공용 값이라 월배당 기준 데이터와 같은 편집자 목록({@link MonthlyReferenceEditors})으로 막는다.
 */
@Controller
@RequestMapping(value = "/stock/admin/high-dividend", produces = MediaType.TEXT_HTML_VALUE)
public class StockHighDividendViewController {

  private static final Logger log = LoggerFactory.getLogger(StockHighDividendViewController.class);

  private static final String TAB_REDIRECT =
      "redirect:/stock/admin?tab=" + HighDividendAdminSupport.ADMIN_TAB_HIGH_DIVIDEND;

  @Autowired private HighDividendDesignationClient highDividendDesignationClient;

  @Autowired private MonthlyReferenceEditors monthlyReferenceEditors;

  @PostMapping
  public String save(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @RequestParam(required = false) UUID id,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) Integer businessYear,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate payDateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate payDateTo,
      @RequestParam(required = false) String note) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    monthlyReferenceEditors.requireEditor(UserUtil.getUserId());
    try {
      highDividendDesignationClient.upsert(
          new HighDividendDesignationUpsertRequest(
              id, symbol, businessYear, payDateFrom, payDateTo, note));
      redirectAttributes.addFlashAttribute("highDividendResult", "saved");
    } catch (RuntimeException e) {
      log.warn("high dividend designation save failed: symbol={}", symbol, e);
      redirectAttributes.addFlashAttribute("highDividendResult", "save-failed");
    }
    return TAB_REDIRECT;
  }

  @PostMapping("/delete")
  public String delete(
      HttpServletRequest request, RedirectAttributes redirectAttributes, @RequestParam UUID id) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    monthlyReferenceEditors.requireEditor(UserUtil.getUserId());
    try {
      highDividendDesignationClient.delete(id);
      redirectAttributes.addFlashAttribute("highDividendResult", "deleted");
    } catch (RuntimeException e) {
      log.warn("high dividend designation delete failed: id={}", id, e);
      redirectAttributes.addFlashAttribute("highDividendResult", "delete-failed");
    }
    return TAB_REDIRECT;
  }
}
