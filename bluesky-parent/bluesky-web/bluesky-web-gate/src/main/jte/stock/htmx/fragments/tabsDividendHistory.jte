@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import java.time.LocalDate
@import java.math.BigDecimal
@import java.math.RoundingMode
@import net.luversof.web.gate.stock.dto.response.DividendView
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import net.luversof.web.gate.stock.controller.StockDividendHtmxController.DividendChange
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil

@param List<DividendView> dividendList
@param List<DividendView> allDividendList = java.util.List.of()
@param String dividendTtmJs = "{}"
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<String> stockTagList = java.util.List.of()
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param java.time.Instant startDate
@param java.time.Instant endDate
@param String timeZone = null
@param String sort
@param BigDecimal totalGrossAmount
@param BigDecimal totalNetAmount
@param BigDecimal totalTax
@param BigDecimal totalTaxableAmount
@param BigDecimal totalAllGrossAmount = BigDecimal.ZERO
@param BigDecimal totalAllNetAmount = BigDecimal.ZERO
@param BigDecimal totalAllTaxableAmount = BigDecimal.ZERO
@param BigDecimal totalAllTax = BigDecimal.ZERO
@param BigDecimal midMonthNetAmount = BigDecimal.ZERO
@param BigDecimal monthEndNetAmount = BigDecimal.ZERO
@param BigDecimal otherWindowNetAmount = BigDecimal.ZERO
@param long midMonthCount = 0L
@param long monthEndCount = 0L
@param long otherWindowCount = 0L
@param BigDecimal prevPeriodNetAmount = null
@param LocalDate prevStartDate = null
@param LocalDate prevEndDate = null
@param List<DividendChange> dividendChangeContributors = java.util.List.of()
@param BigDecimal portfolioYieldOnDailyAverageCostPct = null
@param BigDecimal portfolioYieldOnCostPct = null
@param BigDecimal portfolioYieldOnMarketPct = null
@param BigDecimal portfolioYieldAnnualizedPct = null
@param long periodDayCount = 0L
@param BigDecimal periodStartPrincipal = null
@param BigDecimal periodEndPrincipal = null
@param BigDecimal periodPrincipalDelta = null
@param BigDecimal periodPrincipalDeltaPct = null
@param DividendYieldGroupView bestYieldStock = null
@param DividendYieldGroupView bestYieldAccount = null
@param List<DividendYieldGroupView> stockYieldRows = java.util.List.of()
@param List<DividendYieldGroupView> accountYieldRows = java.util.List.of()
@param List<DividendYieldGroupView> yearlyYieldRows = java.util.List.of()
@param String rangeMode = null
@param String dataFirstDate = null

