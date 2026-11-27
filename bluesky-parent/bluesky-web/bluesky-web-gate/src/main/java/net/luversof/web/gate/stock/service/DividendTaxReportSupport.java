package net.luversof.web.gate.stock.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.dto.response.DividendTaxReport;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendTaxReportClient;

/** 배당 화면 "종소세 신고 자료" 구역의 모델. 조회가 실패하면 null - 구역이 실패 안내만 그린다. */
@Component
public class DividendTaxReportSupport {

  /** 세법의 해는 한국 시간으로 자른다. */
  public static final ZoneId TAX_ZONE = ZoneId.of("Asia/Seoul");

  /** 고를 수 있는 귀속연도 수(올해부터 거꾸로). */
  static final int YEAR_CHOICES = 4;

  private static final Logger log = LoggerFactory.getLogger(DividendTaxReportSupport.class);

  @Autowired private DividendTaxReportClient dividendTaxReportClient;

  @Autowired private AccountClient accountClient;

  /** 화면 모델: 고른 해의 신고 자료, 계좌 이름, 고를 수 있는 해. */
  public record View(
      int year, List<Integer> years, DividendTaxReport report, Map<UUID, String> accountNames) {}

  /** 요청한 해가 고를 수 있는 범위 밖이면 직전 연도(신고하는 해). */
  public static int resolveYear(Integer requested, LocalDate today) {
    int thisYear = today.getYear();
    if (requested != null && requested <= thisYear && requested > thisYear - YEAR_CHOICES) {
      return requested;
    }
    return thisYear - 1;
  }

  public View load(UUID userId, Integer requestedYear) {
    LocalDate today = LocalDate.now(TAX_ZONE);
    int year = resolveYear(requestedYear, today);
    List<Integer> years =
        java.util.stream.IntStream.range(0, YEAR_CHOICES)
            .mapToObj(i -> today.getYear() - i)
            .toList();
    if (userId == null) {
      return new View(year, years, null, Map.of());
    }
    DividendTaxReport report = null;
    try {
      report = dividendTaxReportClient.report(userId, year);
    } catch (RuntimeException e) {
      log.warn("dividend tax report lookup failed: year={}", year, e);
    }
    Map<UUID, String> names = new HashMap<>();
    try {
      List<Account> accounts = accountClient.getAccountsByUserId(userId);
      if (accounts != null) {
        accounts.forEach(a -> names.put(a.id(), a.name()));
      }
    } catch (RuntimeException e) {
      log.warn("account lookup failed for tax report", e);
    }
    return new View(year, years, report, names);
  }
}
