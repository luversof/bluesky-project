package net.luversof.web.gate.stock.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.TaxYearInputUpsertRequest;
import net.luversof.web.gate.stock.httpexchange.DividendTaxReportClient;
import net.luversof.web.gate.stock.service.DividendTaxReportSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 배당 화면의 "종소세 신고" 탭(사용자 요청 2026-11-28: 배당 내역 조회와 성격이 달라 탭으로 나눈다). 귀속연도 신고 자료를 보이고, 입력값(다른 종합소득 과세표준
 * · 기타 금융소득)을 저장한다.
 */
@Controller
@RequestMapping(value = "/stock/dividend", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendTaxViewController {

  private static final Logger log = LoggerFactory.getLogger(StockDividendTaxViewController.class);

  @Autowired private DividendTaxReportClient dividendTaxReportClient;

  @Autowired private DividendTaxReportSupport dividendTaxReportSupport;

  @BlueskyPreAuthorize
  @GetMapping("/tax")
  public String taxPage(
      HttpServletRequest request, Model model, @RequestParam(required = false) Integer taxYear) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    model.addAttribute("taxReport", dividendTaxReportSupport.load(UserUtil.getUserId(), taxYear));
    return "stock/dividendTax";
  }

  @PostMapping("/tax-input")
  public String save(
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      @ModelAttribute TaxYearInputUpsertRequest form) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    UUID userId = UserUtil.getUserId();
    form.setUserId(userId);
    try {
      dividendTaxReportClient.saveInput(form);
      redirectAttributes.addFlashAttribute("taxInputResult", "saved");
    } catch (RuntimeException e) {
      log.warn("tax year input save failed: year={}", form.getYear(), e);
      redirectAttributes.addFlashAttribute("taxInputResult", "save-failed");
    }
    // 고를 수 있는 해만 싣는다(주소에 아무 값이나 실리지 않게) - 범위 밖이면 화면이 직전 연도로 돌린다.
    int year =
        DividendTaxReportSupport.resolveYear(
            form.getYear(), LocalDate.now(DividendTaxReportSupport.TAX_ZONE));
    return "redirect:/stock/dividend/tax?taxYear=" + year;
  }
}
