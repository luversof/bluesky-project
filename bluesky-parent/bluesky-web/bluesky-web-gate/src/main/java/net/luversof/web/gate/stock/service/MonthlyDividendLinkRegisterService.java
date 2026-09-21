package net.luversof.web.gate.stock.service;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutImportParser.LenientParseResult;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutSourceImportService;
import net.luversof.web.gate.stock.util.MonthlyDividendPayoutWindowGuesser;
import net.luversof.web.gate.stock.util.MonthlyDividendSourceMetaParser.SourceMeta;

/**
 * 운용사 상세 <b>링크만</b>으로 월배당 기준 데이터를 등록한다(사용자 요청 2026-09-21).
 *
 * <p>예전에는 종목 마스터에 월배당 태그가 붙어 있어야 프로필 폼의 종목코드 목록에 떴다 &mdash; 새 ETF 를 넣으려면 종목부터 따로 만들어야 했다. 링크를 주면 한
 * 번에 네 가지를 한다: <b>종목</b>(없으면 생성) · <b>월배당 태그</b>(없으면 추가) · <b>프로필</b>(출처 · 지급 시기) · <b>지급 이력</b>.
 *
 * <p>사용자 결정(선택지로 물음):
 *
 * <ul>
 *   <li>여러 줄 중 일부가 실패하면 <b>되는 줄만</b> 등록하고 실패는 사유와 함께 돌려준다(기존 가져오기와 같은 규칙).
 *   <li>미리보기 없이 <b>바로 등록</b>하고 결과를 요약한다(되돌리는 길은 프로필 · 이력 삭제 단추로 이미 있다).
 *   <li>월중 · 월말은 <b>지급 이력으로 자동 판정</b>한다({@link MonthlyDividendPayoutWindowGuesser} &mdash; 등록된 12
 *       종목 258 건에 대 본 결과 12/12 일치).
 * </ul>
 */
