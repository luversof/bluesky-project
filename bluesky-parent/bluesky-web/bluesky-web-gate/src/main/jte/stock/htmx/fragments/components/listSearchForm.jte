@import java.util.List
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import io.github.luversof.boot.context.support.MessageUtil

<%-- 배당 내역 · 매매 내역의 상세 조회 폼(기간 · 계좌 · 종목 · 태그).
	 2026-10-22 까지 dividendSearchForm.jte 와 tradeSearchForm.jte 가 같은 마크업을 한 벌씩 들고 있었다(다른 것은 폼 id · 목록 주소 ·
	 id 앞말 · 접힘 제목 · 종목 칸 이름뿐). 배당 쪽 날짜 칸 id 만 앞말 없이 startDateInput 이었는데, 다른 화면(trade · activity · asset)처럼
	 dividendStartDateInput 으로 맞췄다(부르는 곳은 dividendHistory.ts 하나). 같은 날 활동 목록(activityList.jte)의 사본도 이 조각으로 옮겼다. --%>
@param String formId
@param String contentUrl
@param String fragmentId
<%-- 입력 이름 · id 앞말: ${idPrefix}UiStartDate · ${idPrefix}StartDateInput · ${idPrefix}StartInstantInput … --%>
@param String idPrefix
<%-- 접힘 제목. 화면마다 문구가 달라(배당 = 상세 조회, 매매 = 상세 검색) 부르는 쪽이 준다. --%>
@param String summaryLabel
@param String accountLabel
@param String stockLabel
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<StockItem> tagCountStockItemList = stockItemList
@param List<String> stockTagList
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds
@param List<UUID> selectedStockItemIds
@param List<String> selectedStockTags
@param Instant startDate
@param Instant endDate
@param String timeZone
@param String rangeMode
@param ZoneId zone
<%-- 활동 목록만: 아직 렌더되지 않은 뷰로 전환할 때 common.js 가 조회 버튼(data-activity-refresh)을 눌러 다시 받는다. --%>
@param boolean activityRefresh = false

!{
	String periodLabel = MessageUtil.getMessage("common.label.period");
	String tagLabel = MessageUtil.getMessage("common.label.tag");
	String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
	String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
	String allLabel = MessageUtil.getMessage("common.label.all");
	String searchLabel = MessageUtil.getMessage("common.button.search");
	String resetLabel = MessageUtil.getMessage("common.button.reset");
	String fragmentSelector = "#" + fragmentId;
	String formSelector = "#" + formId;
}

	<!-- Filter & Search Form (collapsible) -->
	<details class="mb-4" data-detail-filter data-filter-active="${(selectedAccountIds != null && !selectedAccountIds.isEmpty()) || (selectedStockItemIds != null && !selectedStockItemIds.isEmpty()) || (selectedStockTags != null && !selectedStockTags.isEmpty()) ? "true" : "false"}">
		<summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
			<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
			${summaryLabel}
			@template.stock.htmx.fragments.components.filterBadge(
				selectedAccountId = selectedAccountId,
				selectedStockItemId = selectedStockItemId,
				selectedAccountIds = selectedAccountIds,
				selectedStockItemIds = selectedStockItemIds,
				selectedStockTags = selectedStockTags
			)
		</summary>
	<form id="${formId}" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
		  hx-get="${contentUrl}"
		  hx-target="${fragmentSelector}"
		  hx-select="${fragmentSelector}"
		  hx-swap="outerHTML"
		  hx-trigger="submit">

		<div class="stock-filter-grid">
			<div class="form-control w-full">
				<label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
				<div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
					<input type="date" name="${idPrefix}UiStartDate" aria-label="${MessageUtil.getMessage("common.label.date.start")}" id="${idPrefix}StartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
					<input type="date" name="${idPrefix}UiEndDate" aria-label="${MessageUtil.getMessage("common.label.date.end")}" id="${idPrefix}EndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
				</div>
			</div>

			<div class="form-control w-full">
				<label class="label p-1" for="filterAccountSelect"><span class="label-text text-xs">${accountLabel}</span></label>
				<select id="filterAccountSelect" name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
					<option value="">${allLabel}</option>
					@if(accountList != null)
					@for(Account account : accountList)
					<option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
					@endfor
					@endif
				</select>
			</div>

			<div class="form-control w-full">
				<label class="label p-1" for="filterStockSelect"><span class="label-text text-xs">${stockLabel}</span></label>
				<select id="filterStockSelect" name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
					<option value="">${allLabel}</option>
					@if(stockItemList != null)
					@for(StockItem stockItem : stockItemList)
					!{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
					<option value="${stockItem.id().toString()}"
							selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
							data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
					@endfor
					@endif
				</select>
			</div>

			<div class="contents">
				@template.stock.htmx.fragments.components.stockSelectionPanel(
					tagLabel = tagLabel,
					allLabel = allLabel,
					helperLabel = tagHelperLabel,
					selectedTagSummaryLabel = tagSummaryLabel,
					stockTagList = stockTagList,
					selectedStockTags = selectedStockTags,
					stockItemList = stockItemList,
					tagCountStockItemList = tagCountStockItemList
				)

				<div class="flex justify-end gap-2 xl:self-end">
					<button type="button" class="btn btn-sm btn-primary" data-selection-save="true" data-activity-refresh="${activityRefresh ? "true" : null}"
						hx-get="${contentUrl}"
						hx-target="${fragmentSelector}"
						hx-select="${fragmentSelector}"
						hx-swap="outerHTML"
						hx-include="${formSelector}">${searchLabel}</button>
					<button type="button" class="btn btn-sm text-xs"
						hx-get="${contentUrl}"
						hx-target="${fragmentSelector}"
						hx-select="${fragmentSelector}"
						hx-swap="outerHTML"
						hx-include="${formSelector}"
						data-selection-reset="true">${resetLabel}</button>
				</div>
			</div>
		</div>
		<input type="hidden" name="startDate" id="${idPrefix}StartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
		<input type="hidden" name="endDate" id="${idPrefix}EndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
		<input type="hidden" name="timeZone" id="${idPrefix}TimeZoneInput" value="${timeZone != null ? timeZone : ""}">
		<input type="hidden" name="rangeMode" id="${idPrefix}RangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

	</form>
	</details>
