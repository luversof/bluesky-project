package net.luversof.api.stock.service.tax;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.HighDividendDesignation;
import net.luversof.api.stock.repository.HighDividendDesignationRepository;
import net.luversof.api.stock.service.AccountService;
import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.service.TaxYearInputService;
import net.luversof.api.stock.service.tax.DividendTaxReportCalculator.TaxDividend;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.response.DividendTaxReport;
import net.luversof.api.stock.web.dto.response.TaxYearInputResponse;

/** 귀속연도 종소세 신고 자료: 배당 · 계좌 · 고배당 지정 · 사용자 입력값을 읽어 계산기에 넘긴다. */
@Service
public class DividendTaxReportService {

  /** 세금 관점의 날짜는 한국 시간 지급일이다. */
  static final ZoneId TAX_ZONE = ZoneId.of("Asia/Seoul");

  @Autowired private AccountService accountService;

  @Autowired private DividendService dividendService;

  @Autowired private HighDividendDesignationRepository highDividendDesignationRepository;

  @Autowired private TaxYearInputService taxYearInputService;

  public DividendTaxReport report(UUID userId, Integer year) {
    if (userId == null) {
      StockErrorCode.INVALID_USER_ID.throwException();
    }
    TaxYearInputResponse input = taxYearInputService.find(userId, year);
    int y = input.year();

    Set<UUID> deferred = new HashSet<>();
    for (Account account : accountService.findByUserId(userId)) {
      if (account.getJsonConfig() != null
          && Boolean.TRUE.equals(account.getJsonConfig().get("isTaxDeferred"))) {
        deferred.add(account.getId());
      }
    }

    DividendSearchRequest request = new DividendSearchRequest();
    request.setUserId(userId);
    request.setStartDate(LocalDate.of(y, 1, 1).atStartOfDay(TAX_ZONE).toInstant());
    request.setEndDate(LocalDate.of(y + 1, 1, 1).atStartOfDay(TAX_ZONE).toInstant());
    List<TaxDividend> dividends = new ArrayList<>();
    for (var d : dividendService.findDividends(request)) {
      if (d.getPayDate() == null) {
        continue;
      }
      dividends.add(
          new TaxDividend(
              d.getAccountId(),
              d.getStockItemId(),
              d.getStockItemName(),
              LocalDate.ofInstant(d.getPayDate(), TAX_ZONE),
              d.getGrossAmount(),
              d.getTaxableAmount(),
              d.getTax()));
    }

    List<HighDividendDesignation> designations = new ArrayList<>();
    highDividendDesignationRepository.findAll().forEach(designations::add);

    return DividendTaxReportCalculator.compute(
        y,
        y == LocalDate.now(TAX_ZONE).getYear(),
        dividends,
        deferred,
        designations,
        input.otherIncomeTaxBase(),
        input.otherFinancialIncome());
  }
}