!{
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");

    // Safe nulls
    BigDecimal safeAllNet = totalAllNetAmount != null ? totalAllNetAmount : BigDecimal.ZERO;
    BigDecimal safeAllGross = totalAllGrossAmount != null ? totalAllGrossAmount : BigDecimal.ZERO;
    BigDecimal safeAllTaxable = totalAllTaxableAmount != null ? totalAllTaxableAmount : BigDecimal.ZERO;
    BigDecimal safeAllTax = totalAllTax != null ? totalAllTax : BigDecimal.ZERO;
    // 세전 − 세후 = 총 차감액(세금 + 수수료·기타). 수수료·기타 = 차감액 − 세금.
    BigDecimal allDeduction = safeAllGross.subtract(safeAllNet);
    BigDecimal allOtherDeduction = allDeduction.subtract(safeAllTax);
    BigDecimal safePrevNet = prevPeriodNetAmount != null ? prevPeriodNetAmount : BigDecimal.ZERO;
    boolean hasComparison = prevPeriodNetAmount != null;
    BigDecimal diffAmount = hasComparison ? safeAllNet.subtract(safePrevNet) : BigDecimal.ZERO;
    boolean diffPositive = diffAmount.compareTo(BigDecimal.ZERO) >= 0;
    String diffPct = "";
    if (hasComparison && safePrevNet.compareTo(BigDecimal.ZERO) != 0) {
        diffPct = safeAllNet.subtract(safePrevNet)
            .multiply(new BigDecimal("100"))
            .divide(safePrevNet, 1, RoundingMode.HALF_UP)
            .toPlainString();
    } else if (hasComparison) {
        diffPct = "∞";
    }

    // Build JSON for charts
    java.util.function.Function<String, String> ej = s ->
        s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    StringBuilder jsSb = new StringBuilder();
    jsSb.append("const dividendData=[");
    List<DividendView> chartList = allDividendList != null ? allDividendList : java.util.List.of();
    for (int ci = 0; ci < chartList.size(); ci++) {
        DividendView d = chartList.get(ci);
        if (ci > 0) jsSb.append(",");
        java.time.ZoneId _zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
        jsSb.append("{payDate:\"").append(d.payDate() != null ? ymd.format(d.payDate().atZone(_zone)) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(d.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(d.accountName())).append("\"");
        jsSb.append(",net:").append(d.netAmount() != null ? d.netAmount().toPlainString() : "0");
        jsSb.append(",gross:").append(d.grossAmount() != null ? d.grossAmount().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String dividendDataJs = jsSb.toString();

    // Nav 버튼 활성화 여부 (use client timezone if available)
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
    java.time.LocalDate today = java.time.LocalDate.now(zone);
    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocalInclusive = endLocal != null ? endLocal.minusDays(1) : null;
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null
        && dataFirstDate != null && !dataFirstDate.isEmpty()
        && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

    java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
    String totalItemsCountText = countMessage.apply(totalItems);
    String detailSummaryLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.detail.summary"), countMessage.apply(totalItems));

    // 여러 섹션이 공유하는 라벨 (하위 컴포넌트에 param 으로 전달)
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String grossAmountLabel = MessageUtil.getMessage("stock.dividend.gross");
    String netAmountLabel = MessageUtil.getMessage("stock.dividend.net");
    String taxLabel = MessageUtil.getMessage("stock.dividend.tax");
    String taxableAmountLabel = MessageUtil.getMessage("stock.dividend.taxable.amount");
    String totalLabel = MessageUtil.getMessage("common.label.total");
    String noDataLabel = MessageUtil.getMessage("common.message.no.data");

    // 하단 인라인 스크립트에서 사용하는 라벨
    String othersLabel = MessageUtil.getMessage("stock.chart.others");
    String noPeriodHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.period.history");
    String averageNoDataLabel = MessageUtil.getMessage("stock.dividend.summary.no.data");
    String averageDescTemplate = MessageUtil.getMessage("stock.dividend.summary.average.desc");
    String countPattern = MessageUtil.getMessage("common.count");
    String ttmLabel = MessageUtil.getMessage("stock.dividend.chart.ttm.label");
    java.util.function.Function<BigDecimal, String> percentFormat = value -> value != null ? String.format("%.2f%%", value.doubleValue()) : "-";
    java.util.function.Function<Long, String> countFormat = count -> java.text.MessageFormat.format(countPattern, count);
    // '전체' 는 날짜를 안 보내므로 기간 배지가 빌 수밖에 없다. 그때는 이 화면이 실제로 그린 자료의
    // 구간을 대신 적는다 - 화면마다 덮는 범위가 다르므로 사용자 전체의 최초 일자를 쓰면 틀린 날짜가 나온다.
    var coveredRange = net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(allDividendList, net.luversof.web.gate.stock.dto.response.DividendView::payDate, zone);
}

<div id="dividendListFragment">

    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "dividendPicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal,
        coveredStartDate = coveredRange.startDate(),
        coveredEndDate = coveredRange.endDate()
    )

    @template.stock.htmx.fragments.dividend.dividendSearchForm(
        accountList = accountList,
        stockItemList = stockItemList,
        stockTagList = stockTagList,
        selectedAccountId = selectedAccountId,
        selectedStockItemId = selectedStockItemId,
        selectedAccountIds = selectedAccountIds,
        selectedStockItemIds = selectedStockItemIds,
        selectedStockTags = selectedStockTags,
        startDate = startDate,
        endDate = endDate,
        timeZone = timeZone,
        rangeMode = rangeMode,
        zone = zone,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        tagLabel = tagLabel
    )

    <!-- Dashboard -->

    @template.stock.htmx.fragments.dividend.dividendSummaryCards(
        decimalFormat = decimalFormat,
        safeAllNet = safeAllNet,
        safeAllGross = safeAllGross,
        safeAllTaxable = safeAllTaxable,
        safeAllTax = safeAllTax,
        allDeduction = allDeduction,
        allOtherDeduction = allOtherDeduction,
        safePrevNet = safePrevNet,
        hasComparison = hasComparison,
        diffPositive = diffPositive,
        diffPct = diffPct,
        diffAmount = diffAmount,
        totalItemsCountText = totalItemsCountText,
        prevStartDate = prevStartDate,
        prevEndDate = prevEndDate,
        dividendChangeContributors = dividendChangeContributors,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel,
        midMonthNetAmount = midMonthNetAmount,
        monthEndNetAmount = monthEndNetAmount,
        otherWindowNetAmount = otherWindowNetAmount,
        midMonthCount = midMonthCount,
        monthEndCount = monthEndCount,
        otherWindowCount = otherWindowCount
    )

    @template.stock.htmx.fragments.dividend.dividendCharts()

    @template.stock.htmx.fragments.dividend.dividendYieldAnalytics(
        decimalFormat = decimalFormat,
        percentFormat = percentFormat,
        countFormat = countFormat,
        safeAllNet = safeAllNet,
        portfolioYieldOnDailyAverageCostPct = portfolioYieldOnDailyAverageCostPct,
        portfolioYieldOnCostPct = portfolioYieldOnCostPct,
        portfolioYieldOnMarketPct = portfolioYieldOnMarketPct,
        portfolioYieldAnnualizedPct = portfolioYieldAnnualizedPct,
        periodDayCount = periodDayCount,
        periodStartPrincipal = periodStartPrincipal,
        periodEndPrincipal = periodEndPrincipal,
        periodPrincipalDelta = periodPrincipalDelta,
        periodPrincipalDeltaPct = periodPrincipalDeltaPct,
        bestYieldStock = bestYieldStock,
        bestYieldAccount = bestYieldAccount,
        yearlyYieldRows = yearlyYieldRows,
        stockYieldRows = stockYieldRows,
        accountYieldRows = accountYieldRows,
        stockItemList = stockItemList,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        tagLabel = tagLabel,
        noDataLabel = noDataLabel,
        grossAmountLabel = grossAmountLabel,
        netAmountLabel = netAmountLabel,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel,
        totalLabel = totalLabel
    )

    @template.stock.htmx.fragments.dividend.dividendTable(
        dividendList = dividendList,
        zone = zone,
        pagination = pagination,
        sort = sort,
        decimalFormat = decimalFormat,
        percentFormat = percentFormat,
        detailSummaryLabel = detailSummaryLabel,
        totalGrossAmount = totalGrossAmount,
        totalNetAmount = totalNetAmount,
        totalTax = totalTax,
        totalTaxableAmount = totalTaxableAmount,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        grossAmountLabel = grossAmountLabel,
        netAmountLabel = netAmountLabel,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel,
        totalLabel = totalLabel
    )

    <%-- 서버가 아는 것만 넘긴다(배당 데이터 · 기간 · 라벨 · 선택기 minDate). 동작은 /js/stock/dividendHistory.js 에 있다
     (2026-09-08 까지 여기 746 줄 인라인 스크립트였다). 조각이 스왑될 때마다 이 블록이 새 config 를 싣고, 모듈이
     htmx:afterSettle 에서 다시 초기화한다. --%>
<script nonce="${CspNonceHolder.getNonce()}">
    (function() {
        $unsafe{dividendDataJs}
        window.dividendHistoryConfig = {
            dividendData: dividendData,
            ttmByMonth: $unsafe{dividendTtmJs},
            ttmLabel: "${ttmLabel}",
            filterStartDate: "${startLocal != null ? startLocal.format(DateTimeFormatter.ofPattern("yyyy-MM")) : ""}",
            filterEndDate: "${endLocalInclusive != null ? endLocalInclusive.format(DateTimeFormatter.ofPattern("yyyy-MM")) : ""}",
            countPattern: "${countPattern}",
            noDataLabel: "${noDataLabel}",
            noPeriodHistoryLabel: "${noPeriodHistoryLabel}",
            averageNoDataLabel: "${averageNoDataLabel}",
            averageDescTemplate: "${averageDescTemplate}",
            netAmountLabel: "${netAmountLabel}",
            othersLabel: "${othersLabel}",
            minDate: "${dataFirstDate != null ? dataFirstDate : ""}"
        };
    })();
</script>
<script type="module" src="/js/stock/dividendHistory.js"></script>

</div>