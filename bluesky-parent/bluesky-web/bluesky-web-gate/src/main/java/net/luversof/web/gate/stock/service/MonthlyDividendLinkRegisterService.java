package net.luversof.web.gate.stock.service;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
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

  /** 시세 갱신은 관리 쪽 창구를 쓴다 - 종목 하나만 고르는 입구가 여기 있다. */
  private final net.luversof.web.gate.stock.httpexchange.StockAdminClient stockAdminClient;

  public MonthlyDividendLinkRegisterService(
      StockItemClient stockItemClient,
      MonthlyDividendProfileClient monthlyDividendProfileClient,
      MonthlyDividendPayoutClient monthlyDividendPayoutClient,
      MonthlyDividendPayoutSourceImportService monthlyDividendPayoutSourceImportService,
      net.luversof.web.gate.stock.httpexchange.StockAdminClient stockAdminClient) {
    this.stockItemClient = stockItemClient;
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
    this.monthlyDividendPayoutSourceImportService = monthlyDividendPayoutSourceImportService;
    this.stockAdminClient = stockAdminClient;
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
      /** 그 종목 시세를 바로 받아 왔는가. 못 받아도 등록은 남는다. */
      boolean priceSeeded,
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

  /**
   * 그 종목 시세를 바로 받아 둔다(사용자 결정 2026-09-22).
   *
   * <p>등록만 하고 시세가 없으면 월배당 ETF 목록에서 현재가 · 연배당 수익률 · 기간 수익률이 모두 빈 줄로 남는다. 신규 한 종목의 2 년치 시드는 실측 약 1.1
   * 초다.
   *
   * <p><b>실패해도 등록은 살린다.</b> 시세는 나중에 종목별 갱신으로 채울 수 있지만, 여기서 예외를 올리면 이미 저장한 종목 · 태그 · 프로필 · 지급 이력이
   * "등록 실패" 로 보고돼 사용자가 다시 넣게 된다. 실패는 반드시 로그로 남긴다.
   */
  private boolean seedPriceHistory(String symbol) {
    UUID userId = UserUtil.getUserId();
    if (userId == null || !StringUtils.hasText(symbol)) {
      return false;
    }

    try {
      stockAdminClient.priceHistoryUpdateOne(symbol, userId);
      return true;
    } catch (Exception ex) {
      log.warn("등록한 종목의 시세를 못 받았다(등록은 유지): {}", symbol, ex);
      return false;
    }
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

      monthlyDividendProfileClient.upsertProfile(
          buildProfileRequest(meta, link, payoutWindow, findExistingProfile(meta.symbol())));

      for (MonthlyDividendPayoutUpsertRequest payout : imported.requests()) {
        monthlyDividendPayoutClient.upsertPayout(payout);
      }

      // 등록만 하고 시세가 없으면 목록에서 현재가 · 수익률이 빈 줄로 남는다(사용자 결정 2026-09-22:
      // 등록하면서 바로 받는다). 신규 한 종목의 2 년치 시드는 실측 약 1.1 초다.
      // 시세를 못 받아도 등록 자체는 살린다 - 시세는 나중에 종목별 갱신으로 채울 수 있다.
      boolean priceSeeded = seedPriceHistory(meta.symbol());

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
          priceSeeded,
          "");
    } catch (Exception ex) {
      log.warn("링크 등록 실패: {}", link, ex);
      String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.toString();
      return new LinkResult(link, false, "", "", "", false, false, 0, 0, false, reason);
    }
  }

  /** 총보수 · 상장일 새로 가져오기 결과(2026-09-28). 실패는 "종목: 사유" 한 줄씩. */
  public record FactsRefreshResult(int updatedCount, int unchangedCount, List<String> failures) {}

  /**
   * 등록된 월배당 프로필마다 운용사에서 총보수 · 상장일을 다시 읽어 바뀐 것만 저장한다(사용자 승인 2026-09-28).
   *
   * <p>다른 값은 기존 프로필 그대로 돌려보낸다(저장이 전체 덮어쓰기). 못 읽은 값은 지우지 않는다 &mdash; 사이트가 잠깐 바뀌어 한 번 못 읽었다고 알던 값을
   * 잃으면 안 된다. 링크가 없는 프로필은 건너뛴다.
   */
  public FactsRefreshResult refreshFacts() {
    List<MonthlyDividendProfileResponse> profiles =
        monthlyDividendProfileClient.findProfiles(
            new org.springframework.util.LinkedMultiValueMap<>());
    int updated = 0;
    int unchanged = 0;
    List<String> failures = new ArrayList<>();
    for (MonthlyDividendProfileResponse existing :
        profiles != null ? profiles : List.<MonthlyDividendProfileResponse>of()) {
      if (existing == null || !StringUtils.hasText(existing.sourceUrl())) {
        continue;
      }
      try {
        SourceMeta meta =
            monthlyDividendPayoutSourceImportService.fetchMeta(
                URI.create(existing.sourceUrl().trim()));
        if (meta.totalExpenseRatioPct() == null && meta.listingDate() == null) {
          failures.add(
              existing.stockItemSymbol()
                  + ": "
                  + msg("stock.monthly.reference.facts.refresh.missing"));
          continue;
        }
        MonthlyDividendProfileUpsertRequest request = withFacts(existing, meta);
        if (sameFacts(existing, request)) {
          unchanged++;
          continue;
        }
        monthlyDividendProfileClient.upsertProfile(request);
        updated++;
      } catch (Exception ex) {
        log.warn("총보수 새로 가져오기 실패: {}", existing.sourceUrl(), ex);
        String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.toString();
        failures.add(existing.stockItemSymbol() + ": " + reason);
      }
    }
    return new FactsRefreshResult(updated, unchanged, List.copyOf(failures));
  }

  /** 기존 프로필을 그대로 옮기고 이번에 읽은 총보수 · 상장일만 얹는다(읽은 값이 있을 때만). */
  public static MonthlyDividendProfileUpsertRequest withFacts(
      MonthlyDividendProfileResponse existing, SourceMeta meta) {
    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol(existing.stockItemSymbol());
    request.setSourceUrl(existing.sourceUrl());
    request.setPayoutWindow(existing.payoutWindow());
    request.setDisplayOrder(existing.displayOrder());
    request.setActive(existing.active());
    request.setNote(existing.note());
    request.setLastVerifiedDate(existing.lastVerifiedDate());
    request.setTotalExpenseRatioPct(
        meta.totalExpenseRatioPct() != null
            ? meta.totalExpenseRatioPct()
            : existing.totalExpenseRatioPct());
    request.setListingDate(
        meta.listingDate() != null ? meta.listingDate() : existing.listingDate());
    return request;
  }

  private static boolean sameFacts(
      MonthlyDividendProfileResponse existing, MonthlyDividendProfileUpsertRequest request) {
    boolean sameExpense =
        existing.totalExpenseRatioPct() == null
            ? request.getTotalExpenseRatioPct() == null
            : request.getTotalExpenseRatioPct() != null
                && existing.totalExpenseRatioPct().compareTo(request.getTotalExpenseRatioPct())
                    == 0;
    return sameExpense
        && java.util.Objects.equals(existing.listingDate(), request.getListingDate());
  }

  /**
   * 이미 등록된 종목이면 그 프로필. 저장이 전체 덮어쓰기라, 이것을 안 보고 새 요청을 만들면 다시 등록할 때마다 사람이 넣은 메모 · 최종 검증일이 지워졌다(발견
   * 2026-09-28). 조회가 실패하면 등록도 실패로 돌린다 &mdash; 모르는 채로 저장하면 지운다.
   */
  private MonthlyDividendProfileResponse findExistingProfile(String symbol) {
    org.springframework.util.LinkedMultiValueMap<String, String> params =
        new org.springframework.util.LinkedMultiValueMap<>();
    params.add("symbol", symbol);
    List<MonthlyDividendProfileResponse> found = monthlyDividendProfileClient.findProfiles(params);
    return found == null || found.isEmpty() ? null : found.get(0);
  }

  /**
   * 링크 등록의 프로필 저장 요청. 기존 프로필이 있으면 그 값을 바탕으로 하고 링크 · 지급 시기 · 활성만 새로 쓴다. 총보수 · 상장일은 이번에 읽은 값이 있을 때만
   * 바꾼다(못 읽었다고 알던 값을 지우지 않는다).
   */
  public static MonthlyDividendProfileUpsertRequest buildProfileRequest(
      SourceMeta meta, String link, String payoutWindow, MonthlyDividendProfileResponse existing) {
    MonthlyDividendProfileUpsertRequest profile = new MonthlyDividendProfileUpsertRequest();
    if (existing != null) {
      profile.setDisplayOrder(existing.displayOrder());
      profile.setNote(existing.note());
      profile.setLastVerifiedDate(existing.lastVerifiedDate());
      profile.setTotalExpenseRatioPct(existing.totalExpenseRatioPct());
      profile.setListingDate(existing.listingDate());
    }
    profile.setSymbol(meta.symbol());
    profile.setSourceUrl(link);
    profile.setPayoutWindow(payoutWindow);
    profile.setActive(Boolean.TRUE);
    if (meta.totalExpenseRatioPct() != null) {
      profile.setTotalExpenseRatioPct(meta.totalExpenseRatioPct());
    }
    if (meta.listingDate() != null) {
      profile.setListingDate(meta.listingDate());
    }
    return profile;
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
