package net.luversof.web.gate.stock.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.TaxYearInputUpsertRequest;
import net.luversof.web.gate.stock.httpexchange.DividendTaxReportClient;
import net.luversof.web.gate.stock.service.DividendTaxReportSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 배당 화면 "종소세 신고 자료" 구역의 입력값(다른 종합소득 과세표준 · 기타 금융소득) 저장. */
@Controller
@RequestMapping(value = "/stock/dividend/tax-input", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendTaxViewController {

  private static final Logger log = LoggerFactory.getLogger(StockDividendTaxViewController.class);

  @Autowired private DividendTaxReportClient dividendTaxReportClient;

  @PostMapping
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
    return "redirect:/stock/dividend?taxYear=" + year + "#tax-report";
  }
}