@Service
public class MonthlyDividendLinkRegisterService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(MonthlyDividendLinkRegisterService.class);

  /** 월배당 기준 종목을 가리는 태그. 종목 마스터의 태그와 같은 문자열이어야 한다. */
  private static final String MONTHLY_DIVIDEND_TAG = "월배당";

  private final StockItemClient stockItemClient;

  private final MonthlyDividendProfileClient monthlyDividendProfileClient;

  private final MonthlyDividendPayoutClient monthlyDividendPayoutClient;

  private final MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService;

  public MonthlyDividendLinkRegisterService(
      StockItemClient stockItemClient,
      MonthlyDividendProfileClient monthlyDividendProfileClient,
      MonthlyDividendPayoutClient monthlyDividendPayoutClient,
      MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService) {
    this.stockItemClient = stockItemClient;
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
    this.monthlyDividendPayoutSourceImportService = monthlyDividendPayoutSourceImportService;
  }

  /** 한 줄(링크 하나)의 결과. 실패해도 나머지 줄은 그대로 간다. */
  public record LinkResult(
      String sourceUrl,
      boolean succeeded,
      String symbol,
      String name,
      String payoutWindow,
      boolean stockItemCreated,
      boolean tagAdded,
      int payoutCount,
      int skippedCount,
      String failureReason) {}

  public record LinkRegisterResult(List<LinkResult> results) {

    public long successCount() {
      return results.stream().filter(LinkResult::succeeded).count();
    }

    public long failureCount() {
      return results.stream().filter(result -> !result.succeeded()).count();
    }

    public int newStockItemCount() {
      return (int) results.stream().filter(LinkResult::stockItemCreated).count();
    }

    public int payoutCount() {
      return results.stream().mapToInt(LinkResult::payoutCount).sum();
    }

    public int skippedCount() {
      return results.stream().mapToInt(LinkResult::skippedCount).sum();
    }
  }

  /**
   * 여러 줄로 받은 링크를 차례로 등록한다.
   *
   * <p>같은 주소를 두 번 적으면 한 번만 한다 &mdash; 붙여넣다 보면 겹치는데, 두 번 부르면 운용사 쪽 호출만 늘고 결과는 같다.
   */
  public LinkRegisterResult registerLinks(String rawLinks) {
    if (!StringUtils.hasText(rawLinks)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.link.empty"));
    }

    List<String> links = splitLinks(rawLinks);
    if (links.isEmpty()) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.link.empty"));
    }

    List<LinkResult> results = new ArrayList<>();
    for (String link : links) {
      results.add(registerOne(link));
    }
    return new LinkRegisterResult(List.copyOf(results));
  }

  /** 여러 줄 입력을 링크 목록으로. 빈 줄은 버리고 같은 주소는 한 번만 둔다(붙여넣다 보면 겹친다). */
  public static List<String> splitLinks(String rawLinks) {
    if (rawLinks == null) {
      return List.of();
    }

    Set<String> links = new LinkedHashSet<>();
    for (String line : rawLinks.split("\\R")) {
      String trimmed = line.trim();
      if (StringUtils.hasText(trimmed)) {
        links.add(trimmed);
      }
    }
    return List.copyOf(links);
  }

  /** 종목 마스터를 어떻게 할지. */
  public enum StockItemAction {
    /** 그 종목이 없다 - 월배당 태그를 달아 만든다. */
    CREATE,
    /** 있는데 월배당 태그가 없다 - 태그만 더한다(이름 · 시장은 건드리지 않는다). */
    ADD_TAG,
    /** 이미 태그까지 있다 - 그대로 둔다. */
    KEEP
  }

  public static StockItemAction decideStockItemAction(StockItem matched) {
    if (matched == null) {
      return StockItemAction.CREATE;
    }

    return hasMonthlyDividendTag(matched) ? StockItemAction.KEEP : StockItemAction.ADD_TAG;
  }

  private LinkResult registerOne(String link) {
    try {
      URI sourceUri = URI.create(link);
      SourceMeta meta = monthlyDividendPayoutSourceImportService.fetchMeta(sourceUri);

      List<StockItem> existing = stockItemClient.getStockItems();
      StockItem matched = findBySymbol(existing, meta.symbol());
      StockItemAction action = decideStockItemAction(matched);
      boolean created = action == StockItemAction.CREATE;
      boolean tagAdded = action == StockItemAction.ADD_TAG;
      if (action == StockItemAction.CREATE) {
        // 시장은 KRX 로 둔다 - 시세 갱신이 KRX · KOSPI · KOSDAQ 만 대상으로 삼는다.
        stockItemClient.createStockItem(
            new StockItem(null, meta.symbol(), meta.name(), "KRX", List.of(MONTHLY_DIVIDEND_TAG)));
      } else if (action == StockItemAction.ADD_TAG) {
        List<String> tags = new ArrayList<>(matched.tags());
        tags.add(MONTHLY_DIVIDEND_TAG);
        stockItemClient.createStockItem(
            new StockItem(matched.id(), matched.symbol(), matched.name(), matched.market(), tags));
      }

      LenientParseResult imported =
          monthlyDividendPayoutSourceImportService.fetchImport(meta.symbol(), link);
      String payoutWindow =
          MonthlyDividendPayoutWindowGuesser.guess(
              imported.requests().stream()
                  .map(MonthlyDividendPayoutUpsertRequest::getRecordDate)
                  .toList());

      MonthlyDividendProfileUpsertRequest profile = new MonthlyDividendProfileUpsertRequest();
      profile.setSymbol(meta.symbol());
      profile.setSourceUrl(link);
      profile.setPayoutWindow(payoutWindow);
      profile.setActive(Boolean.TRUE);
      monthlyDividendProfileClient.upsertProfile(profile);

      for (MonthlyDividendPayoutUpsertRequest payout : imported.requests()) {
        monthlyDividendPayoutClient.upsertPayout(payout);
      }

      return new LinkResult(
          link,
          true,
          meta.symbol(),
          meta.name(),
          payoutWindow,
          created,
          tagAdded,
          imported.requests().size(),
          imported.skipped().size(),
          "");
    } catch (Exception ex) {
      log.warn("링크 등록 실패: {}", link, ex);
      String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.toString();
      return new LinkResult(link, false, "", "", "", false, false, 0, 0, reason);
    }
  }

  private StockItem findBySymbol(List<StockItem> stockItems, String symbol) {
    if (stockItems == null) {
      return null;
    }

    for (StockItem stockItem : stockItems) {
      if (stockItem != null
          && stockItem.symbol() != null
          && stockItem.symbol().trim().equalsIgnoreCase(symbol)) {
        return stockItem;
      }
    }
    return null;
  }

  private static boolean hasMonthlyDividendTag(StockItem stockItem) {
    return stockItem.tags().stream().anyMatch(tag -> MONTHLY_DIVIDEND_TAG.equals(tag.trim()));
  }
}
