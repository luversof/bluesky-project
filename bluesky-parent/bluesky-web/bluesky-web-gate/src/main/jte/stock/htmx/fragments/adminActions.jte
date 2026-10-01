@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil

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
	// 기록이 없으면 대시만 남는다 - 대시는 스스로 뜻을 말하지 못하므로 까닭을 함께 둔다.
	// JS 가 시각을 넣을 때는 이 칸을 통째로 갈아치우므로 까닭도 함께 사라진다.
	String lastRunNoneLabel = MessageUtil.getMessage("stock.admin.label.last.run.none");
	String dataCoverageLabel = MessageUtil.getMessage("stock.admin.label.data.coverage");
	String importOkTemplate = MessageUtil.getMessage("stock.admin.import.result.ok");
	String importDroppedTemplate = MessageUtil.getMessage("stock.admin.import.result.dropped");
	String priceFailedTemplate = MessageUtil.getMessage("stock.admin.import.result.price.failed");
	String priceProgressTemplate = MessageUtil.getMessage("stock.admin.price.progress");
	String priceProgressDoneTemplate = MessageUtil.getMessage("stock.admin.price.progress.done");
	String priceProgressFailedTemplate = MessageUtil.getMessage("stock.admin.price.progress.failed");

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
			 그 폭에서는 줄바꿈을 허용한다. 글꼴 200% 에서는 "(Dividends)" 같은 영어 한 단어가 버튼 안쪽보다 넓어
			 페이지를 33px 밀었다 - 그때는 단어 안에서도 끊는다(wrap-anywhere, 실측 2026-09-17). 한글 단어는 띄어쓰기에서만 나눈다(break-keep). --%>
		<div class="flex flex-wrap gap-4">
			<button hx-post="/api/stock/admin/stock-items"
					data-admin-action="stock-items"
					hx-swap="none"
					hx-indicator="#update-loading"
					hx-confirm="${updateStockItemsConfirm}"
					data-reload-after-request="true"
					class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere max-sm:text-left">
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
					class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere max-sm:text-left">
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
					class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere max-sm:text-left">
				<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
				</svg>
				${updateDividendsLabel}
			</button>
			<button hx-post="/api/stock/admin/price-histories"
					data-admin-action="price-histories"
					hx-swap="none"
					hx-confirm="${updatePriceHistoriesConfirm}"
					<%-- 이 단추는 작업을 시작만 한다 - 끝나는 건 아래 진행 표시가 보고 새로고침한다. 덮개(hx-indicator)도 안 쓴다(18~22 초 동안 화면을 가린다). --%>
					class="btn btn-outline gap-2 max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere max-sm:text-left">
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
				<h3 class="font-semibold" data-page-section>${dataCoverageLabel}</h3>
				<span>${updateTradesLabel}: <span class="whitespace-nowrap date-whole">${asDate.apply(dataStatus.tradeLastDate())}</span>${withCount.apply("", dataStatus.tradeCount())}</span>
				<span>${updateDividendsLabel}: <span class="whitespace-nowrap date-whole">${asDate.apply(dataStatus.dividendLastDate())}</span>${withCount.apply("", dataStatus.dividendCount())}</span>
				<span>${updatePriceHistoriesLabel}: <span class="whitespace-nowrap date-whole">${dataStatus.priceHistoryLastDate() != null ? dataStatus.priceHistoryLastDate().toString() : "-"}</span></span>
				<span>${updateStockItemsLabel}: ${java.text.MessageFormat.format(countSuffix, dataStatus.stockItemCount())}</span>
			</div>
			<%-- 최신 시세 일자 한 줄만 적으면 전 종목이 그 날까지 최신인 것으로 읽힌다. 바로 위에 "종목 86건" 이
				 나란히 있어 더 그렇다. 실측 2026-09-12: 86 종목 중 이 날짜 행이 있는 것은 9 종목뿐이고 70 종목은
				 2026-04-01/03 에, 나머지는 그보다 더 전에 멈춰 있었다(이력이 아예 없는 종목 4). 시세 갱신이 보유
				 종목 위주로 도는 결과라 데이터가 틀린 것은 아니지만, 밝히지 않으면 없는 최신성을 믿게 된다. --%>
			@if(dataStatus.stockItemCount() > 0 && dataStatus.priceHistoryLastDate() != null)
				<div class="text-[11px] text-base-content/60 mb-2">
					${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.history.coverage"),
						String.format("%,d", dataStatus.stockItemCount()),
						String.format("%,d", dataStatus.priceHistoryLastDateItemCount()),
						String.format("%,d", dataStatus.priceHistoryNoHistoryItemCount()))}
				</div>
				<%-- 개수만 적으면 무엇을 고쳐야 할지 알 수 없다. 같은 화면의 다른 이상 항목(거래량 0 인데 종가가
					 바뀐 행, 제한폭을 넘은 행)은 이미 종목·날짜까지 적는다 - 이것만 숫자였다. --%>
				@if(dataStatus.priceHistoryNoHistoryItemRows() != null && !dataStatus.priceHistoryNoHistoryItemRows().isEmpty())
					<div class="text-[11px] text-base-content/60 mb-2 font-mono" data-no-history-items="${dataStatus.priceHistoryNoHistoryItemRows().size()}">
						@for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.NoHistoryItemRow row : dataStatus.priceHistoryNoHistoryItemRows())
							<div>${row.symbol()} ${row.stockItemName()}</div>
						@endfor
						@if(dataStatus.priceHistoryNoHistoryItemCount() > dataStatus.priceHistoryNoHistoryItemRows().size())
							<div data-list-truncated="no-history">
								${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.list.truncated"),
									String.valueOf(dataStatus.priceHistoryNoHistoryItemRows().size()),
									String.valueOf(dataStatus.priceHistoryNoHistoryItemCount() - dataStatus.priceHistoryNoHistoryItemRows().size()))}
							</div>
						@endif
					</div>
				@endif
			@endif
			<%-- 마지막 시세 일자가 '거래가 없던 날'이면 그 날짜에는 확정 종가가 없다. 화면 곳곳이 그 날짜를
				 '평가 기준'으로 적으므로, 여기서 사실을 밝히지 않으면 있지도 않은 최신성을 믿게 된다.
				 (실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일) --%>
			@if(dataStatus.priceHistoryLastDateLooksUntraded())
				<div class="alert alert-warning py-2 px-3 text-xs mb-2" role="alert">
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
					<%-- "그중" 은 바로 위의 거래량 0 행을 받는다. 2026-09-12 까지 이 문장이 중복 행 문장 뒤에 있어
						 화면에서는 "9종목 중 1건" 다음에 "그중 ... 1건" 이 붙었다 - 두 1 이 우연히 같은 숫자라
						 앞 문장의 1 건을 받는 것처럼 읽혔다(실제로는 앞은 2026-09-09 기준, 이것은 쌍방울 2025-05-08 로 무관).
						 받는 말 바로 뒤로 옮긴다. --%>
					@if(dataStatus.priceHistoryZeroVolumeChangedCloseCount() > 0)
						&middot; ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.changed"),
							String.valueOf(dataStatus.priceHistoryZeroVolumeChangedCloseCount()))}
					@endif
					@if(dataStatus.priceHistoryZeroVolumeChangedCloseRows() != null)
						@for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.ZeroVolumeChangedCloseRow row : dataStatus.priceHistoryZeroVolumeChangedCloseRows())
							<%-- 두 숫자만 보면 시세 수집이 깨진 것처럼 보인다. 배율이 깔끔한 정수비면 액면분할·병합이므로
								 그 비를 함께 적는다(실측 2026-08-23: 쌍방울 2025-05-08 13,450 -> 2,690 = 정확히 1/5). --%>
							<div class="font-mono" data-zero-volume-split="${row.splitRatioLabel() != null ? row.splitRatioLabel() : ""}">${row.stockItemName()} ${row.tradeDate() != null ? row.tradeDate().toString() : "-"} &middot; ${row.previousClosePrice()} &rarr; ${row.closePrice()}@if(row.splitRatioLabel() != null) <span class="text-base-content/60">(${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.split"), row.splitRatioLabel())})</span>@endif</div>
						@endfor
						<%-- 이 목록도 상한이 있다(질의 LIMIT 5). 같은 화면의 다른 두 목록과 같은 규칙으로 밝힌다. --%>
						@if(dataStatus.priceHistoryZeroVolumeChangedCloseCount() > dataStatus.priceHistoryZeroVolumeChangedCloseRows().size())
							<div class="text-[11px] text-base-content/60" data-list-truncated="zero-volume">
								${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.list.truncated"),
									String.valueOf(dataStatus.priceHistoryZeroVolumeChangedCloseRows().size()),
									String.valueOf(dataStatus.priceHistoryZeroVolumeChangedCloseCount() - dataStatus.priceHistoryZeroVolumeChangedCloseRows().size()))}
							</div>
						@endif
					@endif
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
					<%-- 거래량 0 그물은 분할을 대부분 놓친다 - 분할은 보통 거래가 재개되며 거래량이 붙는다.
						 (실측 2026-08-24: 그 그물에 걸린 유일한 행 쌍방울 2025-05-08 도 거래량이 우연히 0 이었을 뿐)
						 하루 변동이 제한폭(±30%)을 넘으면 거래로는 생길 수 없으므로 따로 센다. 다만 정리매매는
						 제한폭이 적용되지 않으므로 '오류'라고 쓰지 않고 배율을 함께 적어 사람이 가리게 한다.
						 정상적인 하한가는 기준을 0.301 로 둬 애초에 걸리지 않는다 - 상·하한가는 기준가에 0.7/1.3 을
						 곱한 뒤 호가단위로 맞추므로 30% 를 아주 조금 넘길 수 있다(실측 2026-09-10: 한화오션
						 2015-07-15 -30.00036%). 그 한 행이 빠져 5행 -> 4행이 됐고, 남은 4행 중 분할로 설명되는 것은 1행이다. --%>
					@if(dataStatus.priceHistoryPriceLimitBreachCount() > 0)
						<div class="mt-1">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.limit"),
							String.valueOf(dataStatus.priceHistoryPriceLimitBreachCount()))}</div>
						@if(dataStatus.priceHistoryPriceLimitBreachRows() != null)
							@for(net.luversof.web.gate.stock.dto.response.DataStatusResponse.PriceLimitBreachRow row : dataStatus.priceHistoryPriceLimitBreachRows())
								<div class="font-mono" data-price-limit-split="${row.splitRatioLabel() != null ? row.splitRatioLabel() : ""}" data-price-limit-gap="${row.gapDays()}">${row.stockItemName()} ${row.tradeDate() != null ? row.tradeDate().toString() : "-"} &middot; ${row.previousClosePrice()} &rarr; ${row.closePrice()} (${StockFormatUtil.signedPct(row.changePercent(), 1)})@if(row.splitRatioLabel() != null) <span class="text-base-content/60">(${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.price.quality.split"), row.splitRatioLabel())})</span>@endif</div>
							@endfor
							<%-- 목록에는 상한이 있다. 개수만 크게 적고 몇 줄만 보이면 나머지가 있는 줄 모른다.
								 (같은 화면의 다른 목록도 같은 규칙) --%>
							@if(dataStatus.priceHistoryPriceLimitBreachCount() > dataStatus.priceHistoryPriceLimitBreachRows().size())
								<div class="text-[11px] text-base-content/60" data-list-truncated="limit-breach">
									${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.list.truncated"),
										String.valueOf(dataStatus.priceHistoryPriceLimitBreachRows().size()),
										String.valueOf(dataStatus.priceHistoryPriceLimitBreachCount() - dataStatus.priceHistoryPriceLimitBreachRows().size()))}
								</div>
							@endif
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
				<h3 class="font-semibold" data-page-section>${MessageUtil.getMessage("stock.admin.ledger.title")}</h3>
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
				<div class="alert alert-info py-2 px-3 text-xs mt-2 block" data-ledger-multi-rows="${ledgerIntegrity.multiReasonRows().size()}" role="status">
					<div class="font-medium">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.admin.ledger.multi.reason.rows"), ledgerIntegrity.multiReasonRows().size())}</div>
					@for(net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse.RowFindingSummary row : ledgerIntegrity.multiReasonRows())
						<div class="opacity-80" data-ledger-row-codes="${row.codes().size()}">
							${row.date()} ${row.stockItemName()}@if(row.accountName() != null) [${row.accountName()}]@endif
							&middot; ${String.join(", ", row.codes().stream().map(code -> net.luversof.web.gate.stock.util.StockLedgerRuleLabelUtil.label(code)).toList())}
						</div>
					@endfor
				</div>
			@endif
			@if(ledgerIntegrity != null && !ledgerIntegrity.isClean())
				@for(net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse.Finding finding : ledgerIntegrity.findings())
					<div class="alert alert-warning py-2 px-3 text-xs mt-2 block" role="alert">
						<div class="font-medium" data-ledger-count="${finding.count()}">${net.luversof.web.gate.stock.util.StockLedgerRuleLabelUtil.label(finding.code())} (${finding.count()})</div>
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
			<h3 class="font-semibold" data-page-section>${lastRunLabel}</h3>
			<span>${updateStockItemsLabel}: <span data-admin-last-run="stock-items"><span aria-hidden="true">-</span><span class="sr-only">${lastRunNoneLabel}</span></span></span>
			<span>${updateTradesLabel}: <span data-admin-last-run="trades"><span aria-hidden="true">-</span><span class="sr-only">${lastRunNoneLabel}</span></span></span>
			<span>${updateDividendsLabel}: <span data-admin-last-run="dividends"><span aria-hidden="true">-</span><span class="sr-only">${lastRunNoneLabel}</span></span></span>
			<span>${updatePriceHistoriesLabel}: <span data-admin-last-run="price-histories"><span aria-hidden="true">-</span><span class="sr-only">${lastRunNoneLabel}</span></span></span>
		</div>
	</div>
	
	<%-- 갱신 결과. 버튼이 hx-swap="none" + 전체 새로고침이라, 응답을 sessionStorage 에 담아 두고
		 새로고침된 화면에서 여기에 그린다. 이게 없으면 몇 행이 빠졌는지 사용자가 알 방법이 없다. --%>
	<div id="admin-import-result"
		 class="hidden"
		 data-import-ok-template="${importOkTemplate}"
		 data-import-dropped-template="${importDroppedTemplate}"
		 data-price-failed-template="${priceFailedTemplate}"></div>

	<%-- 전체 갱신 진행 상황(사용자 결정 2026-09-22). 53 종목에 18~22 초라 게이트 읽기 제한(10 초)을 넘겨,
	     누를 때마다 500 이 났고 데이터는 정작 갱신돼 있었다. 이제 시작만 하고 여기서 진행을 물어본다. --%>
	<div id="price-history-progress" class="alert mt-3 hidden" role="status" aria-live="polite"
		 data-progress-template="${priceProgressTemplate}"
		 data-progress-done-template="${priceProgressDoneTemplate}"
		 data-progress-failed-template="${priceProgressFailedTemplate}">
		<span class="loading loading-spinner loading-sm" data-progress-spinner></span>
		<span data-progress-text></span>
	</div>

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
			// 시간대도 앱이 정한다(common.js 의 appTimeZone). 인자를 빼면 브라우저 존을 따라
			// 같은 기록이 기기마다 다른 날짜로 읽힌다(실측 2026-09-16: 9/16 10:30 vs 9/15 8:30).
			if (ts) {
				el.textContent = new Date(ts).toLocaleString(appLocale(), { timeZone: appTimeZone() });
			}
		});
		var RESULT_KEY = 'stock.admin.lastResult.v1';

		function format(template, values) {
			return String(template).replace(/\{(\d+)\}/g, function(_, i) {
				var v = values[Number(i)];
				return v === undefined || v === null ? '' : String(v);
			});
		}

		// 전체 시세 갱신은 뒤에서 돈다 - 시작 응답을 받은 뒤 진행 상황을 물어보고, 끝나면 새로고침한다.
		var progressBox = document.getElementById("price-history-progress");
		var progressTimer = null;
		// 이 화면에서 시작한 작업인가. 지난 실행의 DONE 을 지금 끝난 것으로 알리면 안 된다
		// (실측 2026-09-22: 화면을 열 때마다 2 분 전 결과를 띄우고 새로고침해 되풀이됐다).
		var watchingOurRun = false;

		function showProgress(kind, values) {
			if (!progressBox) return;
			var attr = kind === "done" ? "data-progress-done-template"
				: (kind === "failed" ? "data-progress-failed-template" : "data-progress-template");
			progressBox.classList.remove("hidden", "alert-success", "alert-error", "alert-info");
			progressBox.classList.add(kind === "done" ? "alert-success"
				: (kind === "failed" ? "alert-error" : "alert-info"));
			var spinner = progressBox.querySelector("[data-progress-spinner]");
			if (spinner) spinner.classList.toggle("hidden", kind !== "running");
			var slot = progressBox.querySelector("[data-progress-text]");
			if (slot) slot.textContent = format(progressBox.getAttribute(attr), values);
		}

		function stopWatching() {
			if (progressTimer) clearInterval(progressTimer);
			progressTimer = null;
		}

		function pollPriceHistoryJob() {
			fetch("/api/stock/admin/price-histories/status", { headers: { "Accept": "application/json" } })
				.then(function(res) { return res.ok ? res.json() : null; })
				.then(function(body) {
					if (!body) return;
					if (body.status === "RUNNING") {
						// 다른 탭에서 시작했거나 새로고침 중이던 작업도 여기서 이어 본다.
						watchingOurRun = true;
						if (!progressTimer) progressTimer = setInterval(pollPriceHistoryJob, 1500);
						showProgress("running", [body.processedSymbolCount, body.targetSymbolCount]);
						return;
					}
					stopWatching();
					if (!watchingOurRun) {
						// 지난 실행의 결과다 - 지금 일이 아니므로 아무 말도 하지 않는다.
						return;
					}
					watchingOurRun = false;
					if (body.status === "FAILED") {
						showProgress("failed", [body.failureReason || ""]);
						return;
					}
					showProgress("done", [body.targetSymbolCount, (body.failedSymbols || []).length]);
					// 값이 실제로 바뀌었으니 화면을 다시 그린다(기준일 · 종목 수가 여기 걸려 있다).
					setTimeout(function() { location.reload(); }, 1500);
				})
				.catch(function() { /* 다음 물음에서 다시 본다 */ });
		}

		function watchPriceHistoryJob() {
			watchingOurRun = true;
			showProgress("running", [0, 0]);
			if (!progressTimer) progressTimer = setInterval(pollPriceHistoryJob, 1500);
			pollPriceHistoryJob();
		}

		// 화면을 열 때 한 번 본다 - 돌고 있으면 이어 보고, 이미 끝난 지난 실행이면 잠자코 둔다.
		if (progressBox) pollPriceHistoryJob();

		// 응답에서 "사용자가 알아야 할 것"만 뽑는다. 알릴 게 없으면 null.
		function summarize(action, body) {
			if (!body || typeof body !== 'object') return null;
			if (action === 'price-histories') {
				// 이 응답은 "시작했다" 일 뿐이라 알릴 결과가 없다 - 결과는 진행 표시가 말한다.
				return null;
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
			if (action === 'price-histories') {
				// 작업은 이제 막 시작됐다. 여기서 성공이라 적으면 아직 안 끝난 일을 끝났다고 말하는 셈이다.
				watchPriceHistoryJob();
			}
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