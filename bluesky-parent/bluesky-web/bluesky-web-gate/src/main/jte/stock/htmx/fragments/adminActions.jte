@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param net.luversof.web.gate.stock.dto.response.DataStatusResponse dataStatus = null
@param net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse ledgerIntegrity = null

!{
    String dataManagementLabel = MessageUtil.getMessage("stock.admin.label.data.management");
    String updateStockItemsLabel = MessageUtil.getMessage("stock.admin.button.update.stock.items");
    String updateTradesLabel = MessageUtil.getMessage("stock.admin.button.update.trades");
    String updateDividendsLabel = MessageUtil.getMessage("stock.admin.button.update.dividends");
    String updatePriceHistoriesLabel = MessageUtil.getMessage("stock.admin.button.update.price.histories");
    String updateStockItemsConfirm = MessageUtil.getMessage("stock.admin.confirm.update.stock.items");
    String updateTradesConfirm = MessageUtil.getMessage("stock.admin.confirm.update.trades");
    String updateDividendsConfirm = MessageUtil.getMessage("stock.admin.confirm.update.dividends");
    String updatePriceHistoriesConfirm = MessageUtil.getMessage("stock.admin.confirm.update.price.histories");
    String updatingLabel = MessageUtil.getMessage("stock.admin.message.updating");
    String pleaseWaitLabel = MessageUtil.getMessage("stock.admin.message.please.wait");
    String lastRunLabel = MessageUtil.getMessage("stock.admin.label.last.run");
    String dataCoverageLabel = MessageUtil.getMessage("stock.admin.label.data.coverage");
    String importOkTemplate = MessageUtil.getMessage("stock.admin.import.result.ok");
    String importDroppedTemplate = MessageUtil.getMessage("stock.admin.import.result.dropped");
    String priceFailedTemplate = MessageUtil.getMessage("stock.admin.import.result.price.failed");

    String countSuffix = MessageUtil.getMessage("common.count");
    java.util.function.Function<java.time.Instant, String> asDate = instant ->
        instant == null ? "-" : instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    java.util.function.BiFunction<String, Long, String> withCount = (text, count) ->
        count == null ? text : text + " (" + java.text.MessageFormat.format(countSuffix, count) + ")";
}

