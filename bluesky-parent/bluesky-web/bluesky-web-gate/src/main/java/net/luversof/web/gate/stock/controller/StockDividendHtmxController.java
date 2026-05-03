package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendHtmxController extends StockBaseHtmxController {

    public StockDividendHtmxController(
            TradeProfitClient tradeProfitClient,
            TradeClient tradeClient,
            AccountClient accountClient,
            StockItemClient stockItemClient,
            DividendClient dividendClient,
            MessageSource messageSource) {
        super(tradeProfitClient, tradeClient, accountClient, stockItemClient, dividendClient, messageSource);
    }

    @BlueskyPreAuthorize
    @GetMapping("/dividend/list")
    public String dividendList(
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

        Instant startInstant = startDate;
        Instant endInstant = endDate;
        // Default to this year when client didn't provide range
        if (startInstant == null && endInstant == null && (rangeMode == null || rangeMode.isBlank())) {
            ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
            LocalDate now = LocalDate.now(zone);
            startInstant = LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant();
            endInstant = now.plusDays(1).atStartOfDay(zone).toInstant();
            rangeMode = "ytd";
        }

        var request = new DividendRequest();
        request.setUserId(userId);
        request.setStartDate(startInstant);
        request.setEndDate(endInstant);

        List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

        // Always fetch the global/all dividend set so we can offer "전체 기간" filtering
        var globalReq = new DividendRequest();
        globalReq.setUserId(userId);
        List<DividendResponse> globalDividends = dividendClient.findDividends(globalReq.toParams());
        ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
        LocalDate dataFirstDate = globalDividends.stream()
                .map(
                        d -> {
                            Instant payDate = d.payDate();
                            Instant recordDate = d.recordDate();
                            if (payDate == null && recordDate == null)
                                return null;
                            if (payDate == null)
                                return recordDate;
                            if (recordDate == null)
                                return payDate;
                            return payDate.isBefore(recordDate) ? payDate : recordDate;
                        })
                .filter(inst -> inst != null)
                .map(inst -> inst.atZone(zone).toLocalDate())
                .min(Comparator.naturalOrder())
                .orElse(null);

        var dividendAccountIds = dividends.stream().map(DividendResponse::accountId).collect(Collectors.toSet());
        var dividendStockIds = dividends.stream().map(DividendResponse::stockItemId).collect(Collectors.toSet());
        var globalDividendStockIds = globalDividends.stream().map(DividendResponse::stockItemId)
                .collect(Collectors.toSet());

        List<Account> accounts = accountClient.getAccountsByUserId(userId);
        List<Account> filteredAccountList = accounts.stream().filter(a -> dividendAccountIds.contains(a.id())).toList();

        Map<UUID, String> accountNames = accounts.stream()
                .collect(
                        Collectors.toMap(
                                Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, Boolean> taxDeferredMap = accounts.stream()
                .collect(
                        Collectors.toMap(
                                Account::id,
                                a -> a.jsonConfig() != null
                                        && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
                                (l, r) -> l));

        List<StockItem> stockItemList = stockItemClient.getStockItems();
        List<StockItem> filteredStockItemList = stockItemList.stream().filter(s -> dividendStockIds.contains(s.id()))
                .toList();
        // Stocks that have any dividends in the global timeframe (used for 전체/no-range)
        List<StockItem> filteredStockItemListAll = stockItemList.stream()
                .filter(s -> globalDividendStockIds.contains(s.id()))
                .toList();
        Map<UUID, String> stockItemNames = stockItemList.stream()
                .collect(Collectors.toMap(StockItem::id, StockItem::name));

        // Validate requested filters against full lists (so we can preserve previous
        // selections) and build final lists that show date-available items but keep
        // any previously selected items visible.
        Set<UUID> availableAccountIds = accounts.stream().map(Account::id).collect(Collectors.toSet());
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

        List<Account> finalAccountList;
        if (startInstant != null || endInstant != null) {
            finalAccountList = new ArrayList<>(filteredAccountList);
            if (requestedAccountIds != null) {
                for (UUID sel : requestedAccountIds) {
                    if (sel == null)
                        continue;
                    if (!finalAccountList.stream().anyMatch(a -> a.id().equals(sel))) {
                        accounts.stream().filter(a -> a.id().equals(sel)).findFirst()
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
                    if (sel == null)
                        continue;
                    if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
                        stockItemList.stream().filter(s -> s.id().equals(sel)).findFirst()
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
                    if (sel == null)
                        continue;
                    if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
                        stockItemList.stream().filter(s -> s.id().equals(sel)).findFirst()
                                .ifPresent(s -> finalStockItemList.add(0, s));
                    }
                }
            }
        }

        List<DividendView> viewList = dividends.stream()
                .filter(
                        d -> (effectiveAccountIdList == null
                                || effectiveAccountIdList.isEmpty()
                                || effectiveAccountIdList.contains(d.accountId())))
                .filter(
                        d -> (effectiveStockItemIdList == null
                                || effectiveStockItemIdList.isEmpty()
                                || effectiveStockItemIdList.contains(d.stockItemId())))
                .map(
                        dividend -> {
                            String accountName = accountNames.getOrDefault(dividend.accountId(),
                                    msg("stock.label.unknown"));
                            String stockItemName = Optional.ofNullable(dividend.stockItemName())
                                    .orElse(
                                            Optional.ofNullable(dividend.stockItemId())
                                                    .map(id -> stockItemNames.getOrDefault(id,
                                                            msg("stock.label.unknown")))
                                                    .orElse(msg("stock.label.unknown")));

                            boolean isDeferred = taxDeferredMap.getOrDefault(dividend.accountId(), false);

                            BigDecimal grossAmount = Optional.ofNullable(dividend.grossAmount())
                                    .orElse(BigDecimal.ZERO);
                            BigDecimal tax = isDeferred
                                    ? BigDecimal.ZERO
                                    : Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
                            BigDecimal netAmount = isDeferred
                                    ? grossAmount
                                    : Optional.ofNullable(dividend.netAmount())
                                            .orElse(grossAmount.subtract(tax));

                            BigDecimal taxableAmount = BigDecimal.ZERO;
                            if (!isDeferred) {
                                if (dividend.taxableAmount() != null) {
                                    taxableAmount = dividend.taxableAmount();
                                } else if (dividend.taxPerShare() != null && dividend.quantity() != null) {
                                    taxableAmount = dividend.taxPerShare()
                                            .multiply(BigDecimal.valueOf(dividend.quantity()));
                                }
                            }

                            return new DividendView(
                                    dividend.id(),
                                    dividend.accountId(),
                                    accountName,
                                    dividend.stockItemId(),
                                    stockItemName,
                                    grossAmount,
                                    tax,
                                    taxableAmount,
                                    netAmount,
                                    dividend.recordDate(),
                                    dividend.payDate());
                        })
                .collect(Collectors.toCollection(ArrayList::new));

        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            String field = parts[0];
            String direction = parts.length > 1 ? parts[1] : "asc";

            Comparator<DividendView> comparator = switch (field) {
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

        if (size <= 0)
            size = 15;

        boolean isSearch = (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
                || (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
                || startDate != null
                || endDate != null;

        if (isSearch) {
            size = Math.max(viewList.size(), 1);
        }

        int totalItems = viewList.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        if (totalPages == 0)
            currentPage = 1;

        int fromIndex = (currentPage - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<DividendView> pagedList = (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex)
                : Collections.emptyList();

        BigDecimal totalGrossAmount = pagedList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal totalNetAmount = pagedList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal totalTax = pagedList.stream().map(DividendView::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTaxableAmount = pagedList.stream()
                .map(DividendView::taxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAllGrossAmount = viewList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal totalAllNetAmount = viewList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal totalAllTaxableAmount = viewList.stream().map(DividendView::taxableAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal prevPeriodNetAmount = null;
        LocalDate prevStartDate = null;
        LocalDate prevEndDate = null;
        if (startDate != null && endDate != null) {
            // convert Instants to LocalDate in the request's timezone (reuse earlier
            // `zone`)
            LocalDate startLocal = startDate.atZone(zone).toLocalDate();
            LocalDate endLocal = endDate.atZone(zone).toLocalDate();

            long durationDays = ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
            prevStartDate = startLocal.minusDays(durationDays);
            prevEndDate = startLocal.minusDays(1);

            Instant prevStartInstant = prevStartDate.atStartOfDay(zone).toInstant();
            Instant prevEndInstant = prevEndDate.plusDays(1).atStartOfDay(zone).toInstant();

            var prevRequest = new DividendRequest();
            prevRequest.setUserId(userId);
            prevRequest.setStartDate(prevStartInstant);
            prevRequest.setEndDate(prevEndInstant);

            final List<UUID> finalAccountIdList = effectiveAccountIdList;
            final List<UUID> finalStockItemIdList = effectiveStockItemIdList;
            List<DividendResponse> prevDividends = dividendClient.findDividends(prevRequest.toParams());
            prevPeriodNetAmount = prevDividends.stream()
                    .filter(
                            d -> (finalAccountIdList == null
                                    || finalAccountIdList.isEmpty()
                                    || finalAccountIdList.contains(d.accountId())))
                    .filter(
                            d -> (finalStockItemIdList == null
                                    || finalStockItemIdList.isEmpty()
                                    || finalStockItemIdList.contains(d.stockItemId())))
                    .map(
                            d -> {
                                boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
                                BigDecimal gross = Optional.ofNullable(d.grossAmount()).orElse(BigDecimal.ZERO);
                                if (isDeferred)
                                    return gross;
                                BigDecimal tax2 = Optional.ofNullable(d.tax()).orElse(BigDecimal.ZERO);
                                return Optional.ofNullable(d.netAmount()).orElse(gross.subtract(tax2));
                            })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
        var pagination = new Pagination(pageImpl);

        model.addAttribute("dividendList", pagedList);
        model.addAttribute("allDividendList", viewList);
        model.addAttribute("pagination", pagination);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("size", size);
        model.addAttribute("accountList", finalAccountList);
        model.addAttribute("stockItemList", finalStockItemList);
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
        model.addAttribute("prevPeriodNetAmount", prevPeriodNetAmount);
        model.addAttribute("prevStartDate", prevStartDate);
        model.addAttribute("prevEndDate", prevEndDate);
        model.addAttribute("rangeMode", rangeMode);
        model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
        // reflect rangeMode back into model (may have been defaulted above)
        model.addAttribute("rangeMode", rangeMode);

        return "stock/htmx/fragments/tabsDividendHistory";
    }
}
