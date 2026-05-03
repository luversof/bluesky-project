package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

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
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockTradeHtmxController extends StockBaseHtmxController {

        public StockTradeHtmxController(
                        TradeProfitClient tradeProfitClient,
                        TradeClient tradeClient,
                        AccountClient accountClient,
                        StockItemClient stockItemClient,
                        DividendClient dividendClient,
                        MessageSource messageSource) {
                super(tradeProfitClient, tradeClient, accountClient, stockItemClient, dividendClient, messageSource);
        }

        @BlueskyPreAuthorize
        @GetMapping("/trade/list")
        public String tradeList(
                        @RequestParam(required = false) List<UUID> accountIdList,
                        @RequestParam(required = false) List<UUID> stockItemIdList,
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
                        model.addAttribute(ERROR_ATTRIBUTE, msg("stock.label.login.required"));
                        return ERROR_VIEW;
                }

                // If no date range provided by client, default to this year (ytd)
                Instant startInst = startDate;
                Instant endInst = endDate;
                if (startInst == null && endInst == null && (rangeMode == null || rangeMode.isBlank())) {
                        ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone)
                                        : ZoneId.systemDefault();
                        LocalDate now = LocalDate.now(zone);
                        startInst = LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant();
                        endInst = now.plusDays(1).atStartOfDay(zone).toInstant();
                        rangeMode = "ytd";
                }

                TradeSearchRequest request = new TradeSearchRequest(userId, null, null, startInst, endInst);
                List<TradeResponse> allFromApi = tradeClient.findTrades(request.toParams());

                List<TradeResponse> globalTrades = (startDate == null && endDate == null)
                                ? allFromApi
                                : tradeClient.findTrades(
                                                new TradeSearchRequest(userId, null, null, null, null).toParams());
                ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
                LocalDate dataFirstDate = globalTrades.stream()
                                .filter(t -> t.tradeDate() != null)
                                .map(t -> t.tradeDate().atZone(zone).toLocalDate())
                                .min(Comparator.naturalOrder())
                                .orElse(null);

                List<Account> accountList = accountClient.getAccountsByUserId(userId);
                Map<UUID, String> accountNames = accountList.stream()
                                .collect(Collectors.toMap(Account::id, Account::name, (l, r) -> l, LinkedHashMap::new));

                List<StockItem> stockItemList = stockItemClient.getStockItems();
                Map<UUID, String> stockItemNames = stockItemList.stream()
                                .collect(Collectors.toMap(StockItem::id, StockItem::name));

                List<TradeResponse> enrichedAll = allFromApi.stream()
                                .map(
                                                t -> new TradeResponse(
                                                                t.id(),
                                                                t.accountId(),
                                                                t.stockItemId(),
                                                                stockItemNames.getOrDefault(t.stockItemId(),
                                                                                msg("stock.label.unknown")),
                                                                t.type(),
                                                                t.quantity(),
                                                                t.price(),
                                                                t.fee(),
                                                                t.tax(),
                                                                t.amount(),
                                                                t.realizedProfit(),
                                                                t.tradeDate()))
                                .collect(Collectors.toCollection(ArrayList::new));

                // Determine if client actually provided a date range (before defaults)
                boolean clientProvidedRange = !((startDate == null || startDate.toEpochMilli() == 0)
                                && (endDate == null || endDate.toEpochMilli() == 0)
                                && (rangeMode == null || rangeMode.isBlank()));

                // Prepare filtered lists based on date-only availability (do NOT include
                // user's account/stock filters when computing availability)
                List<Account> filteredAccountList;
                List<StockItem> filteredStockItemList;
                Set<UUID> tradeAccountIds;
                Set<UUID> tradeStockIds;
                if (clientProvidedRange) {
                        net.luversof.web.gate.stock.dto.request.TradeProfitRequest dateOnlyReq = new net.luversof.web.gate.stock.dto.request.TradeProfitRequest();
                        dateOnlyReq.setUserId(userId);
                        dateOnlyReq.setStartDate(startDate);
                        dateOnlyReq.setEndDate(endDate);
                        dateOnlyReq.setTimeZone(timeZone);
                        List<net.luversof.web.gate.stock.domain.TradeProfit> dateRangeEnriched = new ArrayList<>(
                                        getEnrichedTradeProfits(dateOnlyReq));
                        tradeAccountIds = dateRangeEnriched.stream()
                                        .map(net.luversof.web.gate.stock.domain.TradeProfit::accountId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());
                        tradeStockIds = dateRangeEnriched.stream()
                                        .map(net.luversof.web.gate.stock.domain.TradeProfit::stockItemId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());

                        filteredAccountList = accountList.stream().filter(a -> tradeAccountIds.contains(a.id()))
                                        .toList();
                        filteredStockItemList = stockItemList.stream()
                                        .filter(s -> tradeStockIds.contains(s.id())).toList();
                } else {
                        tradeAccountIds = Collections.emptySet();
                        tradeStockIds = Collections.emptySet();
                        filteredAccountList = accountList;
                        filteredStockItemList = stockItemList;
                }

                // Validate requested filters against the full available lists (not the
                // filtered-by-date lists) so we can preserve previously selected items
                Set<UUID> availableAccountIds = accountList.stream().map(Account::id).collect(Collectors.toSet());
                List<UUID> requestedAccountIds = accountIdList;
                List<UUID> effectiveAccountIdList = (requestedAccountIds != null && !requestedAccountIds.isEmpty()
                                && availableAccountIds.containsAll(requestedAccountIds))
                                                ? requestedAccountIds
                                                : null;

                Set<UUID> availableStockIds = stockItemList.stream().map(StockItem::id).collect(Collectors.toSet());
                List<UUID> requestedStockItemIds = stockItemIdList;
                List<UUID> effectiveStockItemIdList = (requestedStockItemIds != null && !requestedStockItemIds.isEmpty()
                                && availableStockIds.containsAll(requestedStockItemIds))
                                                ? requestedStockItemIds
                                                : null;

                // Build final lists for UI selects: when client provided a range, show
                // only date-available items but prepend any previously selected items
                // that don't appear in the date-available set so the selection doesn't
                // disappear.
                List<Account> finalAccountList;
                if (clientProvidedRange) {
                        finalAccountList = new ArrayList<>(filteredAccountList);
                        if (requestedAccountIds != null) {
                                for (UUID sel : requestedAccountIds) {
                                        if (sel == null)
                                                continue;
                                        if (!tradeAccountIds.contains(sel)) {
                                                accountList.stream().filter(a -> a.id().equals(sel)).findFirst()
                                                                .ifPresent(a -> {
                                                                        if (finalAccountList.stream()
                                                                                        .noneMatch(x -> x.id().equals(
                                                                                                        a.id())))
                                                                                finalAccountList.add(0, a);
                                                                });
                                        }
                                }
                        }
                } else {
                        finalAccountList = accountList;
                }

                List<StockItem> finalStockItemList;
                if (clientProvidedRange) {
                        finalStockItemList = new ArrayList<>(filteredStockItemList);
                        if (requestedStockItemIds != null) {
                                for (UUID sel : requestedStockItemIds) {
                                        if (sel == null)
                                                continue;
                                        if (!tradeStockIds.contains(sel)) {
                                                stockItemList.stream().filter(s -> s.id().equals(sel)).findFirst()
                                                                .ifPresent(s -> {
                                                                        if (finalStockItemList.stream()
                                                                                        .noneMatch(x -> x.id().equals(
                                                                                                        s.id())))
                                                                                finalStockItemList.add(0, s);
                                                                });
                                        }
                                }
                        }
                } else {
                        finalStockItemList = stockItemList;
                }

                List<TradeResponse> viewList = enrichedAll.stream()
                                .filter(
                                                t -> effectiveAccountIdList == null
                                                                || effectiveAccountIdList.isEmpty()
                                                                || effectiveAccountIdList.contains(t.accountId()))
                                .filter(
                                                t -> effectiveStockItemIdList == null
                                                                || effectiveStockItemIdList.isEmpty()
                                                                || effectiveStockItemIdList.contains(t.stockItemId()))
                                .collect(Collectors.toCollection(ArrayList::new));

                if (sort != null && !sort.isEmpty()) {
                        String[] parts = sort.split(",");
                        String field = parts[0];
                        String direction = parts.length > 1 ? parts[1] : "asc";
                        Comparator<TradeResponse> comparator = switch (field) {
                                case "tradeDate" ->
                                        Comparator.comparing(
                                                        TradeResponse::tradeDate,
                                                        Comparator.nullsLast(Comparator.naturalOrder()));
                                case "stockItemName" ->
                                        Comparator.comparing(
                                                        TradeResponse::stockItemName,
                                                        Comparator.nullsLast(Comparator.naturalOrder()));
                                case "amount" ->
                                        Comparator.comparing(
                                                        TradeResponse::amount,
                                                        Comparator.nullsLast(Comparator.naturalOrder()));
                                case "fee" ->
                                        Comparator.comparing(
                                                        TradeResponse::fee,
                                                        Comparator.nullsLast(Comparator.naturalOrder()));
                                case "realizedProfit" ->
                                        Comparator.comparing(
                                                        TradeResponse::realizedProfit,
                                                        Comparator.nullsLast(Comparator.naturalOrder()));
                                default -> null;
                        };
                        if (comparator != null) {
                                if ("desc".equalsIgnoreCase(direction))
                                        comparator = comparator.reversed();
                                viewList.sort(comparator);
                        }
                } else {
                        viewList.sort(
                                        Comparator.comparing(
                                                        TradeResponse::tradeDate,
                                                        Comparator.nullsLast(Comparator.reverseOrder())));
                }

                BigDecimal totalAllBuyAmount = viewList.stream()
                                .filter(t -> t.type() == TradeType.BUY)
                                .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAllSellAmount = viewList.stream()
                                .filter(t -> t.type() == TradeType.SELL)
                                .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAllFee = viewList.stream()
                                .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAllTax = viewList.stream()
                                .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAllRealizedProfit = viewList.stream()
                                .filter(t -> t.type() == TradeType.SELL)
                                .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (size <= 0)
                        size = 15;
                int totalItems = viewList.size();
                int totalPages = (int) Math.ceil((double) totalItems / size);
                int currentPage = Math.max(1, Math.min(page, totalPages));
                if (totalPages == 0)
                        currentPage = 1;

                int fromIndex = (currentPage - 1) * size;
                int toIndex = Math.min(fromIndex + size, totalItems);
                List<TradeResponse> pagedList = (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex)
                                : Collections.emptyList();

                BigDecimal totalFee = pagedList.stream()
                                .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalTax = pagedList.stream()
                                .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalRealizedProfit = pagedList.stream()
                                .filter(t -> t.type() == TradeType.SELL)
                                .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
                var pagination = new Pagination(pageImpl);

                model.addAttribute("tradeList", pagedList);
                model.addAttribute("allTradeList", viewList);
                model.addAttribute("pagination", pagination);
                model.addAttribute("totalItems", totalItems);
                model.addAttribute("totalPages", totalPages);
                model.addAttribute("currentPage", currentPage);
                model.addAttribute("size", size);
                model.addAttribute("accountList", finalAccountList);
                model.addAttribute("stockItemList", finalStockItemList);
                model.addAttribute("accountNames", accountNames);
                model.addAttribute(
                                "selectedAccountIds",
                                effectiveAccountIdList != null ? effectiveAccountIdList : List.of());
                model.addAttribute(
                                "selectedStockItemIds",
                                effectiveStockItemIdList != null ? effectiveStockItemIdList : List.of());
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
                model.addAttribute("startDate", startInst);
                model.addAttribute("endDate", endInst);
                // reflect rangeMode back into model (may have been defaulted above)
                model.addAttribute("rangeMode", rangeMode);
                model.addAttribute("timeZone", timeZone);
                model.addAttribute("sort", sort);
                model.addAttribute("totalFee", totalFee);
                model.addAttribute("totalTax", totalTax);
                model.addAttribute("totalRealizedProfit", totalRealizedProfit);
                model.addAttribute("totalAllBuyAmount", totalAllBuyAmount);
                model.addAttribute("totalAllSellAmount", totalAllSellAmount);
                model.addAttribute("totalAllFee", totalAllFee);
                model.addAttribute("totalAllTax", totalAllTax);
                model.addAttribute("totalAllRealizedProfit", totalAllRealizedProfit);
                model.addAttribute("rangeMode", rangeMode);
                model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");

                return "stock/htmx/tradeList";
        }

        public record Activity(
                        String type,
                        String stockItemName,
                        String tradeType,
                        Integer quantity,
                        String description,
                        BigDecimal amount,
                        Instant date,
                        List<String> accountNames) {
        }

        private List<Activity> getAllActivities(UUID userId) {
                return getAllActivities(userId, (Instant) null, (Instant) null);
        }

        private List<Activity> getAllActivities(UUID userId, LocalDate startDate, LocalDate endDate) {
                Instant startInstant = startDate != null ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                : null;
                Instant endInstant = endDate != null
                                ? endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                                : null;

                TradeSearchRequest tradeReq = new TradeSearchRequest(userId, null, null, startInstant, endInstant);
                List<TradeResponse> trades = tradeClient.findTrades(tradeReq.toParams());

                DividendRequest divReq = new DividendRequest();
                divReq.setUserId(userId);
                divReq.setStartDate(startInstant);
                divReq.setEndDate(endInstant);
                List<DividendResponse> dividends = dividendClient.findDividends(divReq.toParams());

                List<StockItem> stockItemList = stockItemClient.getStockItems();
                Map<UUID, String> stockItemNames = stockItemList.stream()
                                .collect(Collectors.toMap(StockItem::id, StockItem::name));

                List<Account> accountList = accountClient.getAccountsByUserId(userId);
                Map<UUID, String> accountNamesMap = accountList.stream()
                                .collect(Collectors.toMap(Account::id, Account::name));

                List<Activity> rawActivities = new ArrayList<>();

                for (TradeResponse t : trades) {
                        String stockName = stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "TRADE",
                                                        stockName,
                                                        t.type().name(),
                                                        t.quantity(),
                                                        null,
                                                        t.amount(),
                                                        t.tradeDate(),
                                                        List.of(accountName)));
                }

                for (DividendResponse d : dividends) {
                        String stockName = d.stockItemName() != null
                                        ? d.stockItemName()
                                        : stockItemNames.getOrDefault(d.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "DIVIDEND",
                                                        stockName,
                                                        null,
                                                        null,
                                                        "배당금지급",
                                                        d.netAmount(),
                                                        d.payDate() != null ? d.payDate() : d.recordDate(),
                                                        List.of(accountName)));
                }

                Map<String, Activity> groupedMap = new HashMap<>();
                for (Activity a : rawActivities) {
                        if (a.date() == null)
                                continue;

                        String dateStr = a.date().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
                        String key = String.format("%s|%s|%s|%s", dateStr, a.type(), a.stockItemName(), a.tradeType());

                        if (groupedMap.containsKey(key)) {
                                Activity existing = groupedMap.get(key);

                                Integer newQty = null;
                                if (existing.quantity() != null || a.quantity() != null) {
                                        newQty = (existing.quantity() != null ? existing.quantity() : 0)
                                                        + (a.quantity() != null ? a.quantity() : 0);
                                }

                                BigDecimal newAmount = null;
                                if (existing.amount() != null || a.amount() != null) {
                                        newAmount = (existing.amount() != null ? existing.amount() : BigDecimal.ZERO)
                                                        .add(a.amount() != null ? a.amount() : BigDecimal.ZERO);
                                }

                                List<String> newAccountNames = new ArrayList<>(existing.accountNames());
                                if (!newAccountNames.contains(a.accountNames().get(0))) {
                                        newAccountNames.add(a.accountNames().get(0));
                                }

                                groupedMap.put(
                                                key,
                                                new Activity(
                                                                existing.type(),
                                                                existing.stockItemName(),
                                                                existing.tradeType(),
                                                                newQty,
                                                                existing.description(),
                                                                newAmount,
                                                                existing.date(),
                                                                newAccountNames));
                        } else {
                                groupedMap.put(key, a);
                        }
                }

                List<Activity> activities = new ArrayList<>(groupedMap.values());
                activities.sort(
                                Comparator.comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder())));
                return activities;
        }

        private List<Activity> getAllActivities(UUID userId, Instant startInstant, Instant endInstant) {
                TradeSearchRequest tradeReq = new TradeSearchRequest(userId, null, null, startInstant, endInstant);
                List<TradeResponse> trades = tradeClient.findTrades(tradeReq.toParams());

                DividendRequest divReq = new DividendRequest();
                divReq.setUserId(userId);
                divReq.setStartDate(startInstant);
                divReq.setEndDate(endInstant);
                List<DividendResponse> dividends = dividendClient.findDividends(divReq.toParams());

                List<StockItem> stockItemList = stockItemClient.getStockItems();
                Map<UUID, String> stockItemNames = stockItemList.stream()
                                .collect(Collectors.toMap(StockItem::id, StockItem::name));

                List<Account> accountList = accountClient.getAccountsByUserId(userId);
                Map<UUID, String> accountNamesMap = accountList.stream()
                                .collect(Collectors.toMap(Account::id, Account::name));

                List<Activity> rawActivities = new ArrayList<>();

                for (TradeResponse t : trades) {
                        String stockName = stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "TRADE",
                                                        stockName,
                                                        t.type().name(),
                                                        t.quantity(),
                                                        null,
                                                        t.amount(),
                                                        t.tradeDate(),
                                                        List.of(accountName)));
                }

                for (DividendResponse d : dividends) {
                        String stockName = d.stockItemName() != null
                                        ? d.stockItemName()
                                        : stockItemNames.getOrDefault(d.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "DIVIDEND",
                                                        stockName,
                                                        null,
                                                        null,
                                                        "배당금지급",
                                                        d.netAmount(),
                                                        d.payDate() != null ? d.payDate() : d.recordDate(),
                                                        List.of(accountName)));
                }

                Map<String, Activity> groupedMap = new HashMap<>();
                for (Activity a : rawActivities) {
                        if (a.date() == null)
                                continue;

                        String dateStr = a.date().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
                        String key = String.format("%s|%s|%s|%s", dateStr, a.type(), a.stockItemName(), a.tradeType());

                        if (groupedMap.containsKey(key)) {
                                Activity existing = groupedMap.get(key);

                                Integer newQty = null;
                                if (existing.quantity() != null || a.quantity() != null) {
                                        newQty = (existing.quantity() != null ? existing.quantity() : 0)
                                                        + (a.quantity() != null ? a.quantity() : 0);
                                }

                                BigDecimal newAmount = null;
                                if (existing.amount() != null || a.amount() != null) {
                                        newAmount = (existing.amount() != null ? existing.amount() : BigDecimal.ZERO)
                                                        .add(a.amount() != null ? a.amount() : BigDecimal.ZERO);
                                }

                                List<String> newAccountNames = new ArrayList<>(existing.accountNames());
                                if (!newAccountNames.contains(a.accountNames().get(0))) {
                                        newAccountNames.add(a.accountNames().get(0));
                                }

                                groupedMap.put(
                                                key,
                                                new Activity(
                                                                existing.type(),
                                                                existing.stockItemName(),
                                                                existing.tradeType(),
                                                                newQty,
                                                                existing.description(),
                                                                newAmount,
                                                                existing.date(),
                                                                newAccountNames));
                        } else {
                                groupedMap.put(key, a);
                        }
                }

                List<Activity> activities = new ArrayList<>(groupedMap.values());
                activities.sort(
                                Comparator.comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder())));
                return activities;
        }

        @BlueskyPreAuthorize
        @GetMapping("/recent-activities")
        public String recentActivities(Model model) {
                UUID userId = UserUtil.getUserId();
                if (userId == null)
                        return ERROR_VIEW;

                List<Activity> activities = getAllActivities(userId);

                // 이번 달 요약
                LocalDate now = LocalDate.now();
                LocalDate monthStart = now.withDayOfMonth(1);
                Instant monthStartInst = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant monthEndInst = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                List<Activity> thisMonth = activities.stream()
                                .filter(
                                                a -> a.date() != null
                                                                && !a.date().isBefore(monthStartInst)
                                                                && a.date().isBefore(monthEndInst))
                                .toList();

                long buyCount = thisMonth.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
                                .count();
                long sellCount = thisMonth.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
                                .count();
                BigDecimal buyAmount = thisMonth.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sellAmount = thisMonth.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                long dividendCount = thisMonth.stream().filter(a -> "DIVIDEND".equals(a.type())).count();
                BigDecimal dividendAmount = thisMonth.stream()
                                .filter(a -> "DIVIDEND".equals(a.type()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                model.addAttribute("activities", activities.stream().limit(5).toList());
                model.addAttribute("thisMonthLabel", now.getMonthValue() + "월");
                model.addAttribute("buyCount", buyCount);
                model.addAttribute("sellCount", sellCount);
                model.addAttribute("buyAmount", buyAmount);
                model.addAttribute("sellAmount", sellAmount);
                model.addAttribute("dividendCount", dividendCount);
                model.addAttribute("dividendAmount", dividendAmount);
                return "stock/htmx/fragments/recentActivities";
        }

        @BlueskyPreAuthorize
        @GetMapping("/activity-list")
        public String activityList(
                        @RequestParam(required = false) List<UUID> accountIdList,
                        @RequestParam(required = false) List<UUID> stockItemIdList,
                        @RequestParam(required = false) Instant startDate,
                        @RequestParam(required = false) Instant endDate,
                        @RequestParam(required = false) String timeZone,
                        @RequestParam(required = false) String rangeMode,
                        Model model) {
                UUID userId = UserUtil.getUserId();
                if (userId == null)
                        return ERROR_VIEW;

                List<Activity> allActivities = getAllActivities(userId);
                ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
                LocalDate dataFirstDate = allActivities.stream()
                                .filter(a -> a.date() != null)
                                .map(a -> a.date().atZone(zone).toLocalDate())
                                .min(Comparator.naturalOrder())
                                .orElse(null);

                // Convert start/end Instants into Instants for the helper (they already are
                // Instants)
                Instant startInstant = startDate;
                Instant endInstant = endDate;
                // Default to this year when client didn't provide range
                if (startInstant == null && endInstant == null && (rangeMode == null || rangeMode.isBlank())) {
                        LocalDate now = LocalDate.now(zone);
                        startInstant = LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant();
                        endInstant = now.plusDays(1).atStartOfDay(zone).toInstant();
                        rangeMode = "ytd";
                }
                List<Activity> activities = getAllActivities(userId, startInstant, endInstant, accountIdList,
                                stockItemIdList);

                // provide account/stock lists for filter UI
                List<Account> accountList = accountClient.getAccountsByUserId(userId);
                List<StockItem> stockItemList = stockItemClient.getStockItems();

                // Determine date-only availability for activities (trades + dividends)
                boolean clientProvidedRangeForActivity = !((startInstant == null || startInstant.toEpochMilli() == 0)
                                && (endInstant == null || endInstant.toEpochMilli() == 0)
                                && (rangeMode == null || rangeMode.isBlank()));

                List<Account> filteredAccountList;
                List<StockItem> filteredStockItemList;
                java.util.Set<UUID> activityAccountIds = new java.util.HashSet<>();
                java.util.Set<UUID> activityStockIds = new java.util.HashSet<>();
                if (clientProvidedRangeForActivity) {
                        net.luversof.web.gate.stock.dto.request.TradeProfitRequest dateOnlyReq = new net.luversof.web.gate.stock.dto.request.TradeProfitRequest();
                        dateOnlyReq.setUserId(userId);
                        dateOnlyReq.setStartDate(startInstant);
                        dateOnlyReq.setEndDate(endInstant);
                        dateOnlyReq.setTimeZone(timeZone);
                        List<net.luversof.web.gate.stock.domain.TradeProfit> dateRangeEnriched = new ArrayList<>(
                                        getEnrichedTradeProfits(dateOnlyReq));
                        activityAccountIds.addAll(dateRangeEnriched.stream()
                                        .map(net.luversof.web.gate.stock.domain.TradeProfit::accountId)
                                        .filter(Objects::nonNull).collect(Collectors.toSet()));
                        activityStockIds.addAll(dateRangeEnriched.stream()
                                        .map(net.luversof.web.gate.stock.domain.TradeProfit::stockItemId)
                                        .filter(Objects::nonNull).collect(Collectors.toSet()));

                        var divReq = new net.luversof.web.gate.stock.dto.request.DividendRequest();
                        divReq.setUserId(userId);
                        divReq.setStartDate(startInstant);
                        divReq.setEndDate(endInstant);
                        List<net.luversof.web.gate.stock.dto.response.DividendResponse> dateDivs = dividendClient
                                        .findDividends(divReq.toParams());
                        activityAccountIds.addAll(dateDivs.stream()
                                        .map(net.luversof.web.gate.stock.dto.response.DividendResponse::accountId)
                                        .filter(Objects::nonNull).collect(Collectors.toSet()));
                        activityStockIds.addAll(dateDivs.stream()
                                        .map(net.luversof.web.gate.stock.dto.response.DividendResponse::stockItemId)
                                        .filter(Objects::nonNull).collect(Collectors.toSet()));

                        filteredAccountList = accountList.stream().filter(a -> activityAccountIds.contains(a.id()))
                                        .toList();
                        filteredStockItemList = stockItemList.stream().filter(s -> activityStockIds.contains(s.id()))
                                        .toList();
                } else {
                        filteredAccountList = accountList;
                        filteredStockItemList = stockItemList;
                }

                // Validate requested filters against full lists so selection can be preserved
                Set<UUID> availableAccountIdsForActivity = accountList.stream().map(Account::id)
                                .collect(Collectors.toSet());
                List<UUID> requestedAccountIdsForActivity = accountIdList;
                List<UUID> effectiveAccountIdListForActivity = (requestedAccountIdsForActivity != null
                                && !requestedAccountIdsForActivity.isEmpty()
                                && availableAccountIdsForActivity.containsAll(requestedAccountIdsForActivity))
                                                ? requestedAccountIdsForActivity
                                                : null;

                Set<UUID> availableStockIdsForActivity = stockItemList.stream().map(StockItem::id)
                                .collect(Collectors.toSet());
                List<UUID> requestedStockIdsForActivity = stockItemIdList;
                List<UUID> effectiveStockItemIdListForActivity = (requestedStockIdsForActivity != null
                                && !requestedStockIdsForActivity.isEmpty()
                                && availableStockIdsForActivity.containsAll(requestedStockIdsForActivity))
                                                ? requestedStockIdsForActivity
                                                : null;

                // Build final lists and preserve selected items
                List<Account> finalAccountListForActivity;
                if (clientProvidedRangeForActivity) {
                        finalAccountListForActivity = new ArrayList<>(filteredAccountList);
                        if (requestedAccountIdsForActivity != null) {
                                for (UUID sel : requestedAccountIdsForActivity) {
                                        if (sel == null)
                                                continue;
                                        if (!activityAccountIds.contains(sel)) {
                                                accountList.stream().filter(a -> a.id().equals(sel)).findFirst()
                                                                .ifPresent(a -> {
                                                                        if (finalAccountListForActivity.stream()
                                                                                        .noneMatch(x -> x.id().equals(
                                                                                                        a.id())))
                                                                                finalAccountListForActivity.add(0, a);
                                                                });
                                        }
                                }
                        }
                } else {
                        finalAccountListForActivity = accountList;
                }

                List<StockItem> finalStockItemListForActivity;
                if (clientProvidedRangeForActivity) {
                        finalStockItemListForActivity = new ArrayList<>(filteredStockItemList);
                        if (requestedStockIdsForActivity != null) {
                                for (UUID sel : requestedStockIdsForActivity) {
                                        if (sel == null)
                                                continue;
                                        if (!activityStockIds.contains(sel)) {
                                                stockItemList.stream().filter(s -> s.id().equals(sel)).findFirst()
                                                                .ifPresent(s -> {
                                                                        if (finalStockItemListForActivity.stream()
                                                                                        .noneMatch(x -> x.id().equals(
                                                                                                        s.id())))
                                                                                finalStockItemListForActivity.add(0, s);
                                                                });
                                        }
                                }
                        }
                } else {
                        finalStockItemListForActivity = stockItemList;
                }

                long buyCount = activities.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
                                .count();
                long sellCount = activities.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
                                .count();
                BigDecimal buyAmount = activities.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "BUY".equals(a.tradeType()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sellAmount = activities.stream()
                                .filter(a -> "TRADE".equals(a.type()) && "SELL".equals(a.tradeType()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                long dividendCount = activities.stream().filter(a -> "DIVIDEND".equals(a.type())).count();
                BigDecimal dividendAmount = activities.stream()
                                .filter(a -> "DIVIDEND".equals(a.type()))
                                .map(a -> a.amount() != null ? a.amount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                model.addAttribute("activities", activities);
                model.addAttribute("accountList", finalAccountListForActivity);
                model.addAttribute("stockItemList", finalStockItemListForActivity);
                model.addAttribute(
                                "selectedAccountIds",
                                effectiveAccountIdListForActivity != null
                                                ? effectiveAccountIdListForActivity
                                                : List.of());
                model.addAttribute(
                                "selectedStockItemIds",
                                effectiveStockItemIdListForActivity != null
                                                ? effectiveStockItemIdListForActivity
                                                : List.of());
                model.addAttribute(
                                "selectedAccountId",
                                (effectiveAccountIdListForActivity != null
                                                && !effectiveAccountIdListForActivity.isEmpty())
                                                                ? effectiveAccountIdListForActivity.get(0)
                                                                : null);
                model.addAttribute(
                                "selectedStockItemId",
                                (effectiveStockItemIdListForActivity != null
                                                && !effectiveStockItemIdListForActivity.isEmpty())
                                                                ? effectiveStockItemIdListForActivity.get(0)
                                                                : null);
                model.addAttribute("startDate", startInstant);
                model.addAttribute("endDate", endInstant);
                model.addAttribute("timeZone", timeZone);
                model.addAttribute("rangeMode", rangeMode);
                model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
                model.addAttribute("buyCount", buyCount);
                model.addAttribute("sellCount", sellCount);
                model.addAttribute("buyAmount", buyAmount);
                model.addAttribute("sellAmount", sellAmount);
                model.addAttribute("dividendCount", dividendCount);
                model.addAttribute("dividendAmount", dividendAmount);
                return "stock/htmx/fragments/activityList";
        }

        private List<Activity> getAllActivities(
                        UUID userId,
                        Instant startInstant,
                        Instant endInstant,
                        List<UUID> accountIdList,
                        List<UUID> stockItemIdList) {
                TradeSearchRequest tradeReq = new TradeSearchRequest(userId, accountIdList, stockItemIdList,
                                startInstant, endInstant);
                List<TradeResponse> trades = tradeClient.findTrades(tradeReq.toParams());

                DividendRequest divReq = new DividendRequest();
                divReq.setUserId(userId);
                divReq.setStartDate(startInstant);
                divReq.setEndDate(endInstant);
                divReq.setAccountIdList(accountIdList);
                divReq.setStockItemIdList(stockItemIdList);
                List<DividendResponse> dividends = dividendClient.findDividends(divReq.toParams());

                List<StockItem> stockItemList = stockItemClient.getStockItems();
                Map<UUID, String> stockItemNames = stockItemList.stream()
                                .collect(Collectors.toMap(StockItem::id, StockItem::name));

                List<Account> accountList = accountClient.getAccountsByUserId(userId);
                Map<UUID, String> accountNamesMap = accountList.stream()
                                .collect(Collectors.toMap(Account::id, Account::name));

                List<Activity> rawActivities = new ArrayList<>();

                for (TradeResponse t : trades) {
                        String stockName = stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(t.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "TRADE",
                                                        stockName,
                                                        t.type().name(),
                                                        t.quantity(),
                                                        null,
                                                        t.amount(),
                                                        t.tradeDate(),
                                                        List.of(accountName)));
                }

                for (DividendResponse d : dividends) {
                        String stockName = d.stockItemName() != null
                                        ? d.stockItemName()
                                        : stockItemNames.getOrDefault(d.stockItemId(), msg("stock.label.unknown"));
                        String accountName = accountNamesMap.getOrDefault(d.accountId(), "Unknown Account");
                        rawActivities.add(
                                        new Activity(
                                                        "DIVIDEND",
                                                        stockName,
                                                        null,
                                                        null,
                                                        "배당금지급",
                                                        d.netAmount(),
                                                        d.payDate() != null ? d.payDate() : d.recordDate(),
                                                        List.of(accountName)));
                }

                Map<String, Activity> groupedMap = new HashMap<>();
                for (Activity a : rawActivities) {
                        if (a.date() == null)
                                continue;

                        String dateStr = a.date().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
                        String key = String.format("%s|%s|%s|%s", dateStr, a.type(), a.stockItemName(), a.tradeType());

                        if (groupedMap.containsKey(key)) {
                                Activity existing = groupedMap.get(key);

                                Integer newQty = null;
                                if (existing.quantity() != null || a.quantity() != null) {
                                        newQty = (existing.quantity() != null ? existing.quantity() : 0)
                                                        + (a.quantity() != null ? a.quantity() : 0);
                                }

                                BigDecimal newAmount = null;
                                if (existing.amount() != null || a.amount() != null) {
                                        newAmount = (existing.amount() != null ? existing.amount() : BigDecimal.ZERO)
                                                        .add(a.amount() != null ? a.amount() : BigDecimal.ZERO);
                                }

                                List<String> newAccountNames = new ArrayList<>(existing.accountNames());
                                if (!newAccountNames.contains(a.accountNames().get(0))) {
                                        newAccountNames.add(a.accountNames().get(0));
                                }

                                groupedMap.put(
                                                key,
                                                new Activity(
                                                                existing.type(),
                                                                existing.stockItemName(),
                                                                existing.tradeType(),
                                                                newQty,
                                                                existing.description(),
                                                                newAmount,
                                                                existing.date(),
                                                                newAccountNames));
                        } else {
                                groupedMap.put(key, a);
                        }
                }

                List<Activity> activities = new ArrayList<>(groupedMap.values());
                activities.sort(
                                Comparator.comparing(Activity::date, Comparator.nullsLast(Comparator.reverseOrder())));
                return activities;
        }
}