@if(isAuthenticated)
@template._components.ui.tableCard(cssClass = "mb-6", body = @`
    <div class="card-body">
        <h2 class="card-title text-2xl mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
                ${dataManagementLabel}
        </h2>
        <%-- 갱신 버튼 라벨이 길어 좁은 화면에서 화면 밖으로 나간다(실측: 320px 에서 16px).
             그 폭에서는 줄바꿈을 허용한다. --%>
        <div class="flex flex-wrap gap-4">
            <button hx-post="/api/stock/admin/stock-items"
                    data-admin-action="stock-items"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateStockItemsConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:text-left">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                ${updateStockItemsLabel}
            </button>
            <button hx-post="/api/stock/admin/trades"
                    data-admin-action="trades"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateTradesConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:text-left">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                </svg>
                ${updateTradesLabel}
            </button>
            <button hx-post="/api/stock/admin/dividends"
                    data-admin-action="dividends"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateDividendsConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:text-left">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                ${updateDividendsLabel}
            </button>
            <button hx-post="/api/stock/admin/price-histories"
                    data-admin-action="price-histories"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updatePriceHistoriesConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:text-left">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
                ${updatePriceHistoriesLabel}
            </button>
        </div>
        <%-- 갱신 이력은 서버에 없으므로 이 브라우저에서 실행한 시각을 localStorage 로 기억해 표시한다 --%>
        <%-- 실제 데이터가 어디까지 채워졌는지(서버 기준). 브라우저 로컬의 '마지막 갱신 클릭 시각'과 달리
             다른 브라우저에서 보거나 갱신이 실패했을 때도 실제 범위를 알 수 있다. --%>
        @if(dataStatus != null)
            <div class="flex flex-wrap gap-x-6 gap-y-1 text-sm text-base-content/70 mb-2">
                <span class="font-semibold">${dataCoverageLabel}</span>
                <span>${updateTradesLabel}: ${withCount.apply(asDate.apply(dataStatus.tradeLastDate()), dataStatus.tradeCount())}</span>
                <span>${updateDividendsLabel}: ${withCount.apply(asDate.apply(dataStatus.dividendLastDate()), dataStatus.dividendCount())}</span>
                <span>${updatePriceHistoriesLabel}: ${dataStatus.priceHistoryLastDate() != null ? dataStatus.priceHistoryLastDate().toString() : "-"}</span>
                <span>${updateStockItemsLabel}: ${java.text.MessageFormat.format(countSuffix, dataStatus.stockItemCount())}</span>
            </div>
            <%-- 마지막 시세 일자가 '거래가 없던 날'이면 그 날짜에는 확정 종가가 없다. 화면 곳곳이 그 날짜를
                 '평가 기준'으로 적으므로, 여기서 사실을 밝히지 않으면 있지도 않은 최신성을 믿게 된다.
                 (실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일) --%>
            @if(dataStatus.priceHistoryLastDateLooksUntraded())
                <div class="alert alert-warning py-2 px-3 text-xs mb-2">
                    ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.untraded"),
                        String.valueOf(dataStatus.priceHistoryLastDate()),
                        String.valueOf(dataStatus.priceHistoryItemCount()),
                        String.valueOf(dataStatus.priceHistoryPreviousDate()))}
                </div>
            @endif
            <%-- 거래량 0 행은 '거래가 없던 시점에 수집된' 행이다. 비율만 보면 그 행들을 평가에서 빼도
                 되는지 판단할 수 있고, 종가가 바뀐 행이 있으면 빼는 순간 과거 값이 달라진다.
                 지금까지 api-stock 이 이 값들을 보내는데도 화면에 그리는 곳이 없어 판단할 수 없었다.
                 (실측 2026-08-23: 57,459 행 중 1,352 행이 거래량 0, 그중 종가가 바뀐 것은
                  쌍방울 2025-05-08 한 건이고 13,450 -> 2,690 으로 정확히 1/5 - 액면분할이다) --%>
            @if(dataStatus.priceHistoryRowCount() > 0)
                <div class="text-[11px] text-base-content/60 mb-2">
                    ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality"),
                        String.format("%,d", dataStatus.priceHistoryRowCount()),
                        String.format("%,d", dataStatus.priceHistoryZeroVolumeRowCount()),
                        String.format("%.2f", dataStatus.priceHistoryZeroVolumeRatioPercent()))}
                    <%-- 마지막 시세일이 직전 거래일과 얼마나 같은지. 종가만 같으면 '거래가 없던 날'이고,
                         시가·고가·저가·거래량까지 같으면 행 자체가 복제됐다는 뜻이라 원인이 다르다.
                         이 구분에 쓸 유일한 값(sameAll)이 api-stock 에서 오는데도 화면에 없었다
                         (실측 2026-08-23: 9종목 중 종가 동일 9 / 전 항목 동일 0 - 복제는 아니다). --%>
                    @if(dataStatus.priceHistoryItemCount() > 0)
                        <div>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.duplicate.rows"),
                            String.valueOf(dataStatus.priceHistorySameCloseCount()),
                            String.valueOf(dataStatus.priceHistoryItemCount()),
                            String.valueOf(dataStatus.priceHistorySameAllCount()))}</div>
                    @endif
                    @if(dataStatus.priceHistoryZeroVolumeChangedCloseCount() > 0)
                        &middot; ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.changed"),
                            String.valueOf(dataStatus.priceHistoryZeroVolumeChangedCloseCount()))}
                    @endif
                    @if(dataStatus.priceHistoryZeroVolumeChangedCloseRows() != null)
                        @for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.ZeroVolumeChangedCloseRow row : dataStatus.priceHistoryZeroVolumeChangedCloseRows())
                            <%-- 두 숫자만 보면 시세 수집이 깨진 것처럼 보인다. 배율이 깔끔한 정수비면 액면분할·병합이므로
                                 그 비를 함께 적는다(실측 2026-08-23: 쌍방울 2025-05-08 13,450 -> 2,690 = 정확히 1/5). --%>
                            <div class="font-mono" data-zero-volume-split="${row.splitRatioLabel() != null ? row.splitRatioLabel() : ""}">${row.stockItemName()} ${row.tradeDate() != null ? row.tradeDate().toString() : "-"} &middot; ${row.previousClosePrice()} &rarr; ${row.closePrice()}@if(row.splitRatioLabel() != null) <span class="opacity-70">(${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.split"), row.splitRatioLabel())})</span>@endif</div>
                        @endfor
                    @endif
                    <%-- 거래량 0 그물은 분할을 대부분 놓친다 - 분할은 보통 거래가 재개되며 거래량이 붙는다.
                         (실측 2026-08-24: 그 그물에 걸린 유일한 행 쌍방울 2025-05-08 도 거래량이 우연히 0 이었을 뿐)
                         하루 변동이 제한폭(±30%)을 넘으면 거래로는 생길 수 없으므로 따로 센다. 다만 정리매매·
                         하한가는 제한폭이 적용되지 않거나 그 자체가 정상이므로 '오류'라고 쓰지 않고 배율을 함께
                         적어 사람이 가리게 한다(실측: 5행 중 분할로 설명되는 것은 1행뿐이다). --%>
                    @if(dataStatus.priceHistoryPriceLimitBreachCount() > 0)
                        <div class="mt-1">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.limit"),
                            String.valueOf(dataStatus.priceHistoryPriceLimitBreachCount()))}</div>
                        @if(dataStatus.priceHistoryPriceLimitBreachRows() != null)
                            @for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.PriceLimitBreachRow row : dataStatus.priceHistoryPriceLimitBreachRows())
                                <div class="font-mono" data-price-limit-split="${row.splitRatioLabel() != null ? row.splitRatioLabel() : ""}" data-price-limit-gap="${row.gapDays()}">${row.stockItemName()} ${row.tradeDate() != null ? row.tradeDate().toString() : "-"} &middot; ${row.previousClosePrice()} &rarr; ${row.closePrice()} (${String.format("%+.1f%%", row.changePercent())})@if(row.splitRatioLabel() != null) <span class="opacity-70">(${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.split"), row.splitRatioLabel())})</span>@endif</div>
                            @endfor
                        @endif
                    @endif
                </div>
            @endif
            <%-- 월배당 지급 이력도 사람이 가져와야 하는 참조 데이터인데, 위 줄들은 매매·배당·시세만 알려 준다.
                 한 달 치가 비면 예상 월배당의 기준(최신 1개월)·12개월 평균의 창·"다가올 배당"의 예상 지급일이
                 함께 어긋나므로 여기서 밝힌다. 밀림 판정은 종목 자신의 과거 최대 간격으로 한다. --%>
            @if(dataStatus.monthlyDividendPayoutLastDate() != null)
                <div class="text-[11px] text-base-content/60 mb-2">
                    ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.payout.reference"),
                        dataStatus.monthlyDividendPayoutLastDate().toString())}
                    @if(dataStatus.monthlyDividendPayoutOverdueRows() != null && !dataStatus.monthlyDividendPayoutOverdueRows().isEmpty())
                        @for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.MonthlyDividendPayoutOverdueRow row : dataStatus.monthlyDividendPayoutOverdueRows())
                            <div data-payout-elapsed="${row.elapsedDays()}" data-payout-widest="${row.widestGapDays()}" class="text-warning">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.payout.reference.overdue"),
                                String.valueOf(dataStatus.monthlyDividendPayoutOverdueRows().size()),
                                row.stockItemName(),
                                String.valueOf(row.elapsedDays()),
                                row.lastPayDate() != null ? row.lastPayDate().toString() : "-",
                                String.valueOf(row.widestGapDays()))}</div>
                        @endfor
                    @endif
                </div>
            @endif
        @endif
        <%-- 원장 점검: 증권사 화면을 사람이 옮겨 담은 값이라 산술적으로 불가능한 기록이 실제로 들어와 있다
             (실측 2026-08-22: 배당 193 건 중 8 건이 세금 > 과세표준). 화면은 그 값을 그대로 더할 뿐이라
             여기서 드러내지 않으면 잘못된 값이 계속 합계에 섞인다. 고치는 것은 사람이 원장에서 해야 하므로
             앱은 '어느 줄이 이상한지'만 알려 준다. --%>
        <div class="mt-3 border-t border-base-200 pt-3">
            <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm">
                <span class="font-semibold">${MessageUtil.getMessage("stock.admin.ledger.title")}</span>
                @if(ledgerIntegrity == null)
                    <span class="text-base-content/60">${MessageUtil.getMessage("stock.admin.ledger.unavailable")}</span>
                @else
                    <span class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.checked"), ledgerIntegrity.dividendCount(), ledgerIntegrity.tradeCount())}</span>
                    <%-- 한 행이 여러 규칙에 걸린다. 건수만 보면 할 일이 실제보다 커 보인다
                         (실측 2026-08-23: 45건이 실제로는 29개 행). --%>
                    <%-- 계좌별로 나눠 보여야 어디부터 손대야 할지 알 수 있다
                         (실측 2026-08-23: 45건이 계좌 3개로 깨끗하게 갈렸다). --%>
                    @if(ledgerIntegrity.accountSummary() != null && !ledgerIntegrity.accountSummary().isEmpty())
                        <span class="text-xs text-base-content/60" data-ledger-accounts="${ledgerIntegrity.accountSummary().size()}">
                            ${String.join(" · ", ledgerIntegrity.accountSummary().stream().map(row -> row.accountName() + " " + row.findingCount()).toList())}
                        </span>
                    @endif
                    @if(ledgerIntegrity.hasOverlappingRows())
                        <span class="text-xs text-base-content/60" data-ledger-distinct="${ledgerIntegrity.distinctRowCount()}">
                            ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.distinct.rows"), ledgerIntegrity.totalFindingCount(), ledgerIntegrity.distinctRowCount())}
                        </span>
                    @endif
                @endif
            </div>
            @if(ledgerIntegrity != null && ledgerIntegrity.isClean())
                <div class="text-xs text-base-content/60 mt-1">${MessageUtil.getMessage("stock.admin.ledger.clean")}</div>
            @endif
            <%-- 한 행이 여러 규칙에 걸리는 경우를 먼저 묶어 보여 준다.
                 규칙별로만 나열하면 같은 행을 규칙 그룹마다 다시 만나게 되고, 그 행들이 사실
                 한 원인이라는 것도 드러나지 않는다
                 (실측 2026-08-23: 발견 48건 = 30행, 그중 10행이 사유 2~4개. KODEX 한국부동산리츠인프라
                  배당 8행이 전부 여기 속하고 원인은 과세표준을 옛 수량 77주로 잡은 것 하나다). --%>
            @if(ledgerIntegrity != null && ledgerIntegrity.hasMultiReasonRows())
                <div class="alert alert-info py-2 px-3 text-xs mt-2 block" data-ledger-multi-rows="${ledgerIntegrity.multiReasonRows().size()}">
                    <div class="font-medium">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.multi.reason.rows"), ledgerIntegrity.multiReasonRows().size())}</div>
                    @for(net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse.RowFindingSummary row : ledgerIntegrity.multiReasonRows())
                        <div class="opacity-80" data-ledger-row-codes="${row.codes().size()}">
                            ${row.date()} ${row.stockItemName()}@if(row.accountName() != null) [${row.accountName()}]@endif
                            &middot; ${String.join(", ", row.codes().stream().map(code -> MessageUtil.getMessage("stock.admin.ledger.rule." + code)).toList())}
                        </div>
                    @endfor
                </div>
            @endif
            @if(ledgerIntegrity != null && !ledgerIntegrity.isClean())
                @for(net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse.Finding finding : ledgerIntegrity.findings())
                    <div class="alert alert-warning py-2 px-3 text-xs mt-2 block">
                        <div class="font-medium" data-ledger-count="${finding.count()}">${MessageUtil.getMessage("stock.admin.ledger.rule." + finding.code())} (${finding.count()})</div>
                        <%-- 예시를 3건만 보여 주던 시절에는 발견의 절반 이상을 화면에서 볼 수 없었다
                             (실측 2026-08-23: 45건 중 25건). 조치하려면 어느 행인지 알아야 하므로
                             앞 3건은 그대로 펼쳐 두고 나머지는 접어 둔다. --%>
                        @if(finding.examples() != null)
                            !{int shownCount = Math.min(3, finding.examples().size());}
                            @for(int exampleIndex = 0; exampleIndex < shownCount; exampleIndex++)
                                !{var example = finding.examples().get(exampleIndex);}
                                <div class="opacity-80">${example.date()} ${example.stockItemName()} &middot; ${example.detail()}</div>
                            @endfor
                            @if(finding.examples().size() > shownCount)
                                <details class="mt-1" data-ledger-collapsed="${finding.examples().size() - shownCount}">
                                    <summary class="cursor-pointer select-none opacity-70">
                                        ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.more.examples"), finding.examples().size() - shownCount)}
                                    </summary>
                                    @for(int exampleIndex = shownCount; exampleIndex < finding.examples().size(); exampleIndex++)
                                        !{var example = finding.examples().get(exampleIndex);}
                                        <div class="opacity-80">${example.date()} ${example.stockItemName()} &middot; ${example.detail()}</div>
                                    @endfor
                                </details>
                            @endif
                            @if(finding.count() > finding.examples().size())
                                <div class="opacity-60 mt-1" data-ledger-truncated="${finding.count() - finding.examples().size()}">
                                    ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.truncated"), finding.count() - finding.examples().size())}
                                </div>
                            @endif
                        @endif
                    </div>
                @endfor
            @endif
        </div>
        <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-base-content/60">
            <span class="font-semibold">${lastRunLabel}</span>
            <span>${updateStockItemsLabel}: <span data-admin-last-run="stock-items">-</span></span>
            <span>${updateTradesLabel}: <span data-admin-last-run="trades">-</span></span>
            <span>${updateDividendsLabel}: <span data-admin-last-run="dividends">-</span></span>
            <span>${updatePriceHistoriesLabel}: <span data-admin-last-run="price-histories">-</span></span>
        </div>
    </div>
    
    <%-- 갱신 결과. 버튼이 hx-swap="none" + 전체 새로고침이라, 응답을 sessionStorage 에 담아 두고
         새로고침된 화면에서 여기에 그린다. 이게 없으면 몇 행이 빠졌는지 사용자가 알 방법이 없다. --%>
    <div id="admin-import-result"
         class="hidden"
         data-import-ok-template="${importOkTemplate}"
         data-import-dropped-template="${importDroppedTemplate}"
         data-price-failed-template="${priceFailedTemplate}"></div>

    <div id="update-loading" class="htmx-indicator fixed inset-0 bg-black/50 z-50 flex items-center justify-center">
        <div class="bg-base-100 p-8 rounded-box shadow-xl flex flex-col items-center gap-4">
            <span class="loading loading-spinner loading-lg text-primary"></span>
            <div class="text-lg font-bold">${updatingLabel}</div>
            <div class="text-sm opacity-70">${pleaseWaitLabel}</div>
        </div>
    </div>

    <script nonce="${CspNonceHolder.getNonce()}">
    (function() {
        var STORAGE_KEY = 'stock.admin.lastRun.v1';
        function readMap() {
            try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}; } catch (e) { return {}; }
        }
        // 저장된 실행 시각 표시
        var lastRunMap = readMap();
        document.querySelectorAll('[data-admin-last-run]').forEach(function(el) {
            var ts = lastRunMap[el.getAttribute('data-admin-last-run')];
            if (ts) el.textContent = new Date(ts).toLocaleString();
        });
        var RESULT_KEY = 'stock.admin.lastResult.v1';

        function format(template, values) {
            return String(template).replace(/\{(\d+)\}/g, function(_, i) {
                var v = values[Number(i)];
                return v === undefined || v === null ? '' : String(v);
            });
        }

        // 응답에서 "사용자가 알아야 할 것"만 뽑는다. 알릴 게 없으면 null.
        function summarize(action, body) {
            if (!body || typeof body !== 'object') return null;
            if (action === 'price-histories') {
                var failed = (body.failedSymbols || []).length;
                if (!failed) return null;
                return { level: 'warning', args: [failed, body.targetSymbolCount, (body.failedSymbols || []).join(', ')], kind: 'price' };
            }
            if (action === 'trades' || action === 'dividends') {
                var source = body.sourceRowCount;
                var imported = body.importedCount;
                if (typeof source !== 'number' || typeof imported !== 'number') return null;
                var dropped = source - imported;
                if (dropped <= 0) return { level: 'success', args: [source], kind: 'ok' };
                return { level: 'warning', args: [source, imported, (body.unknownStockNames || []).join(', ')], kind: 'dropped' };
            }
            return null;
        }

        // 갱신 성공 시 시각 기록 (기록 후 data-reload-after-request 위임이 새로고침한다)
        document.addEventListener('htmx:afterRequest', function(evt) {
            var btn = evt.target && evt.target.closest ? evt.target.closest('[data-admin-action]') : null;
            if (!btn || !evt.detail || !evt.detail.successful) return;
            var action = btn.getAttribute('data-admin-action');
            var map = readMap();
            map[action] = Date.now();
            try { localStorage.setItem(STORAGE_KEY, JSON.stringify(map)); } catch (e) {}
            // 새로고침을 건너뛰고 사라지므로 결과는 세션에 남겨 둔다.
            try {
                var body = JSON.parse((evt.detail.xhr && evt.detail.xhr.responseText) || 'null');
                var summary = summarize(action, body);
                if (summary) sessionStorage.setItem(RESULT_KEY, JSON.stringify(summary));
            } catch (e) {}
        });

        // 새로고침된 화면에서 한 번만 그리고 지운다.
        (function renderStoredResult() {
            var box = document.getElementById('admin-import-result');
            if (!box) return;
            var raw = null;
            try { raw = sessionStorage.getItem(RESULT_KEY); sessionStorage.removeItem(RESULT_KEY); } catch (e) {}
            if (!raw) return;
            var summary = null;
            try { summary = JSON.parse(raw); } catch (e) { return; }
            if (!summary) return;
            var template = summary.kind === 'price'
                ? box.getAttribute('data-price-failed-template')
                : summary.kind === 'dropped'
                    ? box.getAttribute('data-import-dropped-template')
                    : box.getAttribute('data-import-ok-template');
            box.className = 'alert mb-4 ' + (summary.level === 'warning' ? 'alert-warning' : 'alert-success');
            box.textContent = format(template, summary.args);
        })();
    })();
    </script>
`)
@endif