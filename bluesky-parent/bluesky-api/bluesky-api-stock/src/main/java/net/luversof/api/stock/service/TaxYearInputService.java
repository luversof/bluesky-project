package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.TaxYearInput;
import net.luversof.api.stock.repository.TaxYearInputRepository;
import net.luversof.api.stock.web.dto.request.TaxYearInputUpsertRequest;
import net.luversof.api.stock.web.dto.response.TaxYearInputResponse;

/** 귀속연도 세금 입력값. 사용자 · 연도마다 한 줄이다. */
@Service
public class TaxYearInputService {

  @Autowired private TaxYearInputRepository taxYearInputRepository;

  public TaxYearInputResponse find(UUID userId, Integer year) {
    if (userId == null) {
      StockErrorCode.INVALID_USER_ID.throwException();
    }
    int y = requireYear(year);
    return taxYearInputRepository
        .findByUserIdAndYear(userId, y)
        .map(TaxYearInputService::toResponse)
        .orElse(new TaxYearInputResponse(y, null, null));
  }

  public TaxYearInputResponse upsert(TaxYearInputUpsertRequest request) {
    if (request == null || request.userId() == null) {
      StockErrorCode.INVALID_USER_ID.throwException();
    }
    int y = requireYear(request.year());
    requireNonNegative(request.otherIncomeTaxBase(), "otherIncomeTaxBase");
    requireNonNegative(request.otherFinancialIncome(), "otherFinancialIncome");
    TaxYearInput row =
        taxYearInputRepository
            .findByUserIdAndYear(request.userId(), y)
            .orElseGet(TaxYearInput::new);
    row.setUserId(request.userId());
    row.setYear(y);
    row.setOtherIncomeTaxBase(request.otherIncomeTaxBase());
    row.setOtherFinancialIncome(request.otherFinancialIncome());
    return toResponse(taxYearInputRepository.save(row));
  }

  static int requireYear(Integer year) {
    if (year == null || year < 2000 || year > 2100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be 2000..2100");
    }
    return year;
  }

  static void requireNonNegative(BigDecimal value, String name) {
    if (value != null && value.signum() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must not be negative");
    }
  }

  private static TaxYearInputResponse toResponse(TaxYearInput row) {
    return new TaxYearInputResponse(
        row.getYear(), row.getOtherIncomeTaxBase(), row.getOtherFinancialIncome());
  }
}
