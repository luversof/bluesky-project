package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.common.menu.domain.Pagination;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendHtmxController extends StockBaseHtmxController {

  // 월중/월말 배당 구분에 쓰는 종목 태그. 월배당(커버드콜 등) 종목에 부여되어 있다.
  private static final String MID_MONTH_DIVIDEND_TAG = "월중배당";
  private static final String MONTH_END_DIVIDEND_TAG = "월말배당";

  private final net.luversof.web.gate.stock.support.StockAsyncSupport async;

  public StockDividendHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      MessageSource messageSource,
      net.luversof.web.gate.stock.support.StockAsyncSupport async) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
    this.async = async;
  }

  @BlueskyPreAuthorize
  @GetMapping("/dividend/list")
  public String dividendList(
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) List<String> stockTagList,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      return loginRequiredView(model);
    }

    Instant startInstant = startDate;
    Instant endInstant = endDate;
    // rangeMode 는 어떤 프리셋 버튼이 눌렸는지 알리는 화면 상태값이지 기간 그 자체가 아니다
    // (기간은 startDate/endDate 로 온다). 그런데 이 가드가 rangeMode 의 '존재'를 보는 바람에,
    // 날짜 없이 rangeMode 만 실려 오면 기본 기간이 적용되지 않고 전 기간이 조회됐다
    // (실측: rangeMode=ytd 인데 올해 106행이 아니라 전체 300행). 같은 가드를 asset-growth 는
    // 이미 '날짜가 없고 all 도 아니면 기본 적용'으로 쓰고 있어, 그쪽에 맞춘다.
    if (startInstant == null && endInstant == null && !"all".equalsIgnoreCase(rangeMode)) {
      ZoneId zone = resolveZoneIdOrDefault(timeZone);
      var preset = resolvePresetRange(rangeMode, zone);
      startInstant = preset.start();
      endInstant = preset.end();
      rangeMode = preset.mode();
    }

    var request = new DividendRequest();
    request.setUserId(userId);
    request.setStartDate(startInstant);
    request.setEndDate(endInstant);

    // 이 화면 앞부분의 네 호출(배당/배당메타/계좌/종목)은 서로 의존이 없어 함께 던진다.
    // "전체 기간" 필터 UI 에 필요한 건 최초 기준일과 배당 보유 종목 ID 뿐이라,
    // 전 기간 배당 이력을 통째로 내려받는 대신 메타 엔드포인트 1회로 대체한다.
    var dividendParams = request.toParams();
    var dividendsFuture =
        async.supply(() -> emptyIfNull(dividendClient.findDividends(dividendParams)));
    var dividendMetaFuture = async.supply(() -> dividendClient.findDividendMeta(userId));
    var accountsFuture = async.supply(() -> emptyIfNull(accountClient.getAccountsByUserId(userId)));
    var stockItemsFuture = async.supply(() -> emptyIfNull(stockItemClient.getStockItems()));

    // 전기 비교용 배당도 여기서 함께 던진다. 구간은 요청 파라미터만으로 정해지고 응답 필터는
    // 받은 뒤에 적용하므로 다른 조회를 기다릴 이유가 없다(예전에는 맨 뒤에서 순차로 불러
    // 기간 지정 화면에서 배당 조회가 2회 직렬이었다 — 실측 YTD 90.4ms, dividend 2회 14.2ms).
    ZoneId earlyZone = resolveZoneIdOrDefault(timeZone);
    PreviousPeriod previousPeriod = resolvePreviousPeriod(startDate, endDate, rangeMode, earlyZone);
    java.util.concurrent.CompletableFuture<List<DividendResponse>> prevDividendsFuture = null;
    if (previousPeriod != null) {
      var prevRequestEarly = new DividendRequest();
      prevRequestEarly.setUserId(userId);
      prevRequestEarly.setStartDate(previousPeriod.start().atStartOfDay(earlyZone).toInstant());
      prevRequestEarly.setEndDate(
          previousPeriod.end().plusDays(1).atStartOfDay(earlyZone).toInstant());
      var prevParams = prevRequestEarly.toParams();
      prevDividendsFuture =
          async.supply(() -> emptyIfNull(dividendClient.findDividends(prevParams)));
    }

    List<DividendResponse> dividends =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dividendsFuture);
    var dividendMeta =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(dividendMetaFuture);
    ZoneId zone = resolveZoneIdOrDefault(timeZone);
    LocalDate dataFirstDate =
        dividendMeta != null && dividendMeta.firstBasisDate() != null
            ? dividendMeta.firstBasisDate().atZone(zone).toLocalDate()
            : null;

    var dividendAccountIds =
        dividends.stream().map(DividendResponse::accountId).collect(Collectors.toSet());
    var dividendStockIds =
        dividends.stream().map(DividendResponse::stockItemId).collect(Collectors.toSet());
    var globalDividendStockIds =
        dividendMeta != null && dividendMeta.stockItemIds() != null
            ? java.util.Set.copyOf(dividendMeta.stockItemIds())
            : java.util.Set.<UUID>of();

    List<Account> accounts =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountsFuture);
    List<Account> filteredAccountList =
        accounts.stream().filter(a -> dividendAccountIds.contains(a.id())).toList();

    Map<UUID, String> accountNames =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));
    Map<UUID, Boolean> taxDeferredMap =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id,
                    a ->
                        a.jsonConfig() != null
                            && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
                    (l, r) -> l));

    List<StockItem> stockItemList =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(stockItemsFuture);
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, stockItemIdList, stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    List<StockItem> filteredStockItemList =
        stockItemList.stream().filter(s -> dividendStockIds.contains(s.id())).toList();
    // Stocks that have any dividends in the global timeframe (used for 전체/no-range)
    List<StockItem> filteredStockItemListAll =
        stockItemList.stream().filter(s -> globalDividendStockIds.contains(s.id())).toList();
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    // Validate requested filters against full lists (so we can preserve previous
    // selections) and build final lists that show date-available items but keep
    // any previously selected items visible.
    Set<UUID> availableAccountIds = accounts.stream().map(Account::id).collect(Collectors.toSet());
    List<UUID> requestedAccountIds = accountIdList;
    List<UUID> effectiveAccountIdList =
        retainAvailableIds(requestedAccountIds, availableAccountIds);

    Set<UUID> availableStockIds =
        stockItemList.stream().map(StockItem::id).collect(Collectors.toSet());
    List<UUID> requestedStockItemIds = stockTagSelection.requestedStockItemIds();
    List<UUID> effectiveStockItemIdList =
        retainAvailableStockItemIds(stockTagSelection, availableStockIds);

    List<Account> finalAccountList;
    if (startInstant != null || endInstant != null) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null) continue;
          if (!finalAccountList.stream().anyMatch(a -> a.id().equals(sel))) {
            accounts.stream()
                .filter(a -> a.id().equals(sel))
                .findFirst()
                .ifPresent(a -> finalAccountList.add(0, a));
          }
        }
      }
    } else {
      finalAccountList = accounts;
    }

    List<StockItem> finalStockItemList;
    if (startInstant != null || endInstant != null) {
      // Date-specific search -> show only stocks that had dividends in the requested
      // period
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(s -> finalStockItemList.add(0, s));
          }
        }
      }
    } else {
      // No explicit date range (or rangeMode='all') -> show stocks that have any
      // dividend history
      finalStockItemList = new ArrayList<>(filteredStockItemListAll);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(s -> finalStockItemList.add(0, s));
          }
        }
      }
    }

    List<DividendView> viewList =
        dividends.stream()
            .filter(d -> matchesFilter(effectiveAccountIdList, d.accountId()))
            .filter(d -> matchesFilter(effectiveStockItemIdList, d.stockItemId()))
            .map(
                dividend -> {
                  String accountName =
                      accountNames.getOrDefault(dividend.accountId(), msg("stock.label.unknown"));
                  String stockItemName =
                      Optional.ofNullable(dividend.stockItemName())
                          .orElse(
                              Optional.ofNullable(dividend.stockItemId())
                                  .map(
                                      id ->
                                          stockItemNames.getOrDefault(
                                              id, msg("stock.label.unknown")))
                                  .orElse(msg("stock.label.unknown")));

                  boolean isDeferred = taxDeferredMap.getOrDefault(dividend.accountId(), false);

                  BigDecimal grossAmount =
                      Optional.ofNullable(dividend.grossAmount()).orElse(BigDecimal.ZERO);
                  BigDecimal tax =
                      isDeferred
                          ? BigDecimal.ZERO
                          : Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
                  BigDecimal netAmount =
                      isDeferred
                          ? grossAmount
                          : Optional.ofNullable(dividend.netAmount())
                              .orElse(grossAmount.subtract(tax));

                  BigDecimal taxableAmount = BigDecimal.ZERO;
                  if (!isDeferred) {
                    if (dividend.taxableAmount() != null) {
                      taxableAmount = dividend.taxableAmount();
                    } else if (dividend.taxPerShare() != null && dividend.quantity() != null) {
                      taxableAmount =
                          dividend.taxPerShare().multiply(BigDecimal.valueOf(dividend.quantity()));
                    }
                  }

                  return new DividendView(
                      dividend.id(),
                      dividend.accountId(),
                      accountName,
                      dividend.stockItemId(),
                      stockItemName,
                      dividend.quantity(),
                      dividend.amountPerShare(),
                      grossAmount,
                      tax,
                      taxableAmount,
                      dividend.taxPerShare(),
                      netAmount,
                      dividend.recordDate(),
                      dividend.payDate(),
                      null,
                      null,
                      null,
                      null,
                      null,
                      null);
                })
            .collect(Collectors.toCollection(ArrayList::new));

    DividendAnalyticsResult analyticsResult =
        buildDividendAnalytics(
            userId,
            viewList,
            effectiveAccountIdList,
            effectiveStockItemIdList,
            startInstant,
            endInstant,
            zone);
    viewList = analyticsResult.dividendViews();

    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";

      Comparator<DividendView> comparator =
          switch (field) {
            case "payDate" ->
                Comparator.comparing(
                    DividendView::payDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "accountName" ->
                Comparator.comparing(
                    DividendView::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    DividendView::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "grossAmount" ->
                Comparator.comparing(
                    DividendView::grossAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "netAmount" ->
                Comparator.comparing(
                    DividendView::netAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "tax" ->
                Comparator.comparing(
                    DividendView::tax, Comparator.nullsLast(Comparator.naturalOrder()));
            case "taxableAmount" ->
                Comparator.comparing(
                    DividendView::taxableAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };

      if (comparator != null) {
        if ("desc".equalsIgnoreCase(direction)) {
          comparator = comparator.reversed();
        }
        viewList.sort(comparator);
      }
    } else {
      viewList.sort(
          Comparator.comparing(
              DividendView::payDate, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    if (size <= 0) size = 15;
    // 상세 목록은 페이징 없이 전체를 펼쳐서 표시(헤더 sticky로 스크롤).
    if (!viewList.isEmpty()) size = viewList.size();

    boolean isSearch =
        (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
            || (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
            || startDate != null
            || endDate != null;

    if (isSearch) {
      size = Math.max(viewList.size(), 1);
    }

    int totalItems = viewList.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int currentPage = Math.max(1, Math.min(page, totalPages));
    if (totalPages == 0) currentPage = 1;

    int fromIndex = (currentPage - 1) * size;
    int toIndex = Math.min(fromIndex + size, totalItems);
    List<DividendView> pagedList =
        (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex) : Collections.emptyList();

    BigDecimal totalGrossAmount =
        pagedList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalNetAmount =
        pagedList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTax =
        pagedList.stream().map(DividendView::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTaxableAmount =
        pagedList.stream()
            .map(DividendView::taxableAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAllGrossAmount =
        viewList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllNetAmount =
        viewList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllTaxableAmount =
        viewList.stream().map(DividendView::taxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllTax =
        viewList.stream().map(DividendView::tax).reduce(BigDecimal.ZERO, BigDecimal::add);

    // 월중/월말 배당 구분: 종목 태그(월중배당/월말배당) 기준으로 기간 배당(세후)을 나눈다.
    // 태그가 없는 종목(분기배당 등)은 '기타'로 집계한다. 전체 종목 리스트에서 태그를 조회한다.
    Map<UUID, List<String>> tagsByStockId =
        stockItemList.stream()
            .collect(
                Collectors.toMap(
                    StockItem::id,
                    s -> s.tags() != null ? s.tags() : List.of(),
                    (left, right) -> left));
    BigDecimal midMonthNetAmount = BigDecimal.ZERO;
    BigDecimal monthEndNetAmount = BigDecimal.ZERO;
    BigDecimal otherWindowNetAmount = BigDecimal.ZERO;
    long midMonthCount = 0;
    long monthEndCount = 0;
    long otherWindowCount = 0;
    for (DividendView d : viewList) {
      List<String> tags = tagsByStockId.getOrDefault(d.stockItemId(), List.of());
      BigDecimal net = nz(d.netAmount());
      if (tags.contains(MID_MONTH_DIVIDEND_TAG)) {
        midMonthNetAmount = midMonthNetAmount.add(net);
        midMonthCount++;
      } else if (tags.contains(MONTH_END_DIVIDEND_TAG)) {
        monthEndNetAmount = monthEndNetAmount.add(net);
        monthEndCount++;
      } else {
        otherWindowNetAmount = otherWindowNetAmount.add(net);
        otherWindowCount++;
      }
    }

    BigDecimal prevPeriodNetAmount = null;
    LocalDate prevStartDate = null;
    LocalDate prevEndDate = null;
    List<DividendChange> dividendChangeContributors = java.util.List.of();
    if (previousPeriod != null) {
      // 전기 구간과 조회는 앞에서 이미 끝냈다(resolvePreviousPeriod + prevDividendsFuture).
      prevStartDate = previousPeriod.start();
      prevEndDate = previousPeriod.end();

      final List<UUID> finalAccountIdList = effectiveAccountIdList;
      final List<UUID> finalStockItemIdList = effectiveStockItemIdList;
      List<DividendResponse> prevDividends =
          net.luversof.web.gate.stock.support.StockAsyncSupport.join(prevDividendsFuture);
      prevPeriodNetAmount =
          prevDividends.stream()
              .filter(d -> matchesFilter(finalAccountIdList, d.accountId()))
              .filter(d -> matchesFilter(finalStockItemIdList, d.stockItemId()))
              .map(
                  d -> {
                    boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
                    BigDecimal gross = Optional.ofNullable(d.grossAmount()).orElse(BigDecimal.ZERO);
                    if (isDeferred) return gross;
                    BigDecimal tax2 = Optional.ofNullable(d.tax()).orElse(BigDecimal.ZERO);
                    return Optional.ofNullable(d.netAmount()).orElse(gross.subtract(tax2));
                  })
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      // 종목별 증감 분해(전기 대비): 어떤 종목이 차이를 만들었는지.
      Map<String, BigDecimal> prevByStock = new HashMap<>();
      prevDividends.stream()
          .filter(d -> matchesFilter(finalAccountIdList, d.accountId()))
          .filter(d -> matchesFilter(finalStockItemIdList, d.stockItemId()))
          .forEach(
              d -> {
                boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
                BigDecimal gross = nz(d.grossAmount());
                BigDecimal net =
                    isDeferred
                        ? gross
                        : Optional.ofNullable(d.netAmount()).orElse(gross.subtract(nz(d.tax())));
                prevByStock.merge(
                    d.stockItemName() != null ? d.stockItemName() : "-", net, BigDecimal::add);
              });
      Map<String, BigDecimal> curByStock = new HashMap<>();
      viewList.forEach(
          v ->
              curByStock.merge(
                  v.stockItemName() != null ? v.stockItemName() : "-",
                  nz(v.netAmount()),
                  BigDecimal::add));
      // 종목명 -> stockItemId (변동 요인 종목 상세 링크용)
      Map<String, UUID> changeStockIdByName = new HashMap<>();
      stockItemList.forEach(
          s -> {
            if (s != null && s.name() != null && s.id() != null) {
              changeStockIdByName.putIfAbsent(s.name(), s.id());
            }
          });
      Set<String> changeNames = new HashSet<>();
      changeNames.addAll(curByStock.keySet());
      changeNames.addAll(prevByStock.keySet());
      dividendChangeContributors =
          changeNames.stream()
              .map(
                  n -> {
                    BigDecimal c = curByStock.getOrDefault(n, BigDecimal.ZERO);
                    BigDecimal p = prevByStock.getOrDefault(n, BigDecimal.ZERO);
                    return new DividendChange(n, c, p, c.subtract(p), changeStockIdByName.get(n));
                  })
              .filter(ch -> ch.delta().signum() != 0)
              .sorted((a, b) -> b.delta().abs().compareTo(a.delta().abs()))
              .collect(java.util.stream.Collectors.toList());
    }

    var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
    var pagination = new Pagination(pageImpl);

    // 조회/페이징은 그대로 두고, 화면에는 그 페이지를 오름차순(오래된 것 위)으로 표시(명시 정렬 시 제외).
    List<DividendView> displayDividendList = new java.util.ArrayList<>(pagedList);
    if (sort == null || sort.isEmpty()) {
      java.util.Collections.reverse(displayDividendList);
    }
    model.addAttribute("dividendList", displayDividendList);
    model.addAttribute("allDividendList", viewList);
    model.addAttribute("pagination", pagination);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("size", size);
    model.addAttribute("accountList", finalAccountList);
    model.addAttribute("stockItemList", finalStockItemList);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute(
        "selectedAccountIds", effectiveAccountIdList != null ? effectiveAccountIdList : List.of());
    model.addAttribute(
        "selectedStockItemIds",
        effectiveStockItemIdList != null ? effectiveStockItemIdList : List.of());
    model.addAttribute("selectedStockTags", selectedStockTags);
    model.addAttribute(
        "selectedAccountId",
        (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
            ? effectiveAccountIdList.get(0)
            : null);
    model.addAttribute(
        "selectedStockItemId",
        (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
            ? effectiveStockItemIdList.get(0)
            : null);
    // reflect actual instants used (may have been defaulted to YTD above)
    model.addAttribute("startDate", startInstant);
    model.addAttribute("endDate", endInstant);
    model.addAttribute("timeZone", timeZone);
    model.addAttribute("sort", sort);
    model.addAttribute("totalGrossAmount", totalGrossAmount);
    model.addAttribute("totalNetAmount", totalNetAmount);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("totalTaxableAmount", totalTaxableAmount);
    model.addAttribute("totalAllGrossAmount", totalAllGrossAmount);
    model.addAttribute("totalAllNetAmount", totalAllNetAmount);
    model.addAttribute("totalAllTaxableAmount", totalAllTaxableAmount);
    model.addAttribute("totalAllTax", totalAllTax);
    model.addAttribute("midMonthNetAmount", midMonthNetAmount);
    model.addAttribute("monthEndNetAmount", monthEndNetAmount);
    model.addAttribute("otherWindowNetAmount", otherWindowNetAmount);
    model.addAttribute("midMonthCount", midMonthCount);
    model.addAttribute("monthEndCount", monthEndCount);
    model.addAttribute("otherWindowCount", otherWindowCount);
    model.addAttribute("prevPeriodNetAmount", prevPeriodNetAmount);
    model.addAttribute("dividendChangeContributors", dividendChangeContributors);
    model.addAttribute("prevStartDate", prevStartDate);
    model.addAttribute("prevEndDate", prevEndDate);
    // 달력 프리셋(mtd/ytd)은 '진행 중인 기간'을 '완전한 전월/전년'과 비교한다. 밝히지 않으면 증감률이
    // 실제 추세와 다르게 읽힌다(실측은 resolveInProgressPeriod 주석 참고).
    int[] monthProgress = resolveInProgressPeriod(startDate, endDate, rangeMode, zone);
    model.addAttribute("currentPeriodElapsedDays", monthProgress == null ? 0 : monthProgress[0]);
    model.addAttribute("currentPeriodTotalDays", monthProgress == null ? 0 : monthProgress[1]);
    model.addAttribute(
        "portfolioYieldOnCostPct",
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnCostPct()
            : null);
    model.addAttribute(
        "portfolioYieldOnDailyAverageCostPct",
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnDailyAverageCostPct()
            : null);
    model.addAttribute(
        "portfolioYieldOnMarketPct",
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnMarketPct()
            : null);

    // 연 환산 수익률: 기간 수익률 × (365 / 기간일수). 기간이 다른 구간끼리 비교할 수 있게 한다.
    // (배당은 재투자 가정이 없으므로 복리가 아닌 단순 환산을 쓴다.)
    BigDecimal periodYieldPct =
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnDailyAverageCostPct()
            : null;
    BigDecimal annualizedYieldPct = null;
    if (periodYieldPct != null && analyticsResult.periodDayCount() > 0) {
      annualizedYieldPct =
          periodYieldPct
              .multiply(BigDecimal.valueOf(365))
              .divide(
                  BigDecimal.valueOf(analyticsResult.periodDayCount()), 2, RoundingMode.HALF_UP);
    }
    model.addAttribute("portfolioYieldAnnualizedPct", annualizedYieldPct);
    model.addAttribute("periodDayCount", analyticsResult.periodDayCount());

    // 기간 중 원금 변동(기초 → 기말). 계산에는 이미 일평균으로 반영되지만, 변동 자체를 보여준다.
    BigDecimal startPrincipal = analyticsResult.periodStartPrincipal();
    BigDecimal endPrincipal = analyticsResult.periodEndPrincipal();
    model.addAttribute("periodStartPrincipal", startPrincipal);
    model.addAttribute("periodEndPrincipal", endPrincipal);
    BigDecimal principalDelta = null;
    BigDecimal principalDeltaPct = null;
    if (startPrincipal != null && endPrincipal != null) {
      principalDelta = endPrincipal.subtract(startPrincipal);
      if (startPrincipal.compareTo(BigDecimal.ZERO) > 0) {
        principalDeltaPct =
            principalDelta
                .multiply(BigDecimal.valueOf(100))
                .divide(startPrincipal, 2, RoundingMode.HALF_UP);
      }
    }
    model.addAttribute("periodPrincipalDelta", principalDelta);
    model.addAttribute("periodPrincipalDeltaPct", principalDeltaPct);
    model.addAttribute(
        "bestYieldStock",
        analyticsResult.stockYieldRows().isEmpty()
            ? null
            : analyticsResult.stockYieldRows().get(0));
    model.addAttribute(
        "bestYieldAccount",
        analyticsResult.accountYieldRows().isEmpty()
            ? null
            : analyticsResult.accountYieldRows().get(0));
    model.addAttribute("stockYieldRows", analyticsResult.stockYieldRows());
    model.addAttribute("accountYieldRows", analyticsResult.accountYieldRows());
    model.addAttribute("yearlyYieldRows", analyticsResult.yearlyYieldRows());
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    // reflect rangeMode back into model (may have been defaulted above)
    model.addAttribute("rangeMode", rangeMode);

    return "stock/htmx/fragments/tabsDividendHistory";
  }

  /** 비교용 "전기" 구간. 요청 파라미터만으로 정해지므로 원격 조회 전에 계산할 수 있다. */

  /**
   * 달력 프리셋이 아직 진행 중이면 {@code [경과일, 총일수]} 를, 아니면 {@code null} 을 돌려준다.
   *
   * <p>전기 비교는 진행 중인 이번 달/올해를 <b>완전한</b> 전월/전년과 견준다. 그 사실을 밝히지 않으면 증감률이 실제 추세와 다르게, 때로는 반대로 읽힌다.
   *
   * <pre>
   *   실측 2026-08-22
   *   mtd  22일 3,062,734  vs 31일 3,355,246   화면 -8.7%  / 일평균 기준 +28.6%
   *   ytd 234일 23,156,053 vs 365일 14,524,375 화면 +59.4% / 일평균 기준 +148.7%
   * </pre>
   *
   * <p>상대 N개월 프리셋은 양쪽 길이가 최대 3 일 차이라 표기하지 않는다(실측: 1개월 -8.7% vs -11.7%).
   */
  private int[] resolveInProgressPeriod(
      Instant startDate, Instant endDate, String rangeMode, ZoneId zone) {
    if (startDate == null || endDate == null) {
      return null;
    }
    LocalDate endLocal = endDate.atZone(zone).toLocalDate();
    int elapsedDays;
    int totalDays;
    if ("mtd".equals(rangeMode)) {
      elapsedDays = endLocal.getDayOfMonth();
      totalDays = endLocal.lengthOfMonth();
    } else if ("ytd".equals(rangeMode)) {
      elapsedDays = endLocal.getDayOfYear();
      totalDays = endLocal.lengthOfYear();
    } else {
      return null;
    }
    return elapsedDays < totalDays ? new int[] {elapsedDays, totalDays} : null;
  }

  private record PreviousPeriod(LocalDate start, LocalDate end) {}

  private PreviousPeriod resolvePreviousPeriod(
      Instant startDate, Instant endDate, String rangeMode, ZoneId zone) {
    if (startDate == null || endDate == null) {
      return null;
    }
    LocalDate startLocal = startDate.atZone(zone).toLocalDate();
    LocalDate endLocal = endDate.atZone(zone).toLocalDate();
    if ("mtd".equals(rangeMode)) {
      // 이번 달 선택 시 전기 = 전월 1일 ~ 말일 (달력 기준 통월)
      LocalDate thisMonthFirst = startLocal.withDayOfMonth(1);
      return new PreviousPeriod(thisMonthFirst.minusMonths(1), thisMonthFirst.minusDays(1));
    }
    if ("ytd".equals(rangeMode)) {
      // 올해 선택 시 전기 = 전년 1/1 ~ 12/31 (달력 기준 통년)
      LocalDate thisYearFirst = startLocal.withDayOfYear(1);
      return new PreviousPeriod(thisYearFirst.minusYears(1), thisYearFirst.minusDays(1));
    }
    if (rangeMode != null && rangeMode.matches("\\d+") && Integer.parseInt(rangeMode) > 0) {
      // 상대 N개월 프리셋(1/3/6/12/36개월). 전기 = 현재 시작 직전의 동일 N개월(달력 정렬).
      int months = Integer.parseInt(rangeMode);
      return new PreviousPeriod(startLocal.minusMonths(months), startLocal.minusDays(1));
    }
    // 그 외(수동 지정 등 달력 정렬 불가)는 직전 동일 길이 구간으로 비교
    long durationDays = ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
    return new PreviousPeriod(startLocal.minusDays(durationDays), startLocal.minusDays(1));
  }

  private DividendAnalyticsResult buildDividendAnalytics(
      UUID userId,
      List<DividendView> dividendViews,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList,
      Instant startInstant,
      Instant endInstant,
      ZoneId zone) {
    if (dividendViews == null || dividendViews.isEmpty()) {
      return DividendAnalyticsResult.empty(
          dividendViews != null ? dividendViews : new ArrayList<>());
    }

    Set<LocalDate> basisDates =
        dividendViews.stream()
            .map(dividend -> resolveBasisDate(dividend, zone))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));

    // 보유 스냅샷 배치(81ms)와 거래 조회(22ms)는 둘 다 basisDates 에서 파생된 값만 쓰고
    // 서로 의존하지 않는다. 날짜 계산을 먼저 끝낸 뒤 두 호출을 함께 던진다.
    var snapshotFuture = async.supply(() -> loadSnapshotsByDate(userId, basisDates, zone));
    LocalDate maxBasisDate = basisDates.stream().max(Comparator.naturalOrder()).orElse(null);
    LocalDate minBasisDate = basisDates.stream().min(Comparator.naturalOrder()).orElse(null);
    // 전체(all) 모드는 startInstant 이 없다. 이때 시작일을 maxBasisDate 로 잡으면 기간이
    // [maxBasisDate, maxBasisDate] = 1일 로 붕괴해 연 환산 수익률이 폭주한다(예: 3432%).
    // 시작일 미지정 시엔 가장 이른 배당 기준일을 기간 시작으로 사용한다.
    LocalDate periodStartDate =
        startInstant != null ? startInstant.atZone(zone).toLocalDate() : minBasisDate;
    LocalDate periodEndDate = resolvePeriodEndDate(endInstant, maxBasisDate, zone);
    Map<Integer, Long> periodDayCountsByYear =
        buildPeriodDayCountsByYear(periodStartDate, periodEndDate);
    long totalPeriodDayCount =
        periodDayCountsByYear.values().stream().mapToLong(Long::longValue).sum();
    LocalDate tradeCoverageEndDate =
        Stream.of(maxBasisDate, periodEndDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    Instant tradeEndDate =
        tradeCoverageEndDate != null
            ? tradeCoverageEndDate.plusDays(1).atStartOfDay(zone).toInstant()
            : null;

    var tradeParams =
        new TradeSearchRequest(userId, accountIdList, stockItemIdList, null, tradeEndDate)
            .toParams();
    var tradesFuture = async.supply(() -> emptyIfNull(tradeClient.findTrades(tradeParams)));
    Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> snapshotByDate =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(snapshotFuture);
    List<TradeResponse> trades =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(tradesFuture);

    Map<PositionKey, List<TradeResponse>> tradesByKey =
        trades.stream()
            .filter(
                trade ->
                    trade.accountId() != null
                        && trade.stockItemId() != null
                        && trade.tradeDate() != null)
            .collect(
                Collectors.groupingBy(
                    trade -> new PositionKey(trade.accountId(), trade.stockItemId()),
                    Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        list -> {
                          list.sort(
                              Comparator.comparing(
                                      (TradeResponse trade) ->
                                          trade.tradeDate().atZone(zone).toLocalDate())
                                  .thenComparing(
                                      trade ->
                                          trade.type()
                                                  == net.luversof.web.gate.stock.constant.TradeType
                                                      .BUY
                                              ? 0
                                              : 1)
                                  .thenComparing(TradeResponse::tradeDate));
                          return list;
                        })));

    Map<PositionKey, List<DividendView>> dividendsByKey =
        dividendViews.stream()
            .filter(dividend -> dividend.accountId() != null && dividend.stockItemId() != null)
            .collect(
                Collectors.groupingBy(
                    dividend -> new PositionKey(dividend.accountId(), dividend.stockItemId()),
                    Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        list -> {
                          list.sort(
                              Comparator.comparing(
                                      (DividendView dividend) -> resolveBasisDate(dividend, zone),
                                      Comparator.nullsLast(Comparator.naturalOrder()))
                                  .thenComparing(
                                      dividend ->
                                          Optional.ofNullable(dividend.payDate())
                                              .orElse(dividend.recordDate()),
                                      Comparator.nullsLast(Comparator.naturalOrder())));
                          return list;
                        })));

    Map<UUID, DividendView> enrichedById = new HashMap<>();
    Map<UUID, YieldAccumulator> stockAccumulators = new LinkedHashMap<>();
    Map<UUID, YieldAccumulator> accountAccumulators = new LinkedHashMap<>();
    Map<Integer, YieldAccumulator> yearlyAccumulators = new LinkedHashMap<>();
    YieldAccumulator portfolioAccumulator = new YieldAccumulator("portfolio", totalPeriodDayCount);
    // 기간 중 원금 변동 표시용: 포지션별 기초/기말 투입원금을 합산한다.
    BigDecimal periodStartPrincipal = BigDecimal.ZERO;
    BigDecimal periodEndPrincipal = BigDecimal.ZERO;

    for (Map.Entry<PositionKey, List<DividendView>> entry : dividendsByKey.entrySet()) {
      PositionKey key = entry.getKey();
      List<TradeResponse> tradeList = tradesByKey.getOrDefault(key, List.of());
      CostBasisState costBasisState = new CostBasisState();
      int tradeIndex = 0;

      for (DividendView dividend : entry.getValue()) {
        LocalDate basisDate = resolveBasisDate(dividend, zone);
        if (basisDate == null) {
          enrichedById.put(dividend.id(), dividend);
          continue;
        }

        while (tradeIndex < tradeList.size()) {
          TradeResponse trade = tradeList.get(tradeIndex);
          LocalDate tradeDate = trade.tradeDate().atZone(zone).toLocalDate();
          if (tradeDate.isAfter(basisDate)) {
            break;
          }
          costBasisState.apply(trade);
          tradeIndex++;
        }

        Integer quantity =
            dividend.quantity() != null && dividend.quantity() > 0
                ? dividend.quantity()
                : (costBasisState.rawQuantity() > 0 ? (int) costBasisState.rawQuantity() : null);
        BigDecimal averageCostBasis =
            quantity != null && quantity > 0 ? costBasisState.averageCost() : null;
        BigDecimal principalCost = multiplyQuantity(averageCostBasis, quantity);

        HoldingsSnapshotItem snapshotItem =
            snapshotByDate.getOrDefault(basisDate, Map.of()).get(dividend.stockItemId());
        BigDecimal referencePrice = snapshotItem != null ? snapshotItem.priceAtDate() : null;
        BigDecimal principalMarketValue = multiplyQuantity(referencePrice, quantity);

        BigDecimal yieldOnCostPct = percentage(dividend.netAmount(), principalCost);
        BigDecimal yieldOnMarketPct = percentage(dividend.netAmount(), principalMarketValue);

        DividendView enrichedDividend =
            new DividendView(
                dividend.id(),
                dividend.accountId(),
                dividend.accountName(),
                dividend.stockItemId(),
                dividend.stockItemName(),
                quantity,
                dividend.amountPerShare(),
                dividend.grossAmount(),
                dividend.tax(),
                dividend.taxableAmount(),
                dividend.taxableBasePerShare(),
                dividend.netAmount(),
                dividend.recordDate(),
                dividend.payDate(),
                referencePrice,
                averageCostBasis,
                principalCost,
                principalMarketValue,
                yieldOnCostPct,
                yieldOnMarketPct);
        enrichedById.put(enrichedDividend.id(), enrichedDividend);

        portfolioAccumulator.accept(enrichedDividend);
        stockAccumulators
            .computeIfAbsent(
                dividend.stockItemId(),
                ignored ->
                    new YieldAccumulator(
                        dividend.stockItemName(), dividend.stockItemId(), totalPeriodDayCount))
            .accept(enrichedDividend);
        accountAccumulators
            .computeIfAbsent(
                dividend.accountId(),
                ignored ->
                    new YieldAccumulator(
                        dividend.accountName(), dividend.accountId(), totalPeriodDayCount))
            .accept(enrichedDividend);
        yearlyAccumulators
            .computeIfAbsent(
                basisDate.getYear(),
                year ->
                    new YieldAccumulator(
                        String.valueOf(year), periodDayCountsByYear.getOrDefault(year, 0L)))
            .accept(enrichedDividend);
      }

      PeriodPrincipalSummary periodPrincipalSummary =
          summarizePeriodPrincipalCosts(tradeList, periodStartDate, periodEndDate, zone);
      periodStartPrincipal =
          periodStartPrincipal.add(nz(periodPrincipalSummary.startPrincipalCost()));
      periodEndPrincipal = periodEndPrincipal.add(nz(periodPrincipalSummary.endPrincipalCost()));

      stockAccumulators
          .computeIfAbsent(
              key.stockItemId(),
              ignored ->
                  new YieldAccumulator(
                      entry.getValue().get(0).stockItemName(),
                      key.stockItemId(),
                      totalPeriodDayCount))
          .acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
      accountAccumulators
          .computeIfAbsent(
              key.accountId(),
              ignored ->
                  new YieldAccumulator(
                      entry.getValue().get(0).accountName(), key.accountId(), totalPeriodDayCount))
          .acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
      portfolioAccumulator.acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
      periodPrincipalSummary
          .principalCostSumByYear()
          .forEach(
              (year, principalCostSum) ->
                  yearlyAccumulators
                      .computeIfAbsent(
                          year,
                          ignored ->
                              new YieldAccumulator(
                                  String.valueOf(year),
                                  periodDayCountsByYear.getOrDefault(year, 0L)))
                      .acceptDailyPrincipalCostSum(principalCostSum));
    }

    List<DividendView> enrichedDividends =
        dividendViews.stream()
            .map(dividend -> enrichedById.getOrDefault(dividend.id(), dividend))
            .collect(Collectors.toCollection(ArrayList::new));

    List<DividendYieldGroupView> stockYieldRows = sortYieldRows(stockAccumulators.values());
    List<DividendYieldGroupView> accountYieldRows = sortYieldRows(accountAccumulators.values());
    List<DividendYieldGroupView> yearlyYieldRows =
        yearlyAccumulators.entrySet().stream()
            .sorted(Map.Entry.<Integer, YieldAccumulator>comparingByKey().reversed())
            .map(entry -> entry.getValue().toView())
            .collect(Collectors.toCollection(ArrayList::new));

    return new DividendAnalyticsResult(
        enrichedDividends,
        portfolioAccumulator.hasData() ? portfolioAccumulator.toView() : null,
        yearlyYieldRows,
        stockYieldRows,
        accountYieldRows,
        periodStartPrincipal,
        periodEndPrincipal,
        totalPeriodDayCount);
  }

  private Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> loadSnapshotsByDate(
      UUID userId, Set<LocalDate> basisDates, ZoneId zone) {
    Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> result = new LinkedHashMap<>();
    if (basisDates == null || basisDates.isEmpty()) {
      return result;
    }

    // 기준일마다 순차 호출하면 날짜 수만큼 왕복이 발생한다(전체 기간이면 수십 회, 수 초).
    // 배치 엔드포인트로 1회에 받아온다.
    var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
    params.add("userId", userId.toString());
    params.add(
        "dates", basisDates.stream().map(LocalDate::toString).collect(Collectors.joining(",")));
    // 일자 집계 기준 타임존을 함께 넘긴다. 빠뜨리면 api-stock 이 서버 기본 타임존으로
    // 계산해, 컨테이너가 UTC 인 환경에서 기준일이 하루 밀린다.
    if (zone != null) {
      params.add("timeZone", zone.getId());
    }
    Map<String, List<HoldingsSnapshotItem>> batch = tradeProfitClient.holdingsSnapshotBatch(params);
    if (batch == null) {
      batch = Map.of();
    }

    for (LocalDate basisDate : basisDates) {
      List<HoldingsSnapshotItem> snapshotItems = emptyIfNull(batch.get(basisDate.toString()));
      Map<UUID, HoldingsSnapshotItem> itemsByStockId =
          snapshotItems.stream()
              .filter(item -> item.stockItemId() != null)
              .collect(
                  Collectors.toMap(
                      HoldingsSnapshotItem::stockItemId,
                      item -> item,
                      (left, right) -> left,
                      LinkedHashMap::new));
      result.put(basisDate, itemsByStockId);
    }
    return result;
  }

  private List<DividendYieldGroupView> sortYieldRows(Iterable<YieldAccumulator> accumulators) {
    List<DividendYieldGroupView> rows = new ArrayList<>();
    for (YieldAccumulator accumulator : accumulators) {
      if (accumulator.hasData()) {
        rows.add(accumulator.toView());
      }
    }
    rows.sort(
        Comparator.comparing(
                DividendYieldGroupView::yieldOnDailyAverageCostPct,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                DividendYieldGroupView::yieldOnCostPct,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                DividendYieldGroupView::totalNetAmount,
                Comparator.nullsLast(Comparator.reverseOrder())));
    return rows;
  }

  /**
   * 요청의 <b>배타적</b> 종료 시각을 화면이 쓰는 <b>포함</b> 종료일로 바꾼다.
   *
   * <p>게이트는 종료일을 "그 다음 날 00:00" 으로 실어 보낸다. 그대로 날짜만 떼면 하루가 더 세어져 기간 일수가 1 늘고, 그 일수가 일평균 원금의 분모라 수익률이
   * 그만큼 낮아진다. 1 나노초를 빼서 실제 마지막 날을 얻는다.
   */
  static LocalDate resolvePeriodEndDate(Instant endInstant, LocalDate fallback, ZoneId zone) {
    if (endInstant != null) {
      return endInstant.minusNanos(1).atZone(zone).toLocalDate();
    }
    return fallback;
  }

  /** 기간을 연도별 일수로 쪼갠다(양끝 포함). 연 환산 수익률과 일평균 원금이 이 값을 쓴다. */
  static Map<Integer, Long> buildPeriodDayCountsByYear(
      LocalDate periodStartDate, LocalDate periodEndDate) {
    Map<Integer, Long> dayCountsByYear = new LinkedHashMap<>();
    if (periodStartDate == null
        || periodEndDate == null
        || periodEndDate.isBefore(periodStartDate)) {
      return dayCountsByYear;
    }

    LocalDate currentDate = periodStartDate;
    while (!currentDate.isAfter(periodEndDate)) {
      dayCountsByYear.merge(currentDate.getYear(), 1L, Long::sum);
      currentDate = currentDate.plusDays(1);
    }
    return dayCountsByYear;
  }

  private static PeriodPrincipalSummary summarizePeriodPrincipalCosts(
      List<TradeResponse> tradeList,
      LocalDate periodStartDate,
      LocalDate periodEndDate,
      ZoneId zone) {
    if (periodStartDate == null
        || periodEndDate == null
        || periodEndDate.isBefore(periodStartDate)) {
      return PeriodPrincipalSummary.empty();
    }

    CostBasisState costBasisState = new CostBasisState();
    int tradeIndex = 0;
    // 아래 일별 루프는 하루마다 '다음 거래가 오늘 것인지' 들여다본다. 그 자리에서 Instant 를 매번
    // LocalDate 로 바꾸면 같은 값을 하루에 한 번씩 다시 계산한다 — 전체 기간이면 6,100 일이고
    // 이 메서드는 포지션마다 다시 돌기 때문에 변환이 곱절로 늘어난다(실측: 이 한 줄이 배당 화면
    // 프래그먼트 표본의 9.8%). 커서가 옮겨갈 때만 계산해 들고 있는다.
    LocalDate nextTradeDate =
        tradeList.isEmpty() ? null : tradeList.get(0).tradeDate().atZone(zone).toLocalDate();
    while (tradeIndex < tradeList.size()) {
      if (!nextTradeDate.isBefore(periodStartDate)) {
        break;
      }
      costBasisState.apply(tradeList.get(tradeIndex));
      tradeIndex++;
      nextTradeDate =
          tradeIndex < tradeList.size()
              ? tradeList.get(tradeIndex).tradeDate().atZone(zone).toLocalDate()
              : null;
    }

    BigDecimal principalCostSum = BigDecimal.ZERO;
    Map<Integer, BigDecimal> principalCostSumByYear = new LinkedHashMap<>();
    BigDecimal startPrincipalCost = null;
    BigDecimal endPrincipalCost = null;
    LocalDate currentDate = periodStartDate;
    while (!currentDate.isAfter(periodEndDate)) {
      while (tradeIndex < tradeList.size()) {
        if (nextTradeDate.isAfter(currentDate)) {
          break;
        }
        costBasisState.apply(tradeList.get(tradeIndex));
        tradeIndex++;
        nextTradeDate =
            tradeIndex < tradeList.size()
                ? tradeList.get(tradeIndex).tradeDate().atZone(zone).toLocalDate()
                : null;
      }

      BigDecimal principalCost = principalCostForState(costBasisState);
      if (principalCost != null && principalCost.compareTo(BigDecimal.ZERO) > 0) {
        principalCostSum = principalCostSum.add(principalCost);
        principalCostSumByYear.merge(currentDate.getYear(), principalCost, BigDecimal::add);
      }
      // 기간 중 원금 변동 표시용: 첫날/마지막날 시점의 투입원금.
      BigDecimal dayPrincipal = principalCost != null ? principalCost : BigDecimal.ZERO;
      if (startPrincipalCost == null) {
        startPrincipalCost = dayPrincipal;
      }
      endPrincipalCost = dayPrincipal;

      currentDate = currentDate.plusDays(1);
    }

    return new PeriodPrincipalSummary(
        principalCostSum, principalCostSumByYear, startPrincipalCost, endPrincipalCost);
  }

  /** 배당의 기준일. 기준일이 없으면 지급일로 본다. */
  static LocalDate resolveBasisDate(DividendView dividend, ZoneId zone) {
    Instant basisInstant =
        dividend.recordDate() != null ? dividend.recordDate() : dividend.payDate();
    return basisInstant != null ? basisInstant.atZone(zone).toLocalDate() : null;
  }

  static BigDecimal multiplyQuantity(BigDecimal price, Integer quantity) {
    if (price == null || quantity == null || quantity <= 0) {
      return null;
    }
    return price.multiply(BigDecimal.valueOf(quantity));
  }

  private static BigDecimal principalCostForState(CostBasisState costBasisState) {
    BigDecimal averageCost = costBasisState.averageCost();
    if (averageCost == null || costBasisState.rawQuantity() <= 0) {
      return null;
    }
    return averageCost.multiply(BigDecimal.valueOf(costBasisState.rawQuantity()));
  }

  /**
   * 필터 목록에 해당 id 가 포함되는가.
   *
   * <p>{@code null} 은 "필터 없음"(전부 통과), <b>빈 목록</b>은 "필터를 걸었는데 해당이 없음"(전부 제외)이다. 이 구분이 무너지면 필터를 걸었는데
   * 전체가 나온다.
   *
   * <p>같은 화면 안에서 이 규칙이 갈려 있었다. 이번 기간은 {@code list == null || list.contains(id)} 로 빈 목록을 "해당 없음" 으로
   * 봤는데, 전기 비교는 {@code || list.isEmpty()} 가 붙어 "필터 없음" 으로 봤다. 그래서 해당이 없는 필터를 고르면 <b>이번 기간 0 원 vs 전기
   * 전체 금액</b> 이 되어 증감이 통째로 잘못 나왔다. 판정을 한 곳으로 모아 갈릴 수 없게 한다.
   */
  static boolean matchesFilter(List<UUID> filter, UUID id) {
    if (filter == null) {
      return true;
    }
    return id != null && filter.contains(id);
  }

  static BigDecimal percentage(BigDecimal amount, BigDecimal principal) {
    if (amount == null || principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return amount.multiply(BigDecimal.valueOf(100)).divide(principal, 4, RoundingMode.HALF_UP);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private record PositionKey(UUID accountId, UUID stockItemId) {}

  /** 전기 대비 종목별 배당 증감(변동 요인 분해)용. */
  public record DividendChange(
      String stockName,
      BigDecimal currentNet,
      BigDecimal previousNet,
      BigDecimal delta,
      UUID stockItemId) {}

  /**
   * 기간 투입원금 요약.
   *
   * <p>principalCostSum 은 일별 투입원금의 합(시간가중 평균의 분자). startPrincipalCost/endPrincipalCost 는 기간
   * 첫날/마지막날의 투입원금으로, 기간 중 원금이 얼마나 변했는지 보여주는 용도다.
   */
  private record PeriodPrincipalSummary(
      BigDecimal principalCostSum,
      Map<Integer, BigDecimal> principalCostSumByYear,
      BigDecimal startPrincipalCost,
      BigDecimal endPrincipalCost) {

    private static PeriodPrincipalSummary empty() {
      return new PeriodPrincipalSummary(BigDecimal.ZERO, Map.of(), null, null);
    }
  }

  private record DividendAnalyticsResult(
      List<DividendView> dividendViews,
      DividendYieldGroupView portfolioYield,
      List<DividendYieldGroupView> yearlyYieldRows,
      List<DividendYieldGroupView> stockYieldRows,
      List<DividendYieldGroupView> accountYieldRows,
      BigDecimal periodStartPrincipal,
      BigDecimal periodEndPrincipal,
      long periodDayCount) {

    private static DividendAnalyticsResult empty(List<DividendView> dividendViews) {
      return new DividendAnalyticsResult(
          dividendViews, null, List.of(), List.of(), List.of(), null, null, 0L);
    }
  }

  /**
   * 배당 기준일 시점의 보유 원금을 거래로 되짚어 만드는 상태.
   *
   * <p>이 원금이 배당수익률의 <b>분모</b>다. 테스트에서 직접 만들 수 있도록 package-private 로 둔다.
   */
  static final class CostBasisState {
    private long rawQuantity;
    private BigDecimal totalCost = BigDecimal.ZERO;

    void apply(TradeResponse trade) {
      int quantity = trade.quantity();
      if (quantity <= 0) {
        return;
      }

      BigDecimal amount = nz(trade.price()).multiply(BigDecimal.valueOf(quantity));
      if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.BUY) {
        rawQuantity += quantity;
        totalCost = totalCost.add(amount);
        return;
      }

      if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.SELL && rawQuantity > 0) {
        // 매도로 빠져나가는 원가는 증권사 기록 실현손익에서 역산한다. 기록 실현손익의 정의는
        // (매도금액 - 원가 - 세금) 으로 <b>매도 수수료는 빼지 않는다</b>. 실측(사용자 매도 중 실현손익이
        // 기록된 54건 전부): 40건이 이 정의와 1원 이내로 일치했고, 수수료까지 뺀 정의와 일치한 건은 0건.
        //
        // 그래서 매도 '실수령'(수수료까지 뺀 금액)에서 역산하면 COGS 가 수수료만큼 작아지고 그만큼
        // 원금이 부풀어 남는다. 실측: 삼성전자 원금이 362,531,274 로 계산돼 api-stock 의 362,525,079
        // 보다 6,195 컸다(= 마지막 전량매도 이후 매도 수수료 4,611 + 1,584). 이 원금은 배당수익률의
        // 분모이므로 수익률이 그만큼 낮게 표시된다. api-stock 은 같은 계산을 이미 고쳤다.
        BigDecimal cogs = amount.subtract(nz(trade.tax())).subtract(nz(trade.realizedProfit()));
        totalCost = totalCost.subtract(cogs);
        if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
          totalCost = BigDecimal.ZERO;
        }
        rawQuantity -= quantity;
        if (rawQuantity <= 0) {
          rawQuantity = 0;
          totalCost = BigDecimal.ZERO;
        }
      }
    }

    long rawQuantity() {
      return rawQuantity;
    }

    BigDecimal averageCost() {
      if (rawQuantity <= 0 || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
        return null;
      }
      return totalCost.divide(BigDecimal.valueOf(rawQuantity), 2, RoundingMode.HALF_UP);
    }
  }

  /**
   * 배당수익률 한 행(포트폴리오/연도/계좌/종목)을 모으는 누산기.
   *
   * <p>테스트에서 직접 만들 수 있도록 package-private 로 둔다. 이 계산은 화면의 핵심 수치인데 검증이 하나도 없었다.
   */
  static final class YieldAccumulator {
    private final String label;

    /** 상세 링크용 id. 이름으로 되찾으면 같은 이름이 둘일 때 엉뚱한 대상으로 연결된다(연도·포트폴리오 행은 null). */
    private final UUID groupId;

    private final long periodDayCount;
    private BigDecimal totalGrossAmount = BigDecimal.ZERO;
    private BigDecimal totalNetAmount = BigDecimal.ZERO;
    private BigDecimal totalTaxableAmount = BigDecimal.ZERO;
    private BigDecimal dailyPrincipalCostSum = BigDecimal.ZERO;
    private BigDecimal netAmountWithPrincipalCost = BigDecimal.ZERO;
    private BigDecimal netAmountWithPrincipalMarket = BigDecimal.ZERO;
    private final Map<PositionKey, PrincipalAccumulator> principalByPosition =
        new LinkedHashMap<>();
    private long dividendCount;
    private Instant lastDividendDate;

    YieldAccumulator(String label, long periodDayCount) {
      this(label, null, periodDayCount);
    }

    YieldAccumulator(String label, UUID groupId, long periodDayCount) {
      this.label = label;
      this.groupId = groupId;
      this.periodDayCount = periodDayCount;
    }

    void accept(DividendView dividend) {
      totalGrossAmount = totalGrossAmount.add(nz(dividend.grossAmount()));
      totalNetAmount = totalNetAmount.add(nz(dividend.netAmount()));
      totalTaxableAmount = totalTaxableAmount.add(nz(dividend.taxableAmount()));
      dividendCount++;

      // 기준일 원금이 없는 배당(지급일에 이미 전량 매도한 경우 등)은 분모(기준일 평균원금)에
      // 기여하지 않는다. 그런데 분자에 전액을 넣으면 수익률이 과대 계상된다
      // (실측: 193건 중 5건·세후 144,360원이 분모 없이 분자에만 들어가 7.12% 가 7.14% 로 보였다.
      //  이 데이터에선 0.02%p 지만, 매도한 포지션의 비중이 큰 사용자에겐 커진다).
      // 분모에 기여한 배당만 따로 합산해 같은 모집단끼리 나눈다.
      if (dividend.principalCost() != null
          && dividend.principalCost().compareTo(BigDecimal.ZERO) > 0) {
        netAmountWithPrincipalCost = netAmountWithPrincipalCost.add(nz(dividend.netAmount()));
      }
      if (dividend.principalMarketValue() != null
          && dividend.principalMarketValue().compareTo(BigDecimal.ZERO) > 0) {
        netAmountWithPrincipalMarket = netAmountWithPrincipalMarket.add(nz(dividend.netAmount()));
      }

      if (dividend.accountId() != null && dividend.stockItemId() != null) {
        principalByPosition
            .computeIfAbsent(
                new PositionKey(dividend.accountId(), dividend.stockItemId()),
                ignored -> new PrincipalAccumulator())
            .accept(dividend);
      }

      Instant displayDate = dividend.payDate() != null ? dividend.payDate() : dividend.recordDate();
      if (displayDate != null
          && (lastDividendDate == null || displayDate.isAfter(lastDividendDate))) {
        lastDividendDate = displayDate;
      }
    }

    void acceptDailyPrincipalCostSum(BigDecimal principalCostSum) {
      if (principalCostSum != null && principalCostSum.compareTo(BigDecimal.ZERO) > 0) {
        dailyPrincipalCostSum = dailyPrincipalCostSum.add(principalCostSum);
      }
    }

    boolean hasData() {
      return dividendCount > 0;
    }

    DividendYieldGroupView toView() {
      BigDecimal averageDailyPrincipalCost =
          periodDayCount > 0 && dailyPrincipalCostSum.compareTo(BigDecimal.ZERO) > 0
              ? dailyPrincipalCostSum.divide(
                  BigDecimal.valueOf(periodDayCount), 2, RoundingMode.HALF_UP)
              : null;
      BigDecimal averagePrincipalCost = null;
      BigDecimal averagePrincipalMarketValue = null;

      for (PrincipalAccumulator principalAccumulator : principalByPosition.values()) {
        BigDecimal positionAveragePrincipalCost = principalAccumulator.averagePrincipalCost();
        if (positionAveragePrincipalCost != null) {
          averagePrincipalCost =
              averagePrincipalCost == null
                  ? positionAveragePrincipalCost
                  : averagePrincipalCost.add(positionAveragePrincipalCost);
        }

        BigDecimal positionAveragePrincipalMarketValue =
            principalAccumulator.averagePrincipalMarketValue();
        if (positionAveragePrincipalMarketValue != null) {
          averagePrincipalMarketValue =
              averagePrincipalMarketValue == null
                  ? positionAveragePrincipalMarketValue
                  : averagePrincipalMarketValue.add(positionAveragePrincipalMarketValue);
        }
      }

      // 분자는 세 수익률이 같은 규칙을 쓴다 - 기준일 원금이 있는 배당만 넣는다.
      //
      // 기준일에 원금이 있었다는 것은 그 날 그 종목을 들고 있었다는 뜻이므로, 그 배당은 일수 합계
      // (dailyPrincipalCostSum)에도 반드시 기여했다. 반대로 기준일 원금이 없는 배당은 기간에 따라
      // 일수 합계에 기여했을 수도, 전혀 아닐 수도 있다 - 예컨대 NAVER 는 2021-01-18 에 전량 매도했는데
      // 배당이 2021-04-08 에 기록돼 있어, 4월만 보는 기간에서는 원금이 하루도 없는데 배당만 분자에 들어간다.
      // 그런 배당을 빼면 어떤 기간에서도 분모에 기여한 것만 분자에 남는다(실측 전 기간 기준 5건 144,360원).
      BigDecimal yieldOnDailyAverageCostPct =
          averageDailyPrincipalCost != null
              ? percentage(netAmountWithPrincipalCost, averageDailyPrincipalCost)
              : null;
      BigDecimal yieldOnCostPct =
          averagePrincipalCost != null
              ? percentage(netAmountWithPrincipalCost, averagePrincipalCost)
              : null;
      BigDecimal yieldOnMarketPct =
          averagePrincipalMarketValue != null
              ? percentage(netAmountWithPrincipalMarket, averagePrincipalMarketValue)
              : null;
      return new DividendYieldGroupView(
          groupId,
          label,
          totalGrossAmount,
          totalNetAmount,
          totalTaxableAmount,
          netAmountWithPrincipalCost,
          netAmountWithPrincipalMarket,
          averageDailyPrincipalCost,
          averagePrincipalCost,
          averagePrincipalMarketValue,
          yieldOnDailyAverageCostPct,
          yieldOnCostPct,
          yieldOnMarketPct,
          dividendCount,
          lastDividendDate);
    }
  }

  static final class PrincipalAccumulator {
    private BigDecimal principalCostSum = BigDecimal.ZERO;
    private long principalCostCount;
    private BigDecimal principalMarketSum = BigDecimal.ZERO;
    private long principalMarketCount;

    private void accept(DividendView dividend) {
      if (dividend.principalCost() != null
          && dividend.principalCost().compareTo(BigDecimal.ZERO) > 0) {
        principalCostSum = principalCostSum.add(dividend.principalCost());
        principalCostCount++;
      }
      if (dividend.principalMarketValue() != null
          && dividend.principalMarketValue().compareTo(BigDecimal.ZERO) > 0) {
        principalMarketSum = principalMarketSum.add(dividend.principalMarketValue());
        principalMarketCount++;
      }
    }

    private BigDecimal averagePrincipalCost() {
      if (principalCostCount <= 0) {
        return null;
      }
      return principalCostSum.divide(
          BigDecimal.valueOf(principalCostCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averagePrincipalMarketValue() {
      if (principalMarketCount <= 0) {
        return null;
      }
      return principalMarketSum.divide(
          BigDecimal.valueOf(principalMarketCount), 2, RoundingMode.HALF_UP);
    }
  }
}
